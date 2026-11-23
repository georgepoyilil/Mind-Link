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
        if (lowerQuery.contains("love") || lowerQuery.contains("relationship") || lowerQuery.contains("partner") || lowerQuery.contains("intima") || lowerQuery.contains("argument") || lowerQuery.contains("date night") || lowerQuery.contains("dating")) {
            answerBuilder.append("Based on your personal entries regarding love and relationships:\n\n")
            val loveNotes = notes.filter { n ->
                n.tags.any { it == "love" || it.contains("relationship") || it.contains("dating") || it.contains("presence") || it.contains("partner") } ||
                        n.title.lowercase(Locale.ROOT).contains("love") ||
                        n.title.lowercase(Locale.ROOT).contains("relationship")
            }.ifEmpty { notes }

            for (note in loveNotes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** (${note.formattedDate}):\n  $preview\n\n")
            }
            val keyInsight = loveNotes.firstOrNull()?.summary ?: loveNotes.firstOrNull()?.previewText ?: "deep presence and honest communication."
            answerBuilder.append("\n**Core Reflection:** Across your relational entries, your reflections emphasize: $keyInsight")
        }
        // 2. Emotional Vulnerability, Loneliness, Burnout & Mental Health
        else if (lowerQuery.contains("feel") || lowerQuery.contains("emotion") || lowerQuery.contains("vulnerab") || lowerQuery.contains("lonel") || lowerQuery.contains("burnout") || lowerQuery.contains("exhaust") || lowerQuery.contains("mental") || lowerQuery.contains("anxious") || lowerQuery.contains("anxiety") || lowerQuery.contains("fear")) {
            answerBuilder.append("Here is an honest synthesis of your recent emotional patterns, vulnerabilities, and mental bandwidth:\n\n")
            for (note in notes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** [Mood: ${note.mood ?: "Reflective"}]:\n  $preview\n\n")
            }
            val emotionalTakeaway = notes.firstOrNull()?.summary ?: notes.firstOrNull()?.previewText ?: "acknowledging feelings with self-compassion and setting clear boundaries."
            answerBuilder.append("\n**Emotional Takeaway:** Your notes highlight genuine emotional awareness: $emotionalTakeaway")
        }
        // 3. Failure, Rejection & Overcoming Setbacks
        else if (lowerQuery.contains("fail") || lowerQuery.contains("reject") || lowerQuery.contains("imposter") || lowerQuery.contains("setback") || lowerQuery.contains("pass") || lowerQuery.contains("regret")) {
            answerBuilder.append("Here is how you've reflected on failure, setbacks, and resilience across your journal:\n\n")
            for (note in notes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** (${note.formattedDate}):\n  $preview\n\n")
            }
            val resiliencePoint = notes.firstOrNull()?.summary ?: notes.firstOrNull()?.previewText ?: "reframing obstacles into valuable iteration and learning."
            answerBuilder.append("\n**Key Realization:** Across these entries, you consistently work to reframe setbacks into practical learning: $resiliencePoint")
        }
        // 4. Family, Parents, Aging & Grief
        else if (lowerQuery.contains("family") || lowerQuery.contains("parent") || lowerQuery.contains("dad") || lowerQuery.contains("mom") || lowerQuery.contains("grief") || lowerQuery.contains("loss") || lowerQuery.contains("aging")) {
            answerBuilder.append("Here is what your Second Brain holds regarding your family, memories, and loved ones:\n\n")
            val famNotes = notes.filter { n ->
                n.tags.any { it.contains("family") || it.contains("parents") || it.contains("dad") || it.contains("mom") || it.contains("grief") }
            }.ifEmpty { notes }

            for (note in famNotes) {
                val preview = note.summary ?: note.previewText
                takeaways.add("${note.title}: $preview")
                answerBuilder.append("• **${note.title}** (${note.formattedDate}):\n  $preview\n\n")
            }
            val familyInsight = famNotes.firstOrNull()?.summary ?: famNotes.firstOrNull()?.previewText ?: "cherishing memories and prioritizing connection with family."
            answerBuilder.append("\n**Heartfelt Insight:** Your family reflections emphasize staying connected and honoring shared moments: $familyInsight")
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
            val peaceInsight = calmNotes.firstOrNull()?.summary ?: calmNotes.firstOrNull()?.previewText ?: "finding deep contentment in everyday stillness and mindful moments."
            answerBuilder.append("\n**Peaceful Reflection:** Your thoughts highlight finding contentment in mindful moments: $peaceInsight")
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

        val (patternAlert, suggestedReminder) = detectPatternAndReminder(notes)

        SynthesisResult(
            answer = answerBuilder.toString().trim(),
            keyTakeaways = takeaways.take(6),
            patternAlert = patternAlert,
            suggestedReminder = suggestedReminder
        )
    }

    private fun detectPatternAndReminder(notes: List<Note>): Pair<String?, String?> {
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
                val entity = classification.detectedEntities.firstOrNull()?.replaceFirstChar { it.uppercase() }
                if (entity == null) {
                    return null
                }

                // Only suggest a relational presence reminder if there is genuine tension, distraction, conflict or desire to reconnect
                val hasTensionOrDisconnect = listOf(
                    "argument", "fight", "tension", "pulled away", "distract", "phone", "screen",
                    "overwork", "guilt", "neglect", "miss", "distant", "cold", "unspoken", "conflict",
                    "disagree", "frustrat", "busy", "hurried", "tired", "reconnect"
                ).any { lowerText.contains(it) }

                if (!hasTensionOrDisconnect) {
                    return null
                }

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
                val seed = (insightSnippet.hashCode() xor mood.hashCode()).let { if (it < 0) -it else it }
                val followUpPool = listOf(
                    "You reflected: \"$insightSnippet\". What is one tangible, 15-minute experiment you can run in the next 24 hours to test this?",
                    "Regarding your insight (\"$insightSnippet\"): what belief will be hardest to maintain when unexpected friction arrives?",
                    "You noted: \"$insightSnippet\". If someone you deeply respect evaluated this realization, what blind spot would they gently challenge?",
                    "Following up on \"$insightSnippet\": what is the exact trigger or environment that might cause you to revert to old habits?"
                )
                prompts.add(followUpPool[seed % followUpPool.size])
                prompts.add(followUpPool[(seed + 1) % followUpPool.size])
                prompts.add(followUpPool[(seed + 2) % followUpPool.size])
                return prompts.distinct().take(3)
            }
        }

        // 2. EXTRACT SALIENT DYNAMIC CONCEPTS FROM NOTE
        val salientTerms = extractSalientConcepts(title, content)
        val primaryConcept = salientTerms.firstOrNull() ?: title.ifBlank { "this realization" }.removePrefix("Untitled Entry").trim()
        val secondaryConcept = salientTerms.getOrNull(1) ?: primaryConcept
        val noteSeed = (title.hashCode() xor (content.take(80).hashCode()) xor mood.hashCode()).let { if (it < 0) -it else it }

        // 3. DYNAMIC SOCRATIC QUESTION 1: Core Dilemma & Sphere-Specific Tension
        when (classification.primarySphere) {
            LifeSphere.RELATIONAL -> {
                val entity = classification.detectedEntities.firstOrNull()?.replaceFirstChar { it.uppercase() }
                    ?: (if (primaryConcept.isNotBlank() && primaryConcept[0].isUpperCase()) primaryConcept else "them")
                val q1Pool = listOf(
                    "You mentioned $entity: what unspoken boundary between external demands and genuine presence can you establish today?",
                    "When communicating with $entity around $primaryConcept, what emotion is hiding behind the words you actually chose?",
                    "If $entity could describe the version of you they feel most deeply understood by, what would change about how you show up?"
                )
                prompts.add(q1Pool[noteSeed % q1Pool.size])
            }
            LifeSphere.GROWTH_REFLECTION -> {
                val q1Pool = listOf(
                    "What part of \"$primaryConcept\" is entirely within your direct agency today, and what expectation must you let go of?",
                    "If you remove the pressure to prove anything, what does trusting your preparation look like regarding \"$primaryConcept\"?",
                    "What fear or resistance is currently masquerading as 'need for more time or information' about $primaryConcept?"
                )
                prompts.add(q1Pool[noteSeed % q1Pool.size])
            }
            LifeSphere.WORK_ENGINEERING -> {
                val q1Pool = listOf(
                    "What is the single highest-risk assumption in \"$primaryConcept\", and how can you verify it with a minimal test before building further?",
                    "If you stepped back from this technical bottleneck for 24 hours, what simpler architectural trade-off might become visible?",
                    "What rollback or fallback plan around $primaryConcept gives you the psychological safety to ship decisively today?"
                )
                prompts.add(q1Pool[noteSeed % q1Pool.size])
            }
            LifeSphere.PHYSICAL_WELLBEING -> {
                val q1Pool = listOf(
                    "Your reflections around \"$primaryConcept\" signal bodily fatigue: what non-negotiable ritual can protect your recovery tonight?",
                    "What subtle strain signs did you overlook before reaching this point with $primaryConcept, and how can you detect them earlier?",
                    "If you treated your energy as a fixed physical budget, which low-value drain related to $primaryConcept would you cut immediately?"
                )
                prompts.add(q1Pool[noteSeed % q1Pool.size])
            }
            LifeSphere.GENERAL -> {
                val q1Pool = listOf(
                    "What is the most honest truth about \"$primaryConcept\" that you felt while typing this, but haven't explicitly articulated?",
                    "If you viewed \"$primaryConcept\" from an outsider's lens, what obvious strength or opportunity are you overlooking?",
                    "What fundamental assumption about \"$primaryConcept\" would surprise you most if it turned out to be completely false?"
                )
                prompts.add(q1Pool[noteSeed % q1Pool.size])
            }
        }

        // 4. DYNAMIC SOCRATIC QUESTION 2: Perspective Shift & Emotional Valence (Always dynamic, never static)
        val normalizedMood = mood.lowercase().trim()
        val q2Pool = when {
            normalizedMood.contains("stress") || normalizedMood.contains("anxious") || normalizedMood.contains("overwhelm") -> listOf(
                "Given the tension around '$secondaryConcept', what is one non-essential demand you can give yourself permission to drop today?",
                "If you treated this anxiety as information rather than a crisis, what boundary is your mind asking for regarding '$secondaryConcept'?",
                "What is the simplest subtraction you can make in your schedule to create breathing room around $secondaryConcept?",
                "What would you tell a trusted friend who came to you carrying this exact worry about $secondaryConcept?"
            )
            normalizedMood.contains("creative") || normalizedMood.contains("energ") || normalizedMood.contains("excit") -> listOf(
                "Where does '$secondaryConcept' intersect with an unconventional idea or passion you've kept on the back burner?",
                "What bold, low-friction experiment around '$secondaryConcept' could leverage the momentum you feel right now?",
                "If you had total freedom to pursue '$secondaryConcept' without risk of judgment, what would the first prototype look like?",
                "How can you capture and codify the energy behind '$secondaryConcept' so it sustains you on slower days?"
            )
            normalizedMood.contains("analytical") || normalizedMood.contains("focus") -> listOf(
                "If an external expert audited your approach to '$secondaryConcept', which hidden inefficiency would they flag first?",
                "What would an 80/20 simplification of '$secondaryConcept' look like if you were forced to deliver in under an hour?",
                "What counter-intuitive metric will prove whether your trajectory on '$secondaryConcept' was truly the right one?",
                "What second-order consequences of this decision on '$secondaryConcept' might emerge three months from now?"
            )
            else -> listOf(
                "Looking at '$secondaryConcept', what old habit or mental model are you quietly outgrowing through this experience?",
                "If future-you from five years ahead could comment on '$secondaryConcept', what perspective would they offer?",
                "What unspoken value or standard of yours is being tested most strongly by '$secondaryConcept' right now?",
                "What part of this thought around '$secondaryConcept' feels most vulnerable or challenging to fully accept?"
            )
        }
        prompts.add(q2Pool[(noteSeed + 1) % q2Pool.size])

        // 5. DYNAMIC SOCRATIC QUESTION 3: Actionable Micro-Experiment / Future-Proofing (Always dynamic)
        val q3Pool = listOf(
            "What is a single 10-minute micro-action within the next 24 hours that moves \"$primaryConcept\" from contemplation into tangible progress?",
            "What is the quietest obstacle or friction point that might derail you tomorrow, and what is your plan to bypass it?",
            "Who is one person whose perspective or feedback on \"$primaryConcept\" could save you weeks of second-guessing?",
            "How will your daily routine look one month from now if you remain consistent with the insight behind \"$primaryConcept\"?",
            "What is the single most decisive 'no' you must say this week to protect your focus on \"$primaryConcept\"?"
        )
        prompts.add(q3Pool[(noteSeed + 2) % q3Pool.size])

        return prompts.distinct().take(3)
    }

    /**
     * Extracts top 2-3 salient noun/concept phrases from title and content to anchor dynamic Socratic prompts.
     */
    private fun extractSalientConcepts(title: String, content: String): List<String> {
        val stopWords = setOf(
            "the", "and", "a", "an", "in", "on", "at", "to", "for", "with", "from", "by", "about",
            "this", "that", "these", "those", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "can", "could", "will", "would", "should",
            "my", "your", "his", "her", "its", "our", "their", "not", "but", "what", "which",
            "who", "when", "where", "why", "how", "all", "each", "every", "both", "some", "any",
            "just", "like", "very", "also", "then", "into", "over", "more", "most", "other",
            "note", "entry", "journal", "today", "yesterday", "thought", "untitled"
        )

        val candidateWords = mutableListOf<String>()

        // 1. Title concepts first (high signal)
        title.split(Regex("[\\s,.:;!?()\\[\\]\"'#*~_/-]+"))
            .filter { it.length >= 3 && it.lowercase() !in stopWords }
            .forEach { candidateWords.add(it) }

        // 2. Content concepts
        content.split(Regex("[\\s,.:;!?()\\[\\]\"'#*~_/-]+"))
            .filter { it.length >= 4 && it.lowercase() !in stopWords }
            .take(30)
            .forEach { candidateWords.add(it) }

        val frequencyMap = candidateWords.groupingBy { it.lowercase() }.eachCount()
        return frequencyMap.entries
            .sortedByDescending { it.value }
            .map { it.key.replaceFirstChar { ch -> ch.uppercase() } }
            .take(3)
    }

    /**
     * Polishes raw OCR text extracted from Camera snapshots, book pages, or screenshots:
     * - Strips status bar clutter (timestamps, battery, network indicators: LTE, 5G, Wi-Fi)
     * - Removes app navigation chrome (Back, Cancel, Done, Save, Settings, etc.)
     * - Filters camera watermarks (Shot on ..., AI Camera, etc.)
     * - Removes page numbers and margin noise / OCR garbage lines
     * - Re-joins hyphenated line wraps (e.g. "connec-\ntion" -> "connection")
     * - Re-flows broken lines into coherent paragraphs and clean bullet points
     * - Extracts a meaningful title from document headings
     */
    fun polishOcrText(rawText: String): Pair<String, String> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return Pair("Scanned Note", "No legible text found in image.")
        }

        // Noise filtering regexes
        val timeRegex = Regex("""^(?:\d{1,2}:\d{2}(?::\d{2})?\s*(?:AM|PM|am|pm)?|\d{1,2}\s*[.:]\s*\d{2})$""")
        val batteryStatusRegex = Regex("""^(?:\d{1,3}%|LTE|5G|4G|VoLTE|VOLTE|Wi-?Fi|SIM\s*\d?|Roaming)$""", RegexOption.IGNORE_CASE)
        val combinedStatusRegex = Regex("""^(?:\d{1,2}:\d{2}\s*)?(?:5G|4G|LTE|Wi-?Fi)?\s*(?:\|\s*)?\d{1,3}%$""", RegexOption.IGNORE_CASE)
        val uiNavRegex = Regex("""^(?:<|>|Back|< Back|Cancel|Done|Save|Edit|Share|Settings|Search|Options|Menu|Close|Next|Skip)$""", RegexOption.IGNORE_CASE)
        val watermarkRegex = Regex("""^.*(?:Shot on|AI Camera|Dual Camera|Triple Camera|Quad Camera|Watermark|Nothing Phone|Redmi Note|Galaxy S\d+|iPhone).*$""", RegexOption.IGNORE_CASE)
        val pageNumRegex = Regex("""^(?:Page\s+\d+(?:\s+of\s+\d+)?|-?\s*\d+\s*-?|\d+\s*/\s*\d+)$""", RegexOption.IGNORE_CASE)
        val copyrightRegex = Regex("""^(?:©|Copyright|All rights reserved|ISBN\b).*$""", RegexOption.IGNORE_CASE)

        val filteredLines = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length <= 1 && !trimmed[0].isLetterOrDigit()) continue // solitary punctuation
            if (timeRegex.matches(trimmed)) continue
            if (batteryStatusRegex.matches(trimmed)) continue
            if (combinedStatusRegex.matches(trimmed)) continue
            if (uiNavRegex.matches(trimmed)) continue
            if (pageNumRegex.matches(trimmed)) continue
            if (copyrightRegex.matches(trimmed)) continue
            if (watermarkRegex.matches(trimmed) && trimmed.length < 50) continue

            // Filter lines with virtually no alphanumeric characters (sensor/margin noise)
            val alnumCount = trimmed.count { it.isLetterOrDigit() }
            if (alnumCount == 0 && trimmed.length > 1) continue
            if (trimmed.length > 4 && alnumCount.toDouble() / trimmed.length < 0.35) continue

            filteredLines.add(trimmed)
        }

        if (filteredLines.isEmpty()) {
            return Pair("Scanned Note", "No legible text found in image.")
        }

        // Infer title from first line if it looks like a heading
        val firstLine = filteredLines.first()
        val isFirstLineTitleCandidate = firstLine.length in 3..70 &&
                !firstLine.endsWith(".") &&
                !firstLine.startsWith("-") &&
                !firstLine.startsWith("•") &&
                !firstLine.startsWith("*")

        val titleCandidate = if (isFirstLineTitleCandidate) {
            firstLine.replace(Regex("""^[^a-zA-Z0-9]+"""), "").trim()
        } else {
            "Scanned Document"
        }
        val title = if (titleCandidate.length in 3..70) titleCandidate else "Scanned Document"

        val bodyLines = if (isFirstLineTitleCandidate && filteredLines.size > 1) {
            filteredLines.drop(1)
        } else {
            filteredLines
        }

        // Re-assemble broken sentences and paragraphs
        val paragraphBuilder = StringBuilder()
        var currentParagraph = StringBuilder()

        for (i in bodyLines.indices) {
            val line = bodyLines[i]
            val isBullet = line.startsWith("-") || line.startsWith("•") || line.startsWith("*") ||
                    Regex("""^\d+[\.)]\s""").containsMatchIn(line)
            val isHeader = line.startsWith("#")

            if (isBullet || isHeader) {
                if (currentParagraph.isNotEmpty()) {
                    paragraphBuilder.append(currentParagraph.toString().trim()).append("\n\n")
                    currentParagraph.clear()
                }
                // Normalize bullet symbol to markdown "- "
                val normalizedBullet = if (line.startsWith("• ") || line.startsWith("* ")) {
                    "- " + line.substring(2)
                } else if (line.startsWith("•") || line.startsWith("*")) {
                    "- " + line.substring(1).trim()
                } else {
                    line
                }
                paragraphBuilder.append(normalizedBullet).append("\n")
            } else {
                if (currentParagraph.isEmpty()) {
                    currentParagraph.append(line)
                } else {
                    // Check if previous line ended with a hyphen (broken word)
                    val prevText = currentParagraph.toString()
                    if (prevText.endsWith("-") && line.firstOrNull()?.isLetter() == true) {
                        currentParagraph.setLength(currentParagraph.length - 1) // drop '-'
                        currentParagraph.append(line)
                    } else if (prevText.endsWith(".") || prevText.endsWith("!") || prevText.endsWith("?") || prevText.endsWith(":")) {
                        // Previous line ended a sentence; start new paragraph
                        paragraphBuilder.append(currentParagraph.toString().trim()).append("\n\n")
                        currentParagraph.clear()
                        currentParagraph.append(line)
                    } else {
                        // Mid-sentence wrap, join with space
                        currentParagraph.append(" ").append(line)
                    }
                }
            }
        }

        if (currentParagraph.isNotEmpty()) {
            paragraphBuilder.append(currentParagraph.toString().trim())
        }

        val cleanBody = paragraphBuilder.toString().trim()
        val finalBody = if (cleanBody.isNotBlank()) cleanBody else "Scanned text contained only formatted markers."

        return Pair(title, finalBody)
    }
}
