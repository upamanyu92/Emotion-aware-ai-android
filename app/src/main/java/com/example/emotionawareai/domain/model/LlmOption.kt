package com.example.emotionawareai.domain.model

/**
 * Represents an LLM option available for on-device inference.
 *
 * @param id Unique identifier for this LLM option.
 * @param name Human-readable name.
 * @param description Short description of the model capabilities.
 * @param sizeBytes Approximate download/disk size in bytes (0 for built-in).
 * @param minRamMb Minimum device RAM in MB required to run this model.
 * @param qualityRating Quality rating 1-5.
 * @param isBuiltIn True if this LLM is built into the device (e.g. Gemini Nano on Pixel 8+).
 * @param downloadUrl URL to download the model GGUF (null for built-in).
 * @param modelFileName On-disk file name used by [LLMEngine].
 */
data class LlmOption(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val minRamMb: Int,
    val qualityRating: Int,
    val isBuiltIn: Boolean = false,
    val downloadUrl: String? = null,
    val modelFileName: String = "model.gguf"
) {
    /** Human-readable size string. */
    val sizeLabel: String
        get() = when {
            isBuiltIn -> "Built-in"
            sizeBytes >= 1_000_000_000 -> "%.1f GB".format(sizeBytes / 1_000_000_000.0)
            sizeBytes >= 1_000_000 -> "%d MB".format(sizeBytes / 1_000_000)
            else -> "%d KB".format(sizeBytes / 1_000)
        }

    companion object {
        /** Built-in Gemini Nano option for Pixel 8+ devices. */
        val GEMINI_NANO = LlmOption(
            id = "gemini_nano_builtin",
            name = "Gemini Nano (Built-in)",
            description = "Google\u2019s on-device AI, optimised for Pixel. Zero download, lowest resource usage.",
            sizeBytes = 0,
            minRamMb = 0,
            qualityRating = 4,
            isBuiltIn = true
        )

        /** Microsoft BitNet b1.58 2B — current default model. */
        val BITNET_2B = LlmOption(
            id = "bitnet_b158_2b",
            name = "BitNet b1.58 2B",
            description = "Microsoft\u2019s ultra-efficient 1-bit LLM. Great balance of speed and quality.",
            sizeBytes = 500_000_000L,
            minRamMb = 4_096,
            qualityRating = 3,
            downloadUrl = "https://huggingface.co/microsoft/BitNet-b1.58-2B-4T-GGUF/resolve/main/ggml-model-i2_s.gguf",
            modelFileName = "model.gguf"
        )

        /** Google Gemma 2B. */
        val GEMMA_2B = LlmOption(
            id = "gemma_2b",
            name = "Gemma 2B",
            description = "Google\u2019s compact open model. Good general-purpose quality.",
            sizeBytes = 1_500_000_000L,
            minRamMb = 4_096,
            qualityRating = 4,
            downloadUrl = "https://huggingface.co/google/gemma-2b-it-GGUF/resolve/main/gemma-2b-it-q4_k_m.gguf",
            modelFileName = "model.gguf"
        )

        /** TinyLlama 1.1B. */
        val TINYLLAMA_1B = LlmOption(
            id = "tinyllama_1b",
            name = "TinyLlama 1.1B",
            description = "Ultra-lightweight model. Fastest responses, ideal for low-end devices.",
            sizeBytes = 670_000_000L,
            minRamMb = 3_072,
            qualityRating = 2,
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            modelFileName = "model.gguf"
        )

        /** Phi-3 Mini 3.8B. */
        val PHI3_MINI = LlmOption(
            id = "phi3_mini",
            name = "Phi-3 Mini 3.8B",
            description = "Microsoft\u2019s high-quality small model. Best reasoning in its class.",
            sizeBytes = 2_300_000_000L,
            minRamMb = 6_144,
            qualityRating = 5,
            downloadUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf",
            modelFileName = "model.gguf"
        )

        /** Mistral 7B — for high-end devices. */
        val MISTRAL_7B = LlmOption(
            id = "mistral_7b",
            name = "Mistral 7B",
            description = "Premium quality model. Requires high-end device with 8 GB+ RAM.",
            sizeBytes = 4_100_000_000L,
            minRamMb = 8_192,
            qualityRating = 5,
            downloadUrl = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf",
            modelFileName = "model.gguf"
        )

        /** All downloadable options (excluding built-in). */
        val DOWNLOADABLE_OPTIONS = listOf(BITNET_2B, TINYLLAMA_1B, GEMMA_2B, PHI3_MINI, MISTRAL_7B)

        /** Returns the full catalogue, optionally including the built-in option. */
        fun allOptions(includeBuiltIn: Boolean): List<LlmOption> =
            if (includeBuiltIn) listOf(GEMINI_NANO) + DOWNLOADABLE_OPTIONS
            else DOWNLOADABLE_OPTIONS
    }
}
