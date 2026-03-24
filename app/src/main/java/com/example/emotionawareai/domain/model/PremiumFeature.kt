package com.example.emotionawareai.domain.model

enum class PremiumFeature(val title: String, val description: String) {
    ADVANCED_TONE_INSIGHTS(
        title = "Advanced Tone Insights",
        description = "Energy, volatility, and confidence from your voice tone"
    ),
    PRO_THEMES(
        title = "Pro Themes",
        description = "Glassmorphism and premium visual packs"
    ),
    LONG_MEMORY(
        title = "Long Memory",
        description = "Up to 20 recent turns for deeper, more context-aware conversations"
    ),
    EXPORT_CHAT(
        title = "Export & Share",
        description = "Share conversations with optional emotion and tone insights"
    ),
    CONTINUOUS_CONVERSATION(
        title = "Continuous Conversation",
        description = "Hands-free back-and-forth voice mode"
    ),
    WEEKLY_INSIGHTS(
        title = "Weekly Insights",
        description = "AI-generated weekly growth report: mood trends, themes, and next steps"
    ),
    UNLIMITED_GOALS(
        title = "Unlimited Goals",
        description = "Track and grow across multiple life areas simultaneously"
    )
}

