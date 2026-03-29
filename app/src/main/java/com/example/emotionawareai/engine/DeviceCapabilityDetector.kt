package com.example.emotionawareai.engine

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.emotionawareai.domain.model.LlmOption
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects device hardware capabilities and determines whether a built-in
 * on-device LLM (Google AI Core / Gemini Nano) is available.
 *
 * Pixel 8 and above ship with Google AI Core which provides Gemini Nano
 * for on-device inference without downloading a separate model.
 */
@Singleton
class DeviceCapabilityDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Device manufacturer (e.g. "Google"). */
    val manufacturer: String = Build.MANUFACTURER

    /** Device model (e.g. "Pixel 8 Pro"). */
    val model: String = Build.MODEL

    /** Total device RAM in MB. */
    val totalRamMb: Int by lazy {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        (memInfo.totalMem / (1024 * 1024)).toInt()
    }

    /** Android SDK version. */
    val sdkVersion: Int = Build.VERSION.SDK_INT

    /** SoC / chipset name when available (API 31+). */
    val chipset: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { "Unknown" }
        } else {
            "Unknown"
        }

    // ── Pixel 8+ detection ───────────────────────────────────────────────────

    /**
     * Returns `true` when the device is a Google Pixel with generation 8 or
     * above. These devices include Google Tensor G3+ which supports AI Core.
     */
    val isPixel8OrAbove: Boolean by lazy {
        if (!manufacturer.equals("Google", ignoreCase = true)) return@lazy false
        val pixelGeneration = extractPixelGeneration(model)
        val result = pixelGeneration != null && pixelGeneration >= 8
        Log.i(TAG, "Pixel generation: $pixelGeneration, isPixel8OrAbove=$result")
        result
    }

    /**
     * Returns `true` when Google AI Core service is installed on the device.
     * AI Core hosts Gemini Nano for on-device inference.
     */
    val isAiCoreInstalled: Boolean by lazy {
        try {
            context.packageManager.getPackageInfo(AI_CORE_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** `true` when the device supports built-in LLM (Pixel 8+ with AI Core). */
    val hasBuiltInLlm: Boolean by lazy {
        val result = isPixel8OrAbove && isAiCoreInstalled
        Log.i(TAG, "hasBuiltInLlm=$result (pixel8+=$isPixel8OrAbove, aiCore=$isAiCoreInstalled)")
        result
    }

    // ── Compatibility scoring ────────────────────────────────────────────────

    /**
     * A compatibility descriptor for a given [LlmOption] on this device.
     */
    data class Compatibility(
        /** 0-100 score. 100 = perfect fit. */
        val score: Int,
        /** Human-readable label: "Excellent", "Good", "Fair", "Not recommended". */
        val label: String,
        /** Estimated RAM headroom after loading the model (MB, may be negative). */
        val ramHeadroomMb: Int,
        /** True if the model is the recommended default for this device. */
        val isRecommended: Boolean = false
    )

    /**
     * Evaluates how well [option] fits the current device.
     */
    fun evaluateCompatibility(option: LlmOption): Compatibility {
        if (option.isBuiltIn) {
            return Compatibility(
                score = 100,
                label = "Excellent",
                ramHeadroomMb = totalRamMb,
                isRecommended = true
            )
        }

        val headroom = totalRamMb - option.minRamMb
        val score = when {
            headroom >= 4096 -> 100
            headroom >= 2048 -> 85
            headroom >= 1024 -> 70
            headroom >= 0 -> 50
            headroom >= -512 -> 30
            else -> 10
        }
        val label = when {
            score >= 85 -> "Excellent"
            score >= 70 -> "Good"
            score >= 50 -> "Fair"
            else -> "Not recommended"
        }
        return Compatibility(score = score, label = label, ramHeadroomMb = headroom)
    }

    /**
     * Returns the best LLM option for this device — minimum resource usage with
     * highest output quality among compatible models.
     */
    fun recommendedOption(): LlmOption {
        if (hasBuiltInLlm) return LlmOption.GEMINI_NANO

        val options = LlmOption.DOWNLOADABLE_OPTIONS
        // Sort by: compatible first (score >= 50), then highest quality, then smallest size
        return options
            .map { it to evaluateCompatibility(it) }
            .filter { (_, compat) -> compat.score >= 50 }
            .sortedWith(compareByDescending<Pair<LlmOption, Compatibility>> { it.first.qualityRating }
                .thenBy { it.first.sizeBytes })
            .firstOrNull()?.first
            ?: LlmOption.BITNET_2B // fallback to BitNet if nothing scores well
    }

    /**
     * Returns all options with compatibility info, marking the recommended one.
     */
    fun allOptionsWithCompatibility(): List<Pair<LlmOption, Compatibility>> {
        val recommended = recommendedOption()
        val includeBuiltIn = hasBuiltInLlm
        return LlmOption.allOptions(includeBuiltIn).map { option ->
            val compat = evaluateCompatibility(option)
            option to compat.copy(isRecommended = option.id == recommended.id)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Extracts the numeric Pixel generation from the model string.
     * E.g. "Pixel 8 Pro" → 8, "Pixel 9" → 9, "Pixel 7a" → 7.
     */
    private fun extractPixelGeneration(model: String): Int? {
        val regex = Regex("""Pixel\s+(\d+)""", RegexOption.IGNORE_CASE)
        return regex.find(model)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    companion object {
        private const val TAG = "DeviceCapability"
        /** Google AI Core package that hosts Gemini Nano on-device. */
        private const val AI_CORE_PACKAGE = "com.google.android.aicore"
    }
}
