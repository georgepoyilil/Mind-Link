package com.mindlink.journal.ui.navigation

sealed class Screen(val route: String) {
    object NotesList : Screen("notes_list")
    object NoteEditor : Screen("note_editor?noteId={noteId}&mode={mode}") {
        fun createRoute(noteId: Long? = null, mode: String? = null): String {
            val idParam = if (noteId != null && noteId > 0) "noteId=$noteId" else "noteId=0"
            val modeParam = if (mode != null) "&mode=$mode" else "&mode=text"
            return "note_editor?$idParam$modeParam"
        }
    }
    object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: Long): String = "note_detail/$noteId"
    }
    object SecondBrain : Screen("second_brain")
    object ThoughtGraph : Screen("thought_graph")
    object Settings : Screen("settings")
}
