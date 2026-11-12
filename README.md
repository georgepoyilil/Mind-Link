# MindLink — Intelligent Android Journal & Second Brain 🧠⚡

**MindLink** is a native Android journaling and associative memory system built with Jetpack Compose, Material 3, Room SQLite, WorkManager, and on-device AI. 

MindLink pairs instant, distraction-free journaling (including streaming speech-to-text input) with an autonomous **AI Processing Pipeline** and an **Associative Second Brain Memory System** that discovers hidden connections between your past and present thoughts.

---

## 🏛️ Architecture Overview

```
                                      ┌────────────────────────────────┐
                                      │   Jetpack Compose UI Layer     │
                                      │  (Notes, Editor, Detail, Q&A)  │
                                      └──────────────┬─────────────────┘
                                                     │
                                                     ▼
                                      ┌────────────────────────────────┐
                                      │        NoteRepository          │
                                      └──────┬──────────────────┬──────┘
                                             │                  │
                         Instant Save (Sync) │                  │ Trigger Async Worker
                                             ▼                  ▼
                                     ┌──────────────┐   ┌───────────────────────────────┐
                                     │ Room SQLite  │   │  WorkManager Background Task  │
                                     │  (Database)  │   │     (NoteProcessingWorker)    │
                                     └──────────────┘   └───────────────┬───────────────┘
                                                                        │
                                            ┌───────────────────────────┴───────────────────────────┐
                                            ▼                                                       ▼
                             ┌─────────────────────────────┐                         ┌─────────────────────────────┐
                             │    Brainstorming Engine     │                         │      Embedding Engine       │
                             │ (Gemini Flash / Local NLP)  │                         │ (ONNX Runtime all-MiniLM)   │
                             ├─────────────────────────────┤                         ├─────────────────────────────┤
                             │ • 1-Sentence TL;DR Summary  │                         │ • 384-dimensional dense     │
                             │ • Dominant Mood Extraction  │                         │   vector representation     │
                             │ • Action Items & Tasks      │                         │ • Stored in note_embeddings │
                             │ • Socratic Prompts          │                         │   table for cosine lookup   │
                             └─────────────────────────────┘                         └─────────────────────────────┘
```

---

## 🚀 Key Features

### 1. Journaling & Audio Input
* **Instant Persistence:** Notes are committed to SQLite immediately, never blocking the UI while AI processing runs.
* **Streaming Speech-to-Text:** Live voice journaling powered by Android's native `SpeechRecognizer` with an animated pulse indicator.
* **Markdown Formatting Toolbar:** Quick markdown helpers for bold, headers, task checkboxes (`- [ ]`), and bullet points.

### 2. Autonomous Background AI Pipeline (`WorkManager`)
When a note is created or edited, `NoteProcessingWorker` runs asynchronously in the background:
* **Metadata Extraction:** Produces a 1-sentence TL;DR summary, classifies the dominant mood (Reflective, Analytical, Creative, Energized, Stressed, Calm), and auto-tags entries.
* **Action Item Detection:** Parses checklist items and imperative tasks into interactive checkable to-dos.
* **Reflection Prompts:** Generates 2–3 follow-up reflection questions and counter-arguments to push thinking deeper.
* **Vector Indexing:** Generates a 384-dimensional dense vector embedding and saves it to local storage.

### 3. Associative Memory & Second Brain
* **Contextual "Related Thoughts" Card:** Whenever you view a note, MindLink performs an on-device cosine similarity calculation across past embeddings to surface the top 2–3 related historical thoughts with match percentages.
* **Hybrid Search (Vector + FTS):** Combines SQLite Full-Text Search (FTS) with 384-d dense vector semantic search using Reciprocal Rank Fusion (RRF).
* **Conversational Querying & Synthesis:** Ask questions like *"What recurring blockers have I noted recently?"* or *"Summarize my creative ideas"* and receive a cross-referenced AI synthesis citing source notes.

---

## 🛠️ Tech Stack & Dependencies

| Layer | Technology |
|---|---|
| **Language & UI** | Kotlin 1.9.23, Jetpack Compose, Material 3, Compose Navigation |
| **Local Persistence** | AndroidX Room 2.6.1 (SQLite) with FTS4/5 shadow tables & TypeConverters |
| **Background Processing** | AndroidX WorkManager 2.9.0 (`CoroutineWorker`) |
| **Embeddings & Vector Search** | `com.microsoft.onnxruntime:onnxruntime-android:1.17.0` (`all-MiniLM-L6-v2.onnx`) + VectorSearchEngine (Cosine Similarity & RRF) |
| **Generative Brainstorming** | Hybrid Composite Engine: Google Gemini 1.5 Flash (JSON Schema mode) with automatic fallback to local heuristic NLP |
| **Audio Input** | Native Android `SpeechRecognizer` + Audio Permission Handling |

---

## 📂 Project Structure

```
G:\Projects\memory-android\
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   └── (Optional: place all-MiniLM-L6-v2.onnx here)
│       │   ├── java/com/mindlink/journal/
│       │   │   ├── MindLinkApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── MindLinkDatabase.kt
│       │   │   │   │   ├── entity/ (NoteEntity, NoteFtsEntity, NoteEmbeddingEntity)
│       │   │   │   │   ├── dao/ (NoteDao, NoteFtsDao, EmbeddingDao)
│       │   │   │   │   └── converters/ (Converters)
│       │   │   │   ├── ai/
│       │   │   │   │   ├── BrainstormingEngine.kt
│       │   │   │   │   ├── GeminiBrainstormingEngine.kt
│       │   │   │   │   ├── LocalHeuristicBrainstormingEngine.kt
│       │   │   │   │   ├── CompositeBrainstormingEngine.kt
│       │   │   │   │   ├── EmbeddingEngine.kt
│       │   │   │   │   ├── OnnxEmbeddingEngine.kt
│       │   │   │   │   ├── FallbackEmbeddingEngine.kt
│       │   │   │   │   └── VectorSearchEngine.kt
│       │   │   │   ├── repository/ (NoteRepository, NoteRepositoryImpl)
│       │   │   │   └── worker/ (NoteProcessingWorker)
│       │   │   ├── domain/model/ (Note, RelatedThought, SearchResult, SynthesisResponse)
│       │   │   ├── di/ (ServiceLocator)
│       │   │   └── ui/
│       │   │       ├── theme/ (Color, Theme, Type)
│       │   │       ├── navigation/ (Screen, MindLinkNavHost)
│       │   │       ├── notes/ (NotesListScreen, NotesViewModel, NoteCard)
│       │   │       ├── editor/ (NoteEditorScreen, NoteEditorViewModel, SpeechRecognitionHelper)
│       │   │       ├── detail/ (NoteDetailScreen, NoteDetailViewModel, RelatedThoughtsCard, ReflectionPromptsCard)
│       │   │       ├── secondbrain/ (SecondBrainScreen, SecondBrainViewModel, ConversationalSynthesisCard, HybridSearchSection)
│       │   │       └── settings/ (SettingsScreen, SettingsViewModel)
│       └── test/java/com/mindlink/journal/
│           ├── VectorSearchEngineTest.kt
│           ├── LocalHeuristicBrainstormingEngineTest.kt
│           └── FallbackEmbeddingEngineTest.kt
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🧩 On-Device Embedding Setup (`all-MiniLM-L6-v2.onnx`)

MindLink comes equipped with a **Dual-Mode Embedding Pipeline**:
1. **Out of the box:** `FallbackEmbeddingEngine` uses deterministic dimensional projection to generate normalized 384-dimensional unit vectors with zero external downloads required.
2. **Full ONNX Runtime acceleration:** To enable full BERT-based transformer embeddings:
   - Download the quantized `all-MiniLM-L6-v2.onnx` model (e.g. from HuggingFace).
   - Place the file in `app/src/main/assets/all-MiniLM-L6-v2.onnx`.
   - `OnnxEmbeddingEngine` will automatically detect the asset on startup, load the ONNX session, and compute transformer token embeddings and mean pooling on-device!

---

## 🔑 Google Gemini Flash Configuration

1. Launch MindLink and tap the **⚙️ Settings** icon in the top right.
2. Enter your **Google Gemini API Key** (from Google AI Studio).
3. The app will immediately activate Gemini Flash for structured multi-note syntheses and reflection prompts.
4. If you leave this field blank or use the app offline, MindLink automatically uses its built-in on-device NLP heuristic engine!

---

## 🧪 Running Unit Tests

Run the test suite with:
```bash
./gradlew test
```
The included test suite verifies:
* Cosine similarity calculations and edge cases (identical vs orthogonal vectors).
* Vector nearest-neighbor ranking and Reciprocal Rank Fusion (RRF).
* Action item parsing and mood classification lexicons.
* 384-dimensional vector generation and normalization.
