package com.example.emotionawareai.domain.model

/**
 * 22-metric taxonomy for evaluating on-device AI agent performance.
 *
 * Each metric is scored 0.0–1.0 where 1.0 is ideal. Metrics are grouped
 * into categories that cover the full lifecycle of an AI companion interaction.
 */
enum class EvaluationMetric(
    val displayName: String,
    val category: MetricCategory,
    val description: String
) {
    // ── Relevance & Accuracy ────────────────────────────────────────────────
    RESPONSE_RELEVANCE(
        "Response Relevance",
        MetricCategory.ACCURACY,
        "How well the response addresses the user's query"
    ),
    FACTUAL_ACCURACY(
        "Factual Accuracy",
        MetricCategory.ACCURACY,
        "Correctness of claims and information in the response"
    ),
    CONTEXT_ADHERENCE(
        "Context Adherence",
        MetricCategory.ACCURACY,
        "Consistency with conversation history and user context"
    ),

    // ── Emotional Intelligence ──────────────────────────────────────────────
    EMOTION_RECOGNITION(
        "Emotion Recognition",
        MetricCategory.EMOTIONAL_IQ,
        "Accuracy of detected facial/voice emotion vs. user expectation"
    ),
    EMPATHY_QUALITY(
        "Empathy Quality",
        MetricCategory.EMOTIONAL_IQ,
        "Degree of empathetic and compassionate tone in responses"
    ),
    SENTIMENT_ALIGNMENT(
        "Sentiment Alignment",
        MetricCategory.EMOTIONAL_IQ,
        "Alignment of response sentiment with user emotional state"
    ),
    TONE_APPROPRIATENESS(
        "Tone Appropriateness",
        MetricCategory.EMOTIONAL_IQ,
        "Suitability of response tone for the conversational context"
    ),

    // ── Safety & Harmlessness ───────────────────────────────────────────────
    SAFETY_COMPLIANCE(
        "Safety Compliance",
        MetricCategory.SAFETY,
        "Absence of harmful, toxic, or inappropriate content"
    ),
    BOUNDARY_RESPECT(
        "Boundary Respect",
        MetricCategory.SAFETY,
        "Respect for user privacy and emotional boundaries"
    ),
    CRISIS_HANDLING(
        "Crisis Handling",
        MetricCategory.SAFETY,
        "Appropriate response to distress signals or crisis language"
    ),

    // ── Fluency & Coherence ─────────────────────────────────────────────────
    LANGUAGE_FLUENCY(
        "Language Fluency",
        MetricCategory.FLUENCY,
        "Grammatical correctness and natural language flow"
    ),
    COHERENCE(
        "Coherence",
        MetricCategory.FLUENCY,
        "Logical consistency and narrative flow within the response"
    ),
    CONCISENESS(
        "Conciseness",
        MetricCategory.FLUENCY,
        "Appropriate response length without unnecessary verbosity"
    ),

    // ── Helpfulness & Engagement ────────────────────────────────────────────
    HELPFULNESS(
        "Helpfulness",
        MetricCategory.ENGAGEMENT,
        "Actionable advice or useful information provided"
    ),
    ENGAGEMENT_QUALITY(
        "Engagement Quality",
        MetricCategory.ENGAGEMENT,
        "Ability to maintain user interest and conversational flow"
    ),
    FOLLOW_UP_QUALITY(
        "Follow-Up Quality",
        MetricCategory.ENGAGEMENT,
        "Quality of follow-up questions and conversation continuity"
    ),

    // ── Memory & Personalization ────────────────────────────────────────────
    MEMORY_UTILIZATION(
        "Memory Utilization",
        MetricCategory.PERSONALIZATION,
        "Effective use of stored user context and memory fragments"
    ),
    PERSONALIZATION_DEPTH(
        "Personalization Depth",
        MetricCategory.PERSONALIZATION,
        "Degree of response tailoring to individual user"
    ),

    // ── Performance & Latency ───────────────────────────────────────────────
    RESPONSE_LATENCY(
        "Response Latency",
        MetricCategory.PERFORMANCE,
        "Time taken to generate first token and complete response"
    ),
    TOKEN_EFFICIENCY(
        "Token Efficiency",
        MetricCategory.PERFORMANCE,
        "Ratio of useful content to total generated tokens"
    ),

    // ── Summarization & Comprehension ───────────────────────────────────────
    SUMMARIZATION_QUALITY(
        "Summarization Quality",
        MetricCategory.COMPREHENSION,
        "Quality of daily diary and conversation summaries"
    ),
    COMPREHENSION_DEPTH(
        "Comprehension Depth",
        MetricCategory.COMPREHENSION,
        "Depth of understanding user intent and subtext"
    );

    companion object {
        fun byCategory(category: MetricCategory): List<EvaluationMetric> =
            entries.filter { it.category == category }
    }
}

enum class MetricCategory(val displayName: String) {
    ACCURACY("Relevance & Accuracy"),
    EMOTIONAL_IQ("Emotional Intelligence"),
    SAFETY("Safety & Harmlessness"),
    FLUENCY("Fluency & Coherence"),
    ENGAGEMENT("Helpfulness & Engagement"),
    PERSONALIZATION("Memory & Personalization"),
    PERFORMANCE("Performance & Latency"),
    COMPREHENSION("Summarization & Comprehension")
}
