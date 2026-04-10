#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <android/log.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <cerrno>
#include <cstring>

// llama.cpp public API
#include "llama.h"

#define LOG_TAG "LLMEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// ---------------------------------------------------------------------------
// Inference configuration constants
// ---------------------------------------------------------------------------
static constexpr int   N_CTX          = 2048;   // context window (tokens)
static constexpr int   N_BATCH        = 512;    // batch size for prompt eval
static constexpr int   MAX_NEW_TOKENS = 1024;   // cap on generated tokens
static constexpr float TEMPERATURE    = 0.7f;   // sampling temperature
static constexpr float TOP_P          = 0.9f;   // nucleus sampling threshold

// ---------------------------------------------------------------------------
// Global error string — written on failure, read by nativeGetLastError
// ---------------------------------------------------------------------------
static std::string g_last_error;
static std::mutex  g_error_mutex;

static void setLastError(const std::string& msg) {
    std::lock_guard<std::mutex> lock(g_error_mutex);
    g_last_error = msg;
    LOGE("%s", msg.c_str());
}

// ---------------------------------------------------------------------------
// llama.cpp backend initialisation (one-time, thread-safe)
// ---------------------------------------------------------------------------
static void initLlamaBackend() {
    static std::once_flag flag;
    std::call_once(flag, []() {
        // Redirect llama.cpp log output to Android logcat
        llama_log_set([](enum llama_log_level level, const char* text, void*) {
            int prio;
            switch (level) {
                case LLAMA_LOG_LEVEL_ERROR: prio = ANDROID_LOG_ERROR; break;
                case LLAMA_LOG_LEVEL_WARN:  prio = ANDROID_LOG_WARN;  break;
                case LLAMA_LOG_LEVEL_INFO:  prio = ANDROID_LOG_INFO;  break;
                default:                    prio = ANDROID_LOG_DEBUG; break;
            }
            __android_log_print(prio, "llama.cpp", "%s", text);
        }, nullptr);

        llama_backend_init();
        LOGI("llama.cpp backend initialised");
    });
}

// ---------------------------------------------------------------------------
// Helper: validate file exists, is a regular file, is readable, and ≥ 1 MB
// ---------------------------------------------------------------------------
static bool validateModelFile(const char* path) {
    struct stat st;
    if (stat(path, &st) != 0) {
        setLastError(std::string("Model file not found: ") + path
                     + " (errno=" + std::to_string(errno) + ")");
        return false;
    }
    if (!S_ISREG(st.st_mode)) {
        setLastError(std::string("Model path is not a regular file: ") + path);
        return false;
    }
    if (access(path, R_OK) != 0) {
        setLastError(std::string("Model file not readable: ") + path);
        return false;
    }
    if (st.st_size < 1024 * 1024) {
        setLastError(std::string("Model file too small (")
                     + std::to_string(st.st_size) + " bytes): " + path);
        return false;
    }
    LOGI("Model file validated: %s (%lld bytes)", path, (long long)st.st_size);
    return true;
}

// Buffer sizes for llama.cpp token conversion and counting
static constexpr int MAX_TOKEN_PIECE_SIZE  = 512;   // max UTF-8 bytes for a single token piece
static constexpr int MAX_TOKEN_COUNT_BUFFER = 4096;  // max tokens used for countTokens estimation

// ---------------------------------------------------------------------------
// Persistent model state
// ---------------------------------------------------------------------------
struct ModelState {
    std::string     model_path;
    int             n_threads = 4;
    llama_model*    model     = nullptr;
    llama_context*  context   = nullptr;
};

// ---------------------------------------------------------------------------
// JNI: nativeLoadModel
// Loads model + creates inference context. Returns pointer-as-jlong on
// success, 0 on failure. Caller retrieves the error via nativeGetLastError.
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT jlong JNICALL
Java_com_example_emotionawareai_engine_LLMEngine_nativeLoadModel(
        JNIEnv* env,
        jobject /* thiz */,
        jstring model_path_jstr,
        jint    n_threads) {

    const char* model_path = env->GetStringUTFChars(model_path_jstr, nullptr);
    if (!model_path) {
        setLastError("Failed to convert model path string");
        return 0L;
    }

    LOGI("Loading model: %s (n_threads=%d)", model_path, (int)n_threads);

    if (!validateModelFile(model_path)) {
        env->ReleaseStringUTFChars(model_path_jstr, model_path);
        return 0L;
    }

    initLlamaBackend();

    // Load model weights
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;   // CPU-only; ARM NEON/dotprod kernels handle everything

    llama_model* model = llama_load_model_from_file(model_path, model_params);
    if (!model) {
        setLastError(std::string("llama_load_model_from_file failed for: ") + model_path);
        env->ReleaseStringUTFChars(model_path_jstr, model_path);
        return 0L;
    }

    // Create inference context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx          = N_CTX;
    ctx_params.n_batch        = N_BATCH;
    ctx_params.n_threads      = n_threads;
    ctx_params.n_threads_batch = n_threads;

    llama_context* ctx = llama_new_context_with_model(model, ctx_params);
    if (!ctx) {
        setLastError("llama_new_context_with_model failed — not enough memory?");
        llama_free_model(model);
        env->ReleaseStringUTFChars(model_path_jstr, model_path);
        return 0L;
    }

    auto* state = new ModelState();
    state->model_path = std::string(model_path);
    state->n_threads  = n_threads;
    state->model      = model;
    state->context    = ctx;

    env->ReleaseStringUTFChars(model_path_jstr, model_path);
    LOGI("Model loaded successfully: %s", state->model_path.c_str());
    return reinterpret_cast<jlong>(state);
}

// ---------------------------------------------------------------------------
// JNI: nativeGenerateResponse
// Tokenises the prompt, runs the prefill decode, then samples tokens one at a
// time, invoking the Kotlin callback lambda for each text piece.
// Returns JNI_TRUE on success.
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_emotionawareai_engine_LLMEngine_nativeGenerateResponse(
        JNIEnv* env,
        jobject /* thiz */,
        jlong   handle,
        jstring prompt_jstr,
        jobject token_callback) {

    if (handle == 0L) {
        setLastError("nativeGenerateResponse called with null handle");
        return JNI_FALSE;
    }
    auto* state = reinterpret_cast<ModelState*>(handle);
    if (!state->model || !state->context) {
        setLastError("Model or context is null — was it loaded correctly?");
        return JNI_FALSE;
    }

    const char* prompt = env->GetStringUTFChars(prompt_jstr, nullptr);
    if (!prompt) {
        setLastError("Failed to convert prompt string");
        return JNI_FALSE;
    }
    LOGD("Generating response for prompt (first 80 chars): %.80s", prompt);

    // ── Resolve callback method ──────────────────────────────────────────────
    jclass   cb_class  = env->GetObjectClass(token_callback);
    jmethodID cb_invoke = env->GetMethodID(
            cb_class, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");
    if (!cb_invoke) {
        setLastError("Could not find callback invoke() method");
        env->ReleaseStringUTFChars(prompt_jstr, prompt);
        return JNI_FALSE;
    }

    // ── Tokenise prompt ───────────────────────────────────────────────────────
    int prompt_len = static_cast<int>(strlen(prompt));
    std::vector<llama_token> tokens(N_CTX);
    int n_tokens = llama_tokenize(
            state->model,
            prompt,
            prompt_len,
            tokens.data(),
            static_cast<int>(tokens.size()),
            /*add_special=*/true,
            /*parse_special=*/true);

    if (n_tokens < 0) {
        // Buffer too small — truncate to N_CTX
        LOGE("Prompt too long (%d tokens needed), truncating to %d", -n_tokens, N_CTX);
        tokens.resize(N_CTX);
        n_tokens = N_CTX;
    } else {
        tokens.resize(n_tokens);
    }
    LOGD("Prompt tokenised: %d tokens", n_tokens);

    // ── Prefill decode ────────────────────────────────────────────────────────
    // Process prompt tokens in one batch (they all fit within N_BATCH in a
    // typical mobile scenario; for very long prompts N_BATCH handles chunking)
    llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens, 0, 0);
    if (llama_decode(state->context, batch) != 0) {
        setLastError("llama_decode failed during prompt prefill");
        env->ReleaseStringUTFChars(prompt_jstr, prompt);
        return JNI_FALSE;
    }
    int n_past = n_tokens;

    // ── Sampler chain ─────────────────────────────────────────────────────────
    llama_sampler* sampler = llama_sampler_chain_init(
            llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(TOP_P, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(TEMPERATURE));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(0xFFFFFFFF));

    // ── Autoregressive generation loop ────────────────────────────────────────
    char piece[MAX_TOKEN_PIECE_SIZE];
    bool ok = true;

    for (int i = 0; i < MAX_NEW_TOKENS; ++i) {
        llama_token new_token = llama_sampler_sample(sampler, state->context, -1);

        if (llama_token_is_eog(state->model, new_token)) {
            LOGD("EOS reached at step %d", i);
            break;
        }

        // Convert token id to its string piece
        int n_piece = llama_token_to_piece(
                state->model, new_token, piece, MAX_TOKEN_PIECE_SIZE - 1, 0, false);
        if (n_piece <= 0) {
            LOGE("llama_token_to_piece failed for token %d", new_token);
            ok = false;
            break;
        }
        piece[n_piece] = '\0';

        // Invoke the Kotlin callback with this piece
        jstring j_piece = env->NewStringUTF(piece);
        if (j_piece) {
            env->CallObjectMethod(token_callback, cb_invoke, j_piece);
            env->DeleteLocalRef(j_piece);
        }

        if (env->ExceptionCheck()) {
            LOGE("JVM exception during token callback at step %d", i);
            env->ExceptionClear();
            ok = false;
            break;
        }

        // Advance context by one token
        llama_batch next = llama_batch_get_one(&new_token, 1, n_past, 0);
        if (llama_decode(state->context, next) != 0) {
            LOGE("llama_decode failed at step %d", i);
            ok = false;
            break;
        }
        ++n_past;
    }

    llama_sampler_free(sampler);
    env->ReleaseStringUTFChars(prompt_jstr, prompt);
    LOGD("Response generation complete (ok=%d, n_past=%d)", (int)ok, n_past);
    return ok ? JNI_TRUE : JNI_FALSE;
}

// ---------------------------------------------------------------------------
// JNI: nativeCountTokens
// Returns the number of tokens the model's tokenizer would produce for the
// given text. Used for context-window budget checks. Returns -1 on error.
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT jint JNICALL
Java_com_example_emotionawareai_engine_LLMEngine_nativeCountTokens(
        JNIEnv* env,
        jobject /* thiz */,
        jlong   handle,
        jstring text_jstr) {

    if (handle == 0L) return -1;
    auto* state = reinterpret_cast<ModelState*>(handle);
    if (!state->model) return -1;

    const char* text = env->GetStringUTFChars(text_jstr, nullptr);
    if (!text) return -1;

    // Pass a temporary buffer; we only care about the count.
    std::vector<llama_token> tmp(MAX_TOKEN_COUNT_BUFFER);
    int n = llama_tokenize(
            state->model, text, static_cast<int>(strlen(text)),
            tmp.data(), static_cast<int>(tmp.size()),
            /*add_special=*/false, /*parse_special=*/false);
    env->ReleaseStringUTFChars(text_jstr, text);

    // Negative result means the buffer was too small; negate to get the count.
    return n < 0 ? -n : n;
}

// ---------------------------------------------------------------------------
// JNI: nativeGetLastError
// Returns a human-readable description of the most recent failure.
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_emotionawareai_engine_LLMEngine_nativeGetLastError(
        JNIEnv* env,
        jobject /* thiz */) {
    std::lock_guard<std::mutex> lock(g_error_mutex);
    return env->NewStringUTF(g_last_error.c_str());
}

// ---------------------------------------------------------------------------
// JNI: nativeReleaseModel
// Frees the inference context and model weights.
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_example_emotionawareai_engine_LLMEngine_nativeReleaseModel(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong handle) {

    if (handle == 0L) return;
    auto* state = reinterpret_cast<ModelState*>(handle);

    if (state->context) {
        llama_free(state->context);
        state->context = nullptr;
    }
    if (state->model) {
        llama_free_model(state->model);
        state->model = nullptr;
    }

    LOGI("Model released: %s", state->model_path.c_str());
    delete state;
}
