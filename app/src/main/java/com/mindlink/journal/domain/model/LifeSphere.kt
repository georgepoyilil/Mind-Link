package com.mindlink.journal.domain.model

/**
 * Domain-specific Context Silos ("Life Spheres") that isolate different facets of life.
 * Firewalls intimate/relational entries from software engineering dilemmas or physical desk fatigue,
 * ensuring prompts and reminders remain deeply relevant and tone-appropriate as the journal scales.
 */
enum class LifeSphere(val displayName: String) {
    RELATIONAL("Intimacy & Relationships"),
    WORK_ENGINEERING("Engineering & Work"),
    PHYSICAL_WELLBEING("Physical Wellbeing & Rest"),
    GROWTH_REFLECTION("Growth & Self-Doubt"),
    GENERAL("Daily Life")
}
