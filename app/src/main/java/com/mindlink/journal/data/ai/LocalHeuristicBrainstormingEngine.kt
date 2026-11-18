package com.mindlink.journal.data.ai

import com.mindlink.journal.domain.model.LifeSphere
import com.mindlink.journal.domain.model.LifeSphereClassifier
import com.mindlink.journal.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LocalHeuristicBrainstormingEngine : BrainstormingEngine {

    override suspend fun analyzeNote(title: String, content: String): NoteAnalysisResult = withContext(Dispatchers.Default) {
        val fullText = "$title\n$content"

        // 1. Extract Action Items
        val actionItems = extractActionItems(content)

        // 2. Classify Mood
        val mood = classifyMood(fullText)

        // 3. Auto-generate Tags & Categories
        val tags = extractTags(title, content)

        // 4. Generate 1-sentence TL;DR Summary
        val summary = generateSummary(title, content)

        // 5. Generate 2-3 Dynamic Contextual Reflection Prompts
        val reflectionPrompts = generateReflectionPrompts(title, content, mood)

        // 6. Extract Proactive Contextual Reminder (if note exhibits physical/relational strain)
        val contextualReminder = extractContextualReminder(title, content)

        NoteAnalysisResult(
            summary = summary,
            mood = mood,
            tags = tags,
            actionItems = actionItems,
            reflectionPrompts = reflectionPrompts,
            contextualReminder = contextualReminder
        )
    }

    override suspend fun synthesizeNotes(query: String, notes: List<Note>): SynthesisResult = withContext(Dispatchers.Default) {
        if (notes.isEmpty()) {
            return@withContext SynthesisResult(
                answer = "I searched your journal entries but found no notes yet. Tap the + button to capture your first thought or load sample thoughts in Settings!",
                keyTakeaways = emptyList()
            )
        }

        val lowerQuery = query.lowercase(Locale.ROOT)
        val takeaways = mutableListOf<String>()
        val answerBuilder = StringBuilder()

        // 1. Love, Relationships & Partnership
        if (lowerQuery.contains("love") || lowerQuery.contains("relationship") || lowerQuery.contains("elena") || lowerQuery.contains("partner") || lowerQuery.contains("intima") || lowerQuery.contains("argument") || lowerQuery.contains("date night")) {
            answerBuilder.append("Based on your personal entries regarding love and relationships:\n\n")
            val loveNotes = notes.filter { n ->
                n.tags.any { it == "love" || it.contains("relationship") || it.contains("elena") || it.contains("dating") || it.contains("presence") } ||
                        n.title.lowercase(Locale.ROOT).contains("elena") ||
                        n.title.lowercase(Locale.ROOT).contains("love")
            }.ifEmpty { notes }

            for (note in loveNotes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** (${note.formattedDate}):\n  $preview\n\n")
            }
            answerBuilder.append("\n**Core Reflection:** You cherish deep presence and honest vulnerability with Elena, and you've recognized that protecting evening time without work screens is essential to your bond.")
        }
        // 2. Emotional Vulnerability, Loneliness, Burnout & Mental Health
        else if (lowerQuery.contains("feel") || lowerQuery.contains("emotion") || lowerQuery.contains("vulnerab") || lowerQuery.contains("lonel") || lowerQuery.contains("burnout") || lowerQuery.contains("exhaust") || lowerQuery.contains("mental") || lowerQuery.contains("anxious") || lowerQuery.contains("anxiety") || lowerQuery.contains("fear")) {
            answerBuilder.append("Here is an honest synthesis of your recent emotional patterns, vulnerabilities, and mental bandwidth:\n\n")
            for (note in notes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** [Mood: ${note.mood ?: "Reflective"}]:\n  $preview\n\n")
            }
            answerBuilder.append("\n**Emotional Takeaway:** Your notes show high self-awareness around burnout, the ache of loneliness in a new city, and performance anxiety. Admitting exhaustion and seeking connection are healthy turning points.")
        }
        // 3. Failure, Rejection & Overcoming Setbacks
        else if (lowerQuery.contains("fail") || lowerQuery.contains("reject") || lowerQuery.contains("imposter") || lowerQuery.contains("setback") || lowerQuery.contains("pass") || lowerQuery.contains("regret")) {
            answerBuilder.append("Here is how you've reflected on failure, setbacks, and resilience across your journal:\n\n")
            for (note in notes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** (${note.formattedDate}):\n  $preview\n\n")
            }
            answerBuilder.append("\n**Key Realization:** Rejections (like the Sequoia pre-seed pass or architecture imposter feelings) stung deeply, but you consistently reframed them from personal verdicts into catalysts for bootstrapping and self-trust.")
        }
        // 4. Family, Parents, Aging & Grief
        else if (lowerQuery.contains("family") || lowerQuery.contains("parent") || lowerQuery.contains("dad") || lowerQuery.contains("mom") || lowerQuery.contains("grandpa") || lowerQuery.contains("grandma") || lowerQuery.contains("grief") || lowerQuery.contains("loss") || lowerQuery.contains("aging")) {
            answerBuilder.append("Here is what your Second Brain holds regarding your family, memories, and aging parents:\n\n")
            val famNotes = notes.filter { n ->
                n.tags.any { it.contains("family") || it.contains("parents") || it.contains("dad") || it.contains("mom") || it.contains("grief") || it.contains("grandpa") }
            }.ifEmpty { notes }

            for (note in famNotes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** (${note.formattedDate}):\n  $preview\n\n")
            }
            answerBuilder.append("\n**Heartfelt Insight:** Family occupies a profound emotional space in your life—from the quiet dread of watching Dad age, to honoring Grandpa Leo's wisdom on patience, to celebrating Mom's milestone 60th birthday.")
        }
        // 5. Gratitude, Joy & Peace
        else if (lowerQuery.contains("gratitude") || lowerQuery.contains("peace") || lowerQuery.contains("joy") || lowerQuery.contains("content") || lowerQuery.contains("happy") || lowerQuery.contains("thankful")) {
            answerBuilder.append("Here are moments of quiet gratitude, joy, and peace captured in your thoughts:\n\n")
            val calmNotes = notes.filter { it.mood?.equals("Calm", true) == true || it.tags.any { t -> t.contains("gratitude") || t.contains("peace") } }.ifEmpty { notes }
            for (note in calmNotes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}**:\n  $preview\n\n")
            }
            answerBuilder.append("\n**Peaceful Reflection:** Your most profound contentment comes not from career milestones, but from unstructured stillness—warm coffee, amber morning light, and breathing beside someone you love.")
        }
        // 6. Blocker / Issue detection
        else if (lowerQuery.contains("blocker") || lowerQuery.contains("issue") || lowerQuery.contains("stuck") || lowerQuery.contains("problem") || lowerQuery.contains("bottleneck")) {
            answerBuilder.append("Based on your recent notes, here are the recurring blockers identified:\n\n")
            for (note in notes) {
                val blockerLines = note.content.lines().filter { line ->
                    val l = line.lowercase(Locale.ROOT)
                    l.contains("blocker") || l.contains("timeout") || l.contains("bottleneck") || l.contains("fail") || l.contains("leak") || l.contains("flak")
                }
                if (blockerLines.isNotEmpty()) {
                    val summaryItem = "${note.title}: ${blockerLines.first().trim().removePrefix("-").removePrefix("•").trim()}"
                    takeaways.add(summaryItem)
                    answerBuilder.append("• **${note.title}** (${note.formattedDate}):\n")
                    blockerLines.take(3).forEach { b ->
                        answerBuilder.append("  - ${b.trim().removePrefix("-").removePrefix("•").trim()}\n")
                    }
                } else {
                    val preview = note.summary ?: note.previewText
                    takeaways.add("${note.title}: $preview")
                    answerBuilder.append("• **${note.title}**: $preview\n")
                }
            }
        }
        // 7. Action items / Tasks query
        else if (lowerQuery.contains("task") || lowerQuery.contains("action") || lowerQuery.contains("todo") || lowerQuery.contains("pending") || lowerQuery.contains("checklist")) {
            answerBuilder.append("Here are your pending action items extracted across your journal entries:\n\n")
            for (note in notes) {
                if (note.actionItems.isNotEmpty()) {
                    note.actionItems.forEach { action ->
                        takeaways.add("${note.title}: $action")
                        answerBuilder.append("• [ ] $action *(from \"${note.title}\")*\n")
                    }
                }
            }
        }
        // 8. Creative / Ideas query
        else if (lowerQuery.contains("idea") || lowerQuery.contains("creative") || lowerQuery.contains("startup") || lowerQuery.contains("concept") || lowerQuery.contains("build")) {
            answerBuilder.append("Here are the creative concepts and brainstorms found in your Second Brain:\n\n")
            for (note in notes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}**:\n  $preview\n\n")
            }
        }
        // 9. Finance / Cost / Budget query
        else if (lowerQuery.contains("cost") || lowerQuery.contains("money") || lowerQuery.contains("budget") || lowerQuery.contains("finance") || lowerQuery.contains("spend")) {
            answerBuilder.append("Here is the financial & infrastructure cost summary from your journal:\n\n")
            for (note in notes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** (${note.formattedDate}):\n  $preview\n\n")
            }
        }
        // 10. Habits / Health / Routine query
        else if (lowerQuery.contains("health") || lowerQuery.contains("habit") || lowerQuery.contains("routine") || lowerQuery.contains("sleep") || lowerQuery.contains("focus")) {
            answerBuilder.append("Here are the health, routine, and focus patterns from your journal:\n\n")
            for (note in notes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** (${note.formattedDate}):\n  $preview\n\n")
            }
        }
        // 11. General synthesis
        else {
            answerBuilder.append("Here is what your Second Brain found regarding \"$query\":\n\n")
            for (note in notes.take(4)) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** (${note.formattedDate}):\n  $preview\n\n")
            }
            answerBuilder.append("\n**Synthesized Pattern:** You have recurring thoughts around ${notes.firstOrNull()?.tags?.joinToString(", ") ?: "these areas"}, showing active iteration and focus.")
        }

        val (patternAlert, suggestedReminder) = detectPatternAndReminder(lowerQuery, notes)

        SynthesisResult(
            answer = answerBuilder.toString().trim(),
            keyTakeaways = takeaways.take(6),
            patternAlert = patternAlert,
            suggestedReminder = suggestedReminder
        )
    }

    private fun detectPatternAndReminder(query: String, notes: List<Note>): Pair<String?, String?> {
        // Find the first genuine contextual reminder present in the retrieved notes
        for (note in notes) {
            val reminder = extractContextualReminder(note.title, note.content)
            if (reminder != null) {
                return Pair(reminder.triggerPattern, reminder.question)
            }
        }
        return Pair(null, null)
    }

    private fun extractActionItems(content: String): List<String> {
        val actions = mutableListOf<String>()
        val lines = content.lines()

        val imperativeVerbs = setOf(
            "call", "email", "send", "schedule", "book", "buy", "review",
            "write", "finish", "complete", "prepare", "submit", "update", "fix",
            "clean", "organize", "ask", "follow up", "draft", "check", "verify",
            "pay", "meet", "discuss", "set up", "install", "cancel", "ship",
            "deploy", "test", "investigate", "refactor", "plan", "share", "talk", "reach out"
        )

        var insideActionSection = false

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            val lower = line.lowercase(Locale.ROOT)

            // Header check (e.g., "Action items:", "Next steps:", "To-do:")
            if (lower.startsWith("### action") || lower.startsWith("## action") ||
                lower.startsWith("### next step") || lower.startsWith("## next step") ||
                lower.startsWith("### todo") || lower.startsWith("## todo") ||
                lower.startsWith("action items:") || lower.startsWith("next steps:") ||
                lower.startsWith("todo:") || lower.startsWith("tasks:")
            ) {
                insideActionSection = true
                continue
            } else if (line.startsWith("#")) {
                insideActionSection = false
            }

            // 1. Explicit Checkboxes: - [ ] Task, * [ ] Task, [ ] Task
            if (line.startsWith("- [ ]") || line.startsWith("* [ ]") || line.startsWith("- []")) {
                actions.add(line.substring(5).trim())
                continue
            } else if (line.startsWith("[ ]")) {
                actions.add(line.substring(3).trim())
                continue
            } else if (line.startsWith("- [x]") || line.startsWith("* [x]")) {
                continue
            }

            // 2. Explicit keywords: "todo:", "task:", "action:", "need to", "must", "have to", "remember to"
            val keywordPrefixes = listOf(
                "todo:", "task:", "action item:", "action:", "next step:", "follow up on:",
                "follow-up:", "i need to", "need to", "i must", "must", "i have to",
                "have to", "remember to", "don't forget to", "be sure to", "plan to"
            )
            var matchedKeyword = false
            for (p in keywordPrefixes) {
                if (lower.startsWith(p)) {
                    val cleaned = line.substring(p.length).trim().removePrefix(":").trim()
                    if (cleaned.isNotBlank()) {
                        actions.add(cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
                        matchedKeyword = true
                        break
                    }
                }
            }
            if (matchedKeyword) continue

            // 3. Bullet points or numbered lists: "- Call Derek", "* Send pitch deck", "1. Review metrics"
            val bulletPattern = Regex("^(?:[-*•]|\\d+[.)])\\s+(.+)$")
            val match = bulletPattern.find(line)
            if (match != null) {
                val bulletContent = match.groupValues[1].trim()
                val firstWord = bulletContent.split(" ").firstOrNull()?.lowercase(Locale.ROOT)?.trim() ?: ""

                if (insideActionSection || imperativeVerbs.contains(firstWord)) {
                    actions.add(bulletContent)
                    continue
                }
            }

            // 4. Standalone imperative sentences starting with action verbs (e.g. "Call investor tomorrow")
            val firstWord = line.split(" ").firstOrNull()?.lowercase(Locale.ROOT)?.trim() ?: ""
            if (imperativeVerbs.contains(firstWord) && line.length in 8..120 && !line.endsWith("?")) {
                actions.add(line)
            }
        }

        return actions.distinct().take(6)
    }

    private fun classifyMood(text: String): String {
        val lower = text.lowercase(Locale.ROOT)

        val moodScores = mutableMapOf(
            "Reflective" to 0,
            "Analytical" to 0,
            "Creative" to 0,
            "Energized" to 0,
            "Stressed" to 0,
            "Calm" to 0
        )

        // Weighted dictionary: token to (Mood, Weight)
        val weightedLexicon = listOf(
            // Stressed / Burnout (High priority detection)
            Pair(listOf("burnout", "burnt out", "exhaustion", "exhausted", "overwhelmed", "insomnia", "paralyzed", "panic attack"), Pair("Stressed", 5)),
            Pair(listOf("anxiety", "anxious", "dread", "pressure", "headache", "tension", "strain", "doubt", "rejection", "imposter"), Pair("Stressed", 3)),
            Pair(listOf("frustrated", "deadline", "stuck", "failing"), Pair("Stressed", 2)),

            // Reflective / Vulnerable / Relationships / Life
            Pair(listOf("elena", "relationship", "partner", "marriage", "vulnerable", "vulnerability", "loneliness", "lonely", "invisible", "grief", "aging parents"), Pair("Reflective", 5)),
            Pair(listOf("love", "argument", "crying", "tears", "heart", "dad", "father", "mom", "grandpa", "legacy", "memory", "memories", "soul", "presence"), Pair("Reflective", 4)),
            Pair(listOf("ponder", "realize", "realization", "perspective", "journal", "identity", "meaning", "honesty", "growth"), Pair("Reflective", 2)),

            // Calm / Peace / Gratitude
            Pair(listOf("stillness", "serene", "unfiltered gratitude", "quiet morning", "contentment"), Pair("Calm", 5)),
            Pair(listOf("peace", "peaceful", "gratitude", "grateful", "quiet", "breathe", "breathing", "amber morning", "tea", "unstructured"), Pair("Calm", 4)),
            Pair(listOf("relax", "balanced", "content", "steady", "gentle"), Pair("Calm", 2)),

            // Energized / High Momentum / Sports
            Pair(listOf("tempo run", "marathon", "crushed it", "milestone reached", "momentum"), Pair("Energized", 5)),
            Pair(listOf("workout", "running", "shipped", "launched", "win", "excited", "pumped", "inspired"), Pair("Energized", 3)),
            Pair(listOf("ready", "fast", "active"), Pair("Energized", 2)),

            // Creative / Ideation
            Pair(listOf("brainstorm", "ideation", "concept design", "wearable", "prototype"), Pair("Creative", 4)),
            Pair(listOf("idea", "ideas", "create", "vision", "imagine", "sketch", "album", "music", "craft"), Pair("Creative", 3)),
            Pair(listOf("innovate", "invent", "dream"), Pair("Creative", 2)),

            // Analytical / System / Engineering (Only when truly technical)
            Pair(listOf("post-mortem", "cascade failure", "database migration", "profiling", "latency bottleneck"), Pair("Analytical", 4)),
            Pair(listOf("architecture", "database", "sql", "benchmark", "vector search", "throughput", "infrastructure", "cost audit", "metrics"), Pair("Analytical", 3)),
            Pair(listOf("system", "optimize", "evaluate", "api", "query", "logic"), Pair("Analytical", 2))
        )

        for ((phrases, moodWeight) in weightedLexicon) {
            val (mood, weight) = moodWeight
            for (phrase in phrases) {
                if (phrase.contains(" ")) {
                    if (lower.contains(phrase)) {
                        moodScores[mood] = (moodScores[mood] ?: 0) + weight
                    }
                } else {
                    val regex = Regex("\\b${Regex.escape(phrase)}\\b")
                    if (regex.containsMatchIn(lower)) {
                        moodScores[mood] = (moodScores[mood] ?: 0) + weight
                    }
                }
            }
        }

        val topMood = moodScores.maxByOrNull { it.value }
        return if (topMood != null && topMood.value > 0) topMood.key else "Reflective"
    }

    private fun extractTags(title: String, content: String): List<String> {
        val tags = mutableSetOf<String>()
        val combined = "$title $content".lowercase(Locale.ROOT)

        // Direct hashtag extraction (#android, #ai, #wellness)
        val hashtagRegex = Regex("#([a-zA-Z0-9_]+)")
        hashtagRegex.findAll("$title $content").forEach { match ->
            tags.add(match.groupValues[1].lowercase(Locale.ROOT))
        }

        // Expanded domain taxonomy
        val domainKeywords = mapOf(
            "relationship" to "Relationship",
            "elena" to "Love",
            "love" to "Love",
            "vulnerability" to "Vulnerability",
            "burnout" to "Burnout",
            "exhaust" to "Wellness",
            "anxiety" to "MentalHealth",
            "lonel" to "Connection",
            "family" to "Family",
            "parents" to "Family",
            "dad" to "Family",
            "mom" to "Family",
            "grandpa" to "Legacy",
            "grief" to "Grief",
            "gratitude" to "Gratitude",
            "peace" to "Mindfulness",
            "startup" to "Startup",
            "investor" to "Startup",
            "marathon" to "Fitness",
            "running" to "Fitness",
            "architecture" to "Architecture",
            "engineering" to "Engineering",
            "android" to "Android",
            "ai" to "AI",
            "database" to "Database",
            "productivity" to "Productivity",
            "deep work" to "DeepWork",
            "career" to "Career",
            "design" to "Design",
            "ideas" to "Ideas",
            "coffee" to "Coffee"
        )

        for ((word, tag) in domainKeywords) {
            val matches = if (word.length <= 4) {
                Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(combined)
            } else {
                combined.contains(word)
            }
            if (matches) {
                tags.add(tag)
            }
        }

        if (tags.isEmpty()) {
            tags.add("Journal")
            tags.add("Reflection")
        }

        return tags.take(5).toList()
    }

    private fun generateSummary(title: String, content: String): String {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return title

        val sentences = trimmedContent.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length > 15 && !it.startsWith("#") }

        if (sentences.isEmpty()) return title

        // Look for resolution / realization / conclusion sentence
        val realizationSentence = sentences.lastOrNull { s ->
            val l = s.lowercase(Locale.ROOT)
            l.contains("realiz") || l.contains("learn") || l.contains("decid") || l.contains("need to") ||
                    l.contains("next step") || l.contains("ultimately") || l.contains("important") ||
                    l.contains("grateful") || l.contains("appreciat") || l.contains("restore")
        }

        // Look for core dilemma / conflict / friction sentence
        val conflictSentence = sentences.firstOrNull { s ->
            val l = s.lowercase(Locale.ROOT)
            l.contains("struggl") || l.contains("burnout") || l.contains("exhaust") || l.contains("fail") ||
                    l.contains("worried") || l.contains("hard") || l.contains("difficult") || l.contains("blocker") ||
                    l.contains("fear") || l.contains("anxious") || l.contains("overwhelm")
        }

        val synthesized = when {
            conflictSentence != null && realizationSentence != null && conflictSentence != realizationSentence -> {
                "Reflecting on ${conflictSentence.take(65).trim().removeSuffix(".")}: finding clarity in ${realizationSentence.take(65).trim().removeSuffix(".")}."
            }
            realizationSentence != null -> {
                "Key realization: ${realizationSentence.removeSuffix(".")}."
            }
            sentences.size >= 2 -> {
                val corePoint = sentences.first().removeSuffix(".")
                val outcome = sentences.last().removeSuffix(".")
                "Reflecting on $corePoint — resolving to $outcome."
            }
            else -> {
                val first = sentences.first()
                "Thought synthesis: ${first.removeSuffix(".")}."
            }
        }

        return if (synthesized.length > 150) synthesized.take(147) + "..." else synthesized
    }

    fun extractContextualReminder(title: String, content: String): ContextualReminder? {
        val classification = LifeSphereClassifier.classify(title, content)
        val fullText = "$title\n$content"
        val lowerText = fullText.lowercase(Locale.ROOT)

        when (classification.primarySphere) {
            LifeSphere.RELATIONAL -> {
                val entity = classification.detectedEntities.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Elena"
                if (entity.equals("Elena", ignoreCase = true) || lowerText.contains("elena")) {
                    return ContextualReminder(
                        triggerPattern = "Relational Presence: Quality Time with Elena",
                        question = "You reflected on feeling pulled away from Elena: Do you want a reminder this evening at 7:30 PM to step away from screens and connect with undivided presence?",
                        suggestedDelayMinutes = 120
                    )
                }
                val familyMembers = listOf("Mom", "Dad", "Mother", "Father", "Parents", "Grandpa")
                val matchedFamily = familyMembers.find { entity.equals(it, ignoreCase = true) || lowerText.contains(it.lowercase()) }
                if (matchedFamily != null) {
                    return ContextualReminder(
                        triggerPattern = "Family Connection: Check in with $matchedFamily",
                        question = "You reflected on your connection with $matchedFamily: Do you want a reminder tomorrow afternoon to call them and share an unhurried conversation?",
                        suggestedDelayMinutes = 180
                    )
                }
                return ContextualReminder(
                    triggerPattern = "Relational Connection: Presence with $entity",
                    question = "You noted tension or emotional longing in your relationship with $entity: Do you want a reminder this evening to pause work and reconnect with intentional presence?",
                    suggestedDelayMinutes = 120
                )
            }

            LifeSphere.PHYSICAL_WELLBEING -> {
                val isScreen = lowerText.contains("eye") || lowerText.contains("screen") || lowerText.contains("glare")
                if (isScreen) {
                    return ContextualReminder(
                        triggerPattern = "Sensory Overload: Screen & Eye Fatigue",
                        question = "You observed eye burn and heavy screen time: Do you want to set a reminder in 45 minutes for a 10-minute screen-free rest and distant focal break?",
                        suggestedDelayMinutes = 45
                    )
                }
                val isSleep = lowerText.contains("late night") || lowerText.contains("2 am") || lowerText.contains("1 am") || lowerText.contains("insomnia")
                if (isSleep) {
                    return ContextualReminder(
                        triggerPattern = "Sleep Protection & Late-Night Rest",
                        question = "You noted exhaustion from working past midnight: Do you want to set a reminder at 10:15 PM tonight to shut down your laptop and protect your rest?",
                        suggestedDelayMinutes = 180
                    )
                }
                val symptom = when {
                    lowerText.contains("neck") -> "neck stiffness"
                    lowerText.contains("back") -> "lower back strain"
                    lowerText.contains("shoulder") -> "shoulder tightness"
                    else -> "physical fatigue"
                }
                return ContextualReminder(
                    triggerPattern = "Physical Strain: $symptom",
                    question = "You noted $symptom: Do you want to set a reminder at 4:30 PM to stand up, do 5 minutes of spine mobility stretches, and reset your posture?",
                    suggestedDelayMinutes = 90
                )
            }

            LifeSphere.WORK_ENGINEERING -> {
                return ContextualReminder(
                    triggerPattern = "Engineering Focus & Boundaries",
                    question = "You're deep in complex technical execution: Do you want a reminder at 6:00 PM to commit current progress, write down your next step, and close work tabs?",
                    suggestedDelayMinutes = 120
                )
            }

            LifeSphere.GROWTH_REFLECTION -> {
                return ContextualReminder(
                    triggerPattern = "Decision Mindset & Courage",
                    question = "You're wrestling with perfectionism and fear of the wrong choice: Do you want a reminder tomorrow morning at 9:00 AM to review your decision with fresh eyes and trust your preparation?",
                    suggestedDelayMinutes = 180
                )
            }

            LifeSphere.GENERAL -> {
                if (lowerText.contains("late night") || lowerText.contains("2 am") || lowerText.contains("1 am") || lowerText.contains("midnight")) {
                    return ContextualReminder(
                        triggerPattern = "Sleep Protection & Late-Night Rest",
                        question = "You noted exhaustion from working past midnight: Do you want to set a reminder at 10:15 PM tonight to shut down your laptop and protect your rest?",
                        suggestedDelayMinutes = 180
                    )
                }
                return null
            }
        }
    }

    fun generateReflectionPrompts(title: String, content: String, mood: String): List<String> {
        val classification = LifeSphereClassifier.classify(title, content)
        val prompts = mutableListOf<String>()

        // 1. CHECK FOR EVOLUTION STAGE (Has the user already answered a previous reflection?)
        val hasPreviousReflection = content.contains("> **Reflection") || content.contains("**Insight:**") || content.contains("*My Reflection:*")
        if (hasPreviousReflection) {
            val latestInsight = content.lines()
                .filter { it.contains("**Insight:**") || it.contains("*My Reflection:*") }
                .lastOrNull()
                ?.replace(Regex(""".*(\*\*Insight:\*\*|\*My Reflection:\*)"""), "")
                ?.trim()

            if (!latestInsight.isNullOrBlank()) {
                val insightSnippet = if (latestInsight.length > 50) latestInsight.take(47) + "..." else latestInsight
                prompts.add("You reflected: \"$insightSnippet\". What is one tangible, 15-minute experiment you can run in the next 24 hours to test this?")
                prompts.add("If someone you deeply respect evaluated this realization, what blind spot would they gently challenge?")
                prompts.add("What will be the hardest obstacle to remaining consistent with this insight when unexpected stress hits?")
                return prompts
            }
        }

        // 2. DOMAIN-SILOED SOCRATIC INQUIRY (Firewalled by LifeSphere)
        when (classification.primarySphere) {
            LifeSphere.RELATIONAL -> {
                val entity = classification.detectedEntities.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Elena"
                prompts.add("You mentioned $entity and feeling pulled away: what unspoken boundary between work anxiety and evening presence can you establish today?")
                prompts.add("If $entity could describe the version of you they feel most deeply connected to, what would they say is different about how you show up?")
                prompts.add("What is one quiet, intentional gesture tonight that honors this realization and shows them they have your undivided attention?")
            }

            LifeSphere.GROWTH_REFLECTION -> {
                prompts.add("What single part of this situation is entirely within your direct control today, and what expectation must you let go of?")
                prompts.add("If you remove the pressure of perfectionism, what does 'trusting your preparation' look like in practice right now?")
                prompts.add("What would you tell a close friend or colleague who came to you with this exact fear of making the wrong choice?")
            }

            LifeSphere.WORK_ENGINEERING -> {
                prompts.add("What is the single highest-risk assumption in this technical path, and how can you verify it with a minimal test before committing?")
                prompts.add("If you completely stepped back from this bottleneck for 24 hours, what simpler architectural solution might become visible?")
                prompts.add("What rollback or fallback plan gives you the psychological safety to move forward decisively today?")
            }

            LifeSphere.PHYSICAL_WELLBEING -> {
                prompts.add("Your body is signaling fatigue here: what boundary or ritual can protect your physical recovery before burnout sets in?")
                prompts.add("What subtle warning signs did you overlook before reaching this level of strain, and how can you catch them sooner?")
                prompts.add("What is one non-negotiable change to your evening routine that guarantees restful recovery tonight?")
            }

            LifeSphere.GENERAL -> {
                val titleTopic = title.ifBlank { "this reflection" }.removePrefix("Untitled Entry").trim()
                prompts.add("What is the most honest truth about \"$titleTopic\" that you haven't explicitly typed out yet?")
                prompts.add("What will this look like one year from now if you act decisively on this insight today?")
                prompts.add("What is one concrete, low-friction action step that honors what you felt while writing this?")
            }
        }

        return prompts.distinct().take(3)
    }

    /**
     * Polishes raw OCR text extracted from Camera snapshots (books, whiteboards, handwritten notes)
     * into clean structured markdown with an inferred title.
     */
    fun polishOcrText(rawText: String): Pair<String, String> {
        val cleanLines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (cleanLines.isEmpty()) {
            return Pair("Scanned Note", "No legible text found in scanned image.")
        }

        // Infer title from first line
        val titleCandidate = cleanLines.first().take(60)
            .replace(Regex("^[^a-zA-Z0-9]+"), "")
            .trim()
        val title = if (titleCandidate.length > 3) titleCandidate else "Scanned Note Insights"

        val bodyLines = if (cleanLines.size > 1) cleanLines.drop(1) else cleanLines
        val contentBuilder = StringBuilder()
        contentBuilder.append("### Scanned & Polished Thought\n\n")

        for (line in bodyLines) {
            if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
                contentBuilder.append("$line\n")
            } else if (line.length > 60) {
                contentBuilder.append("$line\n\n")
            } else {
                contentBuilder.append("- $line\n")
            }
        }

        return Pair(title, contentBuilder.toString().trim())
    }
}
