package com.mindlink.journal.domain.model

import java.util.Locale

object LifeSphereClassifier {

    data class ClassificationResult(
        val primarySphere: LifeSphere,
        val detectedEntities: List<String>,
        val relationalScore: Int,
        val workScore: Int,
        val physicalScore: Int,
        val growthScore: Int
    )

    fun classify(title: String, content: String): ClassificationResult {
        val fullText = "$title\n$content".lowercase(Locale.ROOT)

        val relationalEntities = listOf(
            "elena", "mom", "dad", "mother", "father", "wife", "husband",
            "partner", "girlfriend", "boyfriend", "daughter", "son", "parents",
            "family", "sarah", "alex", "best friend"
        ).filter { fullText.contains(it) }

        var relationalScore = 0
        if (relationalEntities.isNotEmpty()) relationalScore += 5
        val relationalKeywords = listOf(
            "argument", "dishes", "dinner", "couch", "living room", "held hands", "tears",
            "miss", "love", "relationship", "intimacy", "unspoken", "vulnerab", "guilt",
            "distract", "connected", "disconnection", "date night", "partner", "spouse"
        )
        relationalKeywords.forEach { if (fullText.contains(it)) relationalScore += 2 }

        var physicalScore = 0
        // Physical strain must have explicit symptom words, NOT just sitting down on a couch!
        val physicalKeywords = listOf(
            "neck pain", "stiff neck", "neck stiffness", "lower back", "back pain",
            "shoulder ache", "shoulder tightness", "eye strain", "eyes burn", "screen glare",
            "headache", "migraine", "exhausted", "sleep deprived", "insomnia", "past 2 am",
            "past 1 am", "spine mobility", "poor posture", "ergonomic"
        )
        physicalKeywords.forEach { if (fullText.contains(it)) physicalScore += 3 }

        var workScore = 0
        val workKeywords = listOf(
            "pull request", "schema migration", "staging", "production traffic", "rollback script",
            "integration test", "vulkan", "shader", "memory leak", "database schema",
            "api endpoint", "refactoring", "microservice", "deploy", "ci/cd", "latency"
        )
        workKeywords.forEach { if (fullText.contains(it)) workScore += 3 }

        var growthScore = 0
        val growthKeywords = listOf(
            "perfectionism", "imposter", "fear of making", "wrong choice", "paralyzed",
            "reputation", "zero failure", "illusion of absolute", "courage", "self-doubt",
            "decision", "dilemma", "mindset"
        )
        growthKeywords.forEach { if (fullText.contains(it)) growthScore += 2 }

        // Determine primary domain with strict precedence:
        // Relational conflict/presence has top emotional precedence when clear entities are involved.
        val primary = when {
            relationalScore >= 4 && relationalScore >= physicalScore && relationalScore >= (workScore - 2) -> {
                LifeSphere.RELATIONAL
            }
            growthScore >= 4 && growthScore >= workScore && growthScore >= physicalScore -> {
                LifeSphere.GROWTH_REFLECTION
            }
            workScore >= 4 && workScore >= physicalScore -> {
                LifeSphere.WORK_ENGINEERING
            }
            physicalScore >= 3 -> {
                LifeSphere.PHYSICAL_WELLBEING
            }
            relationalScore > 0 -> {
                LifeSphere.RELATIONAL
            }
            growthScore > 0 -> {
                LifeSphere.GROWTH_REFLECTION
            }
            workScore > 0 -> {
                LifeSphere.WORK_ENGINEERING
            }
            else -> LifeSphere.GENERAL
        }

        return ClassificationResult(
            primarySphere = primary,
            detectedEntities = relationalEntities,
            relationalScore = relationalScore,
            workScore = workScore,
            physicalScore = physicalScore,
            growthScore = growthScore
        )
    }
}
