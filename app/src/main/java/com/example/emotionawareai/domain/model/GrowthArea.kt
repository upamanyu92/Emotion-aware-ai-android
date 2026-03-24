package com.example.emotionawareai.domain.model

/**
 * Growth focus areas that the user can select during onboarding or from Goals screen.
 */
enum class GrowthArea(val displayName: String, val emoji: String, val description: String) {
    ANXIETY("Anxiety & Worry", "😰", "Understand and manage anxious thoughts"),
    STRESS("Stress & Burnout", "😤", "Build resilience and healthy boundaries"),
    MOTIVATION("Motivation & Goals", "🎯", "Find direction and sustain momentum"),
    RELATIONSHIPS("Relationships", "💛", "Improve connection and communication"),
    SELF_ESTEEM("Self-Esteem", "🌟", "Build confidence and self-compassion"),
    GRIEF("Grief & Loss", "🕊️", "Process difficult emotions and heal"),
    SLEEP("Sleep & Energy", "😴", "Improve rest and daily energy"),
    MINDFULNESS("Mindfulness", "🧘", "Stay present and reduce mental noise")
}
