package com.mindlink.journal.ui.secondbrain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.domain.model.Note
import com.mindlink.journal.domain.model.SearchResult
import com.mindlink.journal.domain.model.SynthesisResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class DynamicInquiry(
    val display: String,
    val query: String,
    val category: String = "Insight"
)

data class SecondBrainUiState(
    val query: String = "",
    val isSynthesizing: Boolean = false,
    val isSearching: Boolean = false,
    val synthesisResponse: SynthesisResponse? = null,
    val hybridResults: List<SearchResult> = emptyList(),
    val dynamicInquiries: List<DynamicInquiry> = emptyList(),
    val errorMessage: String? = null
)

class SecondBrainViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecondBrainUiState())
    val uiState: StateFlow<SecondBrainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllNotes().collect { allNotes ->
                val activeNotes = allNotes.filter { !it.isDeleted }
                val inquiries = generateDynamicInquiries(activeNotes)
                _uiState.update { it.copy(dynamicInquiries = inquiries) }
            }
        }
    }

    private fun generateDynamicInquiries(notes: List<Note>): List<DynamicInquiry> {
        if (notes.isEmpty()) {
            return listOf(
                DynamicInquiry("How am I feeling overall?", "How am I feeling overall based on my thoughts?", "Mindset"),
                DynamicInquiry("Recurring blockers", "What recurring blockers or bottlenecks have I noted?", "Blockers"),
                DynamicInquiry("Moments of peace & joy", "What moments of peace, calm, and gratitude have I noted?", "Wellbeing"),
                DynamicInquiry("Open action items", "What action items or commitments have I noted?", "Action")
            )
        }

        val inquiries = mutableListOf<DynamicInquiry>()
        val corpus = notes.joinToString(" ") { "${it.title} ${it.content} ${it.tags.joinToString(" ")}" }.lowercase(Locale.ROOT)
        val tagFrequency = notes.flatMap { it.tags }
            .filter { it.isNotBlank() }
            .groupingBy { it.lowercase(Locale.ROOT) }
            .eachCount()

        // 1. Relational Context (Elena, Family, Loved Ones)
        if (corpus.contains("elena") || tagFrequency.containsKey("love") || tagFrequency.containsKey("relationship")) {
            inquiries.add(
                DynamicInquiry(
                    display = "Relationship with Elena",
                    query = "How has my dynamic and feelings with Elena evolved across my journal thoughts?",
                    category = "Relational"
                )
            )
        }

        if (corpus.contains("mom") || corpus.contains("dad") || corpus.contains("parent") || tagFrequency.containsKey("family")) {
            inquiries.add(
                DynamicInquiry(
                    display = "Reflections on Family",
                    query = "What heartfelt reflections and memories regarding my family and parents have I written down?",
                    category = "Roots"
                )
            )
        }

        // 2. Technical, Architecture & Career
        if (corpus.contains("architecture") || corpus.contains("code") || corpus.contains("vulkan") || corpus.contains("shader") || tagFrequency.containsKey("work")) {
            inquiries.add(
                DynamicInquiry(
                    display = "Architectural & Code Decisions",
                    query = "What technical trade-offs, architecture decisions, and code bottlenecks have I documented?",
                    category = "Technical"
                )
            )
        }

        // 3. Emotional Bandwidth, Burnout & Rest
        if (corpus.contains("burnout") || corpus.contains("exhaust") || corpus.contains("fatigue") || corpus.contains("screen") || tagFrequency.containsKey("health")) {
            inquiries.add(
                DynamicInquiry(
                    display = "Burnout Triggers & Recovery",
                    query = "What patterns trigger my work exhaustion and what habits help me recover clarity?",
                    category = "Wellbeing"
                )
            )
        }

        // 4. Resilience, Rejections & Overcoming Setbacks
        if (corpus.contains("fail") || corpus.contains("reject") || corpus.contains("pass") || corpus.contains("sequoia") || corpus.contains("setback")) {
            inquiries.add(
                DynamicInquiry(
                    display = "Handling Rejections & Failures",
                    query = "What setbacks, passes, or failures have I reflected on, and how did I reframe them into growth?",
                    category = "Resilience"
                )
            )
        }

        // 5. Open Action Items & Commitments
        val hasActionItems = notes.any { it.actionItems.isNotEmpty() }
        if (hasActionItems) {
            inquiries.add(
                DynamicInquiry(
                    display = "Pending Action Items",
                    query = "What concrete action items and commitments are still open across my journal?",
                    category = "Execution"
                )
            )
        }

        // 6. Ambitions & Creative Ideas
        if (corpus.contains("idea") || corpus.contains("vision") || tagFrequency.containsKey("ideas") || tagFrequency.containsKey("goals")) {
            inquiries.add(
                DynamicInquiry(
                    display = "Core Ideas & Aspirations",
                    query = "What are the most promising ideas, concepts, and goals I've brainstormed?",
                    category = "Vision"
                )
            )
        }

        // 7. Perspective Shifts & Growth over Time
        if (notes.size >= 3) {
            inquiries.add(
                DynamicInquiry(
                    display = "Shifts in My Thinking",
                    query = "How have my priorities and mental outlook shifted from my earlier entries to my latest notes?",
                    category = "Growth"
                )
            )
        }

        // Ensure minimum 4 inquiries
        if (inquiries.size < 4) {
            inquiries.add(DynamicInquiry("How am I feeling overall?", "How am I feeling overall based on my thoughts?", "Mindset"))
            inquiries.add(DynamicInquiry("Moments of peace & calm", "What brought me peace and deep focus?", "Wellbeing"))
        }

        return inquiries.distinctBy { it.display }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun submitQuery(userQuery: String? = null) {
        val targetQuery = userQuery ?: _uiState.value.query
        if (targetQuery.isBlank()) return

        _uiState.update {
            it.copy(
                query = targetQuery,
                isSynthesizing = true,
                isSearching = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                // 1. Run Hybrid Search (FTS + Vector cosine similarity)
                val searchResults = repository.searchNotesHybrid(targetQuery)

                // 2. Run Conversational Synthesis
                val synthesis = repository.synthesizeSecondBrain(targetQuery)

                _uiState.update {
                    it.copy(
                        isSynthesizing = false,
                        isSearching = false,
                        synthesisResponse = synthesis,
                        hybridResults = searchResults
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSynthesizing = false,
                        isSearching = false,
                        errorMessage = e.message ?: "Failed to query Second Brain"
                    )
                }
            }
        }
    }

    class Factory(private val repository: NoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SecondBrainViewModel(repository) as T
        }
    }
}
