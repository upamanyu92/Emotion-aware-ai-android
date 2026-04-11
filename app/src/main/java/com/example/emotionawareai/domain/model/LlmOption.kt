package com.example.emotionawareai.domain.model

/**
 * Represents an LLM option available for on-device inference.
 *
 * @param id Unique identifier for this LLM option.
 * @param name User-friendly display name shown in the model picker UI.
 * @param technicalName Technical model name for advanced reference (e.g. "Qwen 2.5 7B Instruct").
 * @param description Short description of the model capabilities.
 * @param sizeBytes Approximate download/disk size in bytes (0 for built-in).
 * @param minRamMb Minimum device RAM in MB required to run this model.
 * @param qualityRating Quality rating 1-5.
 * @param isBuiltIn True if this LLM is built into the device (e.g. Gemini Nano on Pixel 8+).
 * @param downloadUrl URL to download the model GGUF (null for built-in).
 * @param modelFileName On-disk file name used by [LLMEngine].
 * @param requiresWarning True for heavy models that may cause thermal throttling or instability
 *   on low-end devices. The UI will show a safety warning before confirming such a download.
 * @param parameterLabel Short string describing parameter count (e.g. "1.5B params").
 * @param idealUseCase One-line description of the best agentic use case for this model.
 */
data class LlmOption(
    val id: String,
    val name: String,
    val technicalName: String = "",
    val description: String,
    val sizeBytes: Long,
    val minRamMb: Int,
    val qualityRating: Int,
    val isBuiltIn: Boolean = false,
    val downloadUrl: String? = null,
    val modelFileName: String = "model.gguf",
    val requiresWarning: Boolean = false,
    val parameterLabel: String = "",
    val idealUseCase: String = ""
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
            name = "Gemini Nano",
            technicalName = "Gemini Nano (Built-in)",
            description = "Google\u2019s on-device AI built right into Pixel phones. Zero download needed.",
            sizeBytes = 0,
            minRamMb = 0,
            qualityRating = 4,
            isBuiltIn = true,
            parameterLabel = "Built-in",
            idealUseCase = "Fast everyday chat and emotional support with zero setup"
        )

        /** Hugging Face SmolLM2 135M — smallest available model for any device. */
        val SMOLLM2_135M = LlmOption(
            id = "smollm2_135m",
            name = "Pocket Brain",
            technicalName = "SmolLM2 135M",
            description = "Ultra-tiny model under 100 MB. Instant responses on any Android phone.",
            sizeBytes = 90_000_000L,
            minRamMb = 1_024,
            qualityRating = 2,
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_0_4_4.gguf",
            modelFileName = "model.gguf",
            parameterLabel = "135M params",
            idealUseCase = "Quick replies and basic Q&A on very low-end devices"
        )

        /** TinyLlama 1.1B — very small, fast. */
        val TINYLLAMA_1B = LlmOption(
            id = "tinyllama_1b",
            name = "Speedy Spark",
            technicalName = "TinyLlama 1.1B",
            description = "Lightning-fast lightweight model. Great for quick responses on entry-level devices.",
            sizeBytes = 670_000_000L,
            minRamMb = 3_072,
            qualityRating = 2,
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            modelFileName = "model.gguf",
            parameterLabel = "1.1B params",
            idealUseCase = "Snappy emotional check-ins and short conversations"
        )

        /** Microsoft BitNet b1.58 2B — ultra-efficient 1-bit LLM. */
        val BITNET_2B = LlmOption(
            id = "bitnet_b158_2b",
            name = "Energy Saver",
            technicalName = "BitNet b1.58 2B",
            description = "Microsoft\u2019s revolutionary 1-bit AI — uses minimal battery and memory.",
            sizeBytes = 500_000_000L,
            minRamMb = 4_096,
            qualityRating = 3,
            downloadUrl = "https://huggingface.co/microsoft/BitNet-b1.58-2B-4T-GGUF/resolve/main/ggml-model-i2_s.gguf",
            modelFileName = "model.gguf",
            parameterLabel = "2B params (1-bit)",
            idealUseCase = "Long conversations with minimal battery drain"
        )

        /** Google Gemma 2B — good general-purpose model. */
        val GEMMA_2B = LlmOption(
            id = "gemma_2b",
            name = "Balanced Mind",
            technicalName = "Gemma 2B",
            description = "Google\u2019s well-rounded model. Solid quality for everyday emotional conversations.",
            sizeBytes = 1_500_000_000L,
            minRamMb = 4_096,
            qualityRating = 4,
            downloadUrl = "https://huggingface.co/google/gemma-2b-it-GGUF/resolve/main/gemma-2b-it-q4_k_m.gguf",
            modelFileName = "model.gguf",
            parameterLabel = "2B params",
            idealUseCase = "Balanced daily companion — good quality without high RAM"
        )

        /** Qwen 2.5 Instruct 1.5B — optimized for structured output and coding. */
        val QWEN_25_1B5 = LlmOption(
            id = "qwen25_1b5",
            name = "Quick Wit",
            technicalName = "Qwen 2.5 1.5B Instruct",
            description = "Alibaba\u2019s compact instruct model, optimized for structured responses on 6 GB+ phones.",
            sizeBytes = 1_000_000_000L,
            minRamMb = 6_144,
            qualityRating = 3,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            modelFileName = "model.gguf",
            parameterLabel = "1.5B params",
            idealUseCase = "Structured data generation and formatted emotional insights"
        )

        /** Llama 3.2 Instruct 3B — excellent edge-device tool use. */
        val LLAMA_32_3B = LlmOption(
            id = "llama32_3b",
            name = "Chat Expert",
            technicalName = "Llama 3.2 3B Instruct",
            description = "Meta\u2019s latest edge model. Excellent at following instructions and natural conversation.",
            sizeBytes = 2_000_000_000L,
            minRamMb = 6_144,
            qualityRating = 4,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            modelFileName = "model.gguf",
            parameterLabel = "3B params",
            idealUseCase = "Natural emotional conversations with strong context understanding"
        )

        /** Phi-3 Mini 3.8B — strong reasoning from textbook training. */
        val PHI3_MINI = LlmOption(
            id = "phi3_mini",
            name = "Sharp Thinker",
            technicalName = "Phi-3.5 Mini 3.8B",
            description = "Microsoft\u2019s reasoning-focused model. Punches above its size on logic tasks.",
            sizeBytes = 2_300_000_000L,
            minRamMb = 6_144,
            qualityRating = 5,
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            modelFileName = "model.gguf",
            parameterLabel = "3.8B params",
            idealUseCase = "Deep emotional reasoning and logical self-reflection"
        )

        /** Qwen 2.5 Instruct 7B — top-tier coding and large context. */
        val QWEN_25_7B = LlmOption(
            id = "qwen25_7b",
            name = "Code Wizard",
            technicalName = "Qwen 2.5 7B Instruct",
            description = "High-quality 7B model with massive context window. Requires 8\u201312 GB RAM.",
            sizeBytes = 4_700_000_000L,
            minRamMb = 10_240,
            qualityRating = 5,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-7B-Instruct-GGUF/resolve/main/qwen2.5-7b-instruct-q4_k_m.gguf",
            modelFileName = "model.gguf",
            requiresWarning = true,
            parameterLabel = "7B params",
            idealUseCase = "Complex goal planning and predictive emotional trend analysis"
        )

        /** DeepSeek-R1-Distill 7B — Chain-of-Thought reasoning with transparent logic. */
        val DEEPSEEK_R1_7B = LlmOption(
            id = "deepseek_r1_7b",
            name = "Deep Analyst",
            technicalName = "DeepSeek-R1-Distill 7B",
            description = "Transparent step-by-step reasoning via Chain-of-Thought. Requires 8\u201312 GB RAM.",
            sizeBytes = 4_700_000_000L,
            minRamMb = 10_240,
            qualityRating = 5,
            downloadUrl = "https://huggingface.co/bartowski/DeepSeek-R1-Distill-Qwen-7B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-7B-Q4_K_M.gguf",
            modelFileName = "model.gguf",
            requiresWarning = true,
            parameterLabel = "7B params",
            idealUseCase = "Detailed emotional analysis with verifiable reasoning steps"
        )

        /** Mistral 7B — premium quality for high-end devices. */
        val MISTRAL_7B = LlmOption(
            id = "mistral_7b",
            name = "Master Mind",
            technicalName = "Mistral 7B Instruct",
            description = "Highly capable open model with premium response quality. Requires 8 GB+ RAM.",
            sizeBytes = 4_100_000_000L,
            minRamMb = 8_192,
            qualityRating = 5,
            downloadUrl = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf",
            modelFileName = "model.gguf",
            requiresWarning = true,
            parameterLabel = "7B params",
            idealUseCase = "Rich, nuanced emotional conversations and journaling support"
        )

        /** Qwen 2.5 Instruct 14B — enterprise-grade reasoning for flagship devices. */
        val QWEN_25_14B = LlmOption(
            id = "qwen25_14b",
            name = "Enterprise Brain",
            technicalName = "Qwen 2.5 14B Instruct",
            description = "Enterprise-grade reasoning and complex system design. Requires 16\u201324 GB RAM.",
            sizeBytes = 9_000_000_000L,
            minRamMb = 20_480,
            qualityRating = 5,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-14B-Instruct-GGUF/resolve/main/qwen2.5-14b-instruct-q4_k_m.gguf",
            modelFileName = "model.gguf",
            requiresWarning = true,
            parameterLabel = "14B params",
            idealUseCase = "End-to-end emotional architecture and complex multi-step reasoning"
        )

        /** Phi-4 14B — advanced mathematics and data parsing for flagship devices. */
        val PHI4_14B = LlmOption(
            id = "phi4_14b",
            name = "Logic Genius",
            technicalName = "Phi-4 14B",
            description = "Bleeding-edge logic, math, and data parsing. Requires 16\u201324 GB RAM.",
            sizeBytes = 8_500_000_000L,
            minRamMb = 20_480,
            qualityRating = 5,
            downloadUrl = "https://huggingface.co/microsoft/phi-4-gguf/resolve/main/phi-4-q4.gguf",
            modelFileName = "model.gguf",
            requiresWarning = true,
            parameterLabel = "14B params",
            idealUseCase = "Advanced emotional data parsing and trend forecasting"
        )

        /**
         * The fallback pre-configured model for new installations when no selection
         * has been made. SmolLM2 135M is under 100 MB and requires no HF token.
         */
        val CONFIGURED_MODEL: LlmOption = SMOLLM2_135M

        /** All downloadable options ordered from smallest to largest (excluding built-in). */
        val DOWNLOADABLE_OPTIONS = listOf(
            SMOLLM2_135M,
            TINYLLAMA_1B,
            BITNET_2B,
            GEMMA_2B,
            QWEN_25_1B5,
            LLAMA_32_3B,
            PHI3_MINI,
            MISTRAL_7B,
            QWEN_25_7B,
            DEEPSEEK_R1_7B,
            QWEN_25_14B,
            PHI4_14B
        )

        /** Returns the full catalogue, optionally including the built-in option. */
        fun allOptions(includeBuiltIn: Boolean): List<LlmOption> =
            if (includeBuiltIn) listOf(GEMINI_NANO) + DOWNLOADABLE_OPTIONS
            else DOWNLOADABLE_OPTIONS

        /** Returns the option matching [id], or null when the ID is unknown. */
        fun fromId(id: String): LlmOption? =
            allOptions(includeBuiltIn = true).firstOrNull { it.id == id }
    }
}
