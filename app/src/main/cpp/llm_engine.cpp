#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <android/log.h>

#define LOG_TAG "LLMEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// ---------------------------------------------------------------------------
// Internal state – replace with llama_model* / llama_context* when integrating
// bitnet.cpp (Microsoft's 1-bit LLM runtime, a fork of llama.cpp).
// The model file is the Microsoft BitNet b1.58 2B GGUF downloaded by
// ModelDownloader.kt. Pointers are stored as jlong handles passed to Kotlin.
// ---------------------------------------------------------------------------

struct ModelState {
    std::string model_path;
    bool is_loaded = false;
    // llama_model*   model   = nullptr;  // bitnet.cpp integration point
    // llama_context* context = nullptr;  // bitnet.cpp integration point
};

// ---------------------------------------------------------------------------
// JNI: nativeLoadModel
// Returns a native handle (pointer cast to jlong) on success, 0 on failure.
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT jlong JNICALL
Java_com_example_emotionawareai_engine_LLMEngine_nativeLoadModel(
        JNIEnv* env,
        jobject /* thiz */,
        jstring model_path_jstr) {

    const char* model_path = env->GetStringUTFChars(model_path_jstr, nullptr);
    if (model_path == nullptr) {
        LOGE("Failed to get model path string");
        return 0L;
    }

    LOGI("Loading model from: %s", model_path);

    auto* state = new ModelState();
    state->model_path = std::string(model_path);
    env->ReleaseStringUTFChars(model_path_jstr, model_path);

    // -----------------------------------------------------------------
    // bitnet.cpp integration point:
    //
    // llama_model_params model_params = llama_model_default_params();
    // model_params.n_gpu_layers = 0; // CPU-only; BitNet uses optimised 1-bit kernels
    // state->model = llama_load_model_from_file(state->model_path.c_str(), model_params);
    // if (state->model == nullptr) {
    //     LOGE("Failed to load BitNet model");
    //     delete state;
    //     return 0L;
    // }
    // llama_context_params ctx_params = llama_context_default_params();
    // ctx_params.n_ctx    = 2048;
    // ctx_params.n_batch  = 512;
    // ctx_params.n_threads = 4;
    // state->context = llama_new_context_with_model(state->model, ctx_params);
    // -----------------------------------------------------------------

    state->is_loaded = true;
    LOGI("Model stub loaded successfully (path: %s)", state->model_path.c_str());

    return reinterpret_cast<jlong>(state);
}

// ---------------------------------------------------------------------------
// JNI: nativeGenerateResponse
// Calls the token callback (Kotlin lambda via JNI) for each generated token.
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
        LOGE("Invalid model handle");
        return JNI_FALSE;
    }

    auto* state = reinterpret_cast<ModelState*>(handle);
    if (!state->is_loaded) {
        LOGE("Model not loaded");
        return JNI_FALSE;
    }

    const char* prompt = env->GetStringUTFChars(prompt_jstr, nullptr);
    if (prompt == nullptr) {
        LOGE("Failed to get prompt string");
        return JNI_FALSE;
    }

    LOGD("Generating response for prompt (first 80 chars): %.80s", prompt);

    // Obtain the callback method reference
    jclass callback_class = env->GetObjectClass(token_callback);
    jmethodID invoke_method = env->GetMethodID(
            callback_class, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");

    if (invoke_method == nullptr) {
        LOGE("Could not find callback invoke method");
        env->ReleaseStringUTFChars(prompt_jstr, prompt);
        return JNI_FALSE;
    }

    // -----------------------------------------------------------------
    // bitnet.cpp integration point:
    //
    // std::vector<llama_token> tokens = llama_tokenize(state->model, prompt, true);
    // llama_eval(state->context, tokens.data(), (int)tokens.size(), 0, 4);
    //
    // for (int i = 0; i < max_tokens; ++i) {
    //     llama_token token = llama_sample_token_greedy(state->context, &candidates);
    //     if (token == llama_token_eos(state->model)) break;
    //     const char* token_str = llama_token_to_piece(state->context, token);
    //     jstring j_token = env->NewStringUTF(token_str);
    //     env->CallObjectMethod(token_callback, invoke_method, j_token);
    //     env->DeleteLocalRef(j_token);
    //     llama_eval(state->context, &token, 1, n_past, 4);
    // }
    // -----------------------------------------------------------------

    // Stub: produce a placeholder response word-by-word to exercise the
    // streaming callback path end-to-end.
    std::string stub_response = "I understand how you're feeling. "
                                "I'm here to listen and help you. "
                                "Tell me more about what's on your mind.";

    std::istringstream stream(stub_response);
    std::string word;
    while (std::getline(stream, word, ' ')) {
        word += ' ';
        jstring j_token = env->NewStringUTF(word.c_str());
        env->CallObjectMethod(token_callback, invoke_method, j_token);
        env->DeleteLocalRef(j_token);

        if (env->ExceptionCheck()) {
            LOGE("Exception during token callback");
            env->ExceptionClear();
            break;
        }
    }

    env->ReleaseStringUTFChars(prompt_jstr, prompt);
    LOGD("Response generation complete");
    return JNI_TRUE;
}

// ---------------------------------------------------------------------------
// JNI: nativeReleaseModel
// Frees all resources held by the model handle.
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_com_example_emotionawareai_engine_LLMEngine_nativeReleaseModel(
        JNIEnv* /* env */,
        jobject /* thiz */,
        jlong handle) {

    if (handle == 0L) return;

    auto* state = reinterpret_cast<ModelState*>(handle);

    // -----------------------------------------------------------------
    // bitnet.cpp integration point:
    //
    // if (state->context) { llama_free(state->context); state->context = nullptr; }
    // if (state->model)   { llama_free_model(state->model); state->model = nullptr; }
    // -----------------------------------------------------------------

    LOGI("Releasing model: %s", state->model_path.c_str());
    delete state;
}
