package com.mindlink.journal.data.local

import com.mindlink.journal.data.repository.NoteRepository
import com.mindlink.journal.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

object SampleDataSeeder {

    val sampleNotes = listOf(
        // 1. Engineering / Architecture
        Note(
            title = "Sprint Retrospective: Recurring API Bottlenecks",
            content = """
                Sprint Retrospective notes for sprint 42.
                
                ### Recurring Blockers
                - Backend API endpoints are timing out during peak traffic hours (2 PM - 5 PM).
                - Foreign key lookups on the transactions table are unindexed in PostgreSQL.
                - Token refresh cycle causes UI micro-stutters on mobile clients.
                
                ### Action Items
                - [ ] Implement exponential backoff and jitter in mobile sync worker
                - [ ] Meet with DevOps regarding Redis connection pool exhaustion
                - [ ] Add database index on `account_id` in transactions migration
                
                Overall team velocity took a 20% hit this week due to these recurring infrastructure blockers.
            """.trimIndent(),
            mood = "Stressed",
            tags = listOf("blocker", "backend", "performance", "sprint"),
            actionItems = listOf(
                "Implement exponential backoff and jitter in mobile sync worker",
                "Meet with DevOps regarding Redis connection pool exhaustion",
                "Add database index on account_id in transactions migration"
            ),
            summary = "Sprint velocity dropped 20% due to recurring backend database timeouts and unindexed foreign keys.",
            reflectionPrompts = listOf(
                "What is the single highest-impact index change that can stabilize peak hour latency?",
                "Can mobile clients gracefully degrade to offline-cached data during API degradation?"
            ),
            isProcessed = true
        ),

        // 2. Technical Architecture
        Note(
            title = "Evaluating Vector Database Options for Mobile Apps",
            content = """
                Deep dive into on-device semantic search and mobile vector indexes.
                
                ### Options Evaluated
                1. **sqlite-vec**: Fast C extension for SQLite, but requires custom NDK builds and Android 14+ packaging quirks.
                2. **ObjectBox Vector Search**: Feature-rich and fast HNSW implementation, but adds ~8MB native binary overhead.
                3. **Room + Raw FloatArray Cosine Search**: Minimal APK overhead, fully offline, sub-millisecond lookup for <5,000 notes on modern ARM64 chips.
                
                ### Tasks
                - [ ] Benchmark cosine similarity latency on 5,000 vectors on a physical device
                - [ ] Profile memory footprint of 384-dimensional 32-bit floats vs int8 quantization
                - [ ] Validate battery drain during continuous WorkManager batch indexing
            """.trimIndent(),
            mood = "Analytical",
            tags = listOf("architecture", "android", "ai", "vectors"),
            actionItems = listOf(
                "Benchmark cosine similarity latency on 5,000 vectors on a physical device",
                "Profile memory footprint of 384-dimensional 32-bit floats vs int8 quantization",
                "Validate battery drain during continuous WorkManager batch indexing"
            ),
            summary = "Evaluating mobile vector search trade-offs between sqlite-vec, ObjectBox, and lightweight in-memory cosine ranking.",
            reflectionPrompts = listOf(
                "At what note volume does in-memory cosine search become a CPU bottleneck on mid-range devices?",
                "Could int8 quantization reduce vector storage by 75% without hurting semantic retrieval accuracy?"
            ),
            isProcessed = true
        ),

        // 3. Creative / Startup
        Note(
            title = "Brainstorm: Ambient Second Brain Audio Wearable",
            content = """
                Concept notes for a privacy-first ambient memory companion.
                
                ### The Core Idea
                A lightweight magnetic lapel pin that passively buffers audio in memory. When the user taps it twice or says "MindLink, capture", it transcribes the last 60 seconds of conversation, extracts actionable commitments, and links them into their Second Brain.
                
                ### Hardware & Architecture
                - Dual MEMS beamforming microphone array.
                - Ultra-low power Bluetooth Low Energy (BLE 5.3) wake-on-voice DSP.
                - Local whisper.tflite / ONNX inference on the phone to prevent cloud eavesdropping.
                
                ### Next Steps
                - [ ] Sketch 3D hardware enclosure in CAD
                - [ ] Research Nordic Semiconductor nRF5340 dual-core BLE SoC
                - [ ] Interview 5 remote workers about daily memory capture friction
            """.trimIndent(),
            mood = "Creative",
            tags = listOf("ideas", "hardware", "ai", "wearable"),
            actionItems = listOf(
                "Sketch 3D hardware enclosure in CAD",
                "Research Nordic Semiconductor nRF5340 dual-core BLE SoC",
                "Interview 5 remote workers about daily memory capture friction"
            ),
            summary = "Concept for a privacy-first ambient audio lapel pin that transcribes spoken thoughts and links them into Second Brain.",
            reflectionPrompts = listOf(
                "What is the simplest software-only prototype you could validate on a smartwatch first?",
                "How can you make social consent and privacy indicator lights 100% transparent to bystanders?"
            ),
            isProcessed = true
        ),

        // 4. Personal Habits / Health
        Note(
            title = "Deep Work Rituals & Evening Digital Sunset",
            content = """
                Reflections on cognitive stamina, sleep hygiene, and focus blocks.
                
                I noticed a stark correlation this week: days where I scrolled social media after 9:30 PM resulted in a 40-minute delay in sleep onset and fragmented REM sleep. Next morning, writing complex algorithms felt like wading through mud.
                
                ### Behavioral Changes
                - **Digital Sunset at 9:00 PM**: Phone goes onto the living room charging dock. Only paper books and Kindle allowed in bedroom.
                - **Morning Focus Fortress**: 8:00 AM to 11:30 AM reserved strictly for coding and system design with notifications muted.
                
                ### Action Items
                - [ ] Configure Android Digital Wellbeing Bedtime mode to grayscale screen at 9:00 PM
                - [ ] Move charging cable out of the bedroom tonight
                - [ ] Track subjective morning energy levels in MindLink daily
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("health", "habits", "productivity", "focus"),
            actionItems = listOf(
                "Configure Android Digital Wellbeing Bedtime mode to grayscale screen at 9:00 PM",
                "Move charging cable out of the bedroom tonight",
                "Track subjective morning energy levels in MindLink daily"
            ),
            summary = "Establishing a 9:00 PM digital sunset to protect deep REM sleep and protect morning 3-hour focus blocks.",
            reflectionPrompts = listOf(
                "What friction can you introduce to make touching your phone after 9 PM feel unnatural?",
                "How will you handle urgent late-night alerts without compromising sleep hygiene?"
            ),
            isProcessed = true
        ),

        // 5. Work / Management
        Note(
            title = "1-on-1 Sync with Sarah - Tech Lead Feedback",
            content = """
                Quarterly alignment conversation with Sarah.
                
                ### Key Discussion Points
                - Sarah is doing stellar work leading the Android Compose migration, but she's experiencing blocker fatigue on our flaky CI/CD pipeline.
                - Tests fail intermittently due to emulator boot timeouts on the GitHub Actions Linux runner.
                - Career aspirations: She wants to transition into Staff Engineer by focusing on platform architecture and developer velocity.
                
                ### Follow-up Commitments
                - [ ] Dedicate 2 engineering days to eliminate CI/CD emulator test flakiness
                - [ ] Nominate Sarah for Tech Lead on the upcoming Offline Sync rewrite
                - [ ] Send Sarah the Staff Engineer archetype rubric before Friday
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("meeting", "team", "management", "leadership"),
            actionItems = listOf(
                "Dedicate 2 engineering days to eliminate CI/CD emulator test flakiness",
                "Nominate Sarah for Tech Lead on the upcoming Offline Sync rewrite",
                "Send Sarah the Staff Engineer archetype rubric before Friday"
            ),
            summary = "Sarah is blocked by CI/CD emulator test flakiness; aligning her Q3 goals toward Staff Engineer leadership.",
            reflectionPrompts = listOf(
                "How can you empower Sarah to lead architectural decisions rather than just executing them?",
                "What developer tooling improvements will yield the highest morale boost for the whole team?"
            ),
            isProcessed = true
        ),

        // 6. Finance / Cloud Costs
        Note(
            title = "Quarterly Cloud Infrastructure Cost Audit",
            content = """
                Reviewing our AWS and Google Cloud monthly billing statement.
                
                ### Cost Inefficiencies Discovered
                - Cloud bills spiked 22% ($1,850 increase) compared to last quarter.
                - Root cause: 3 large GPU EC2 instances (`g5.2xlarge`) used for embedding model fine-tuning were left running idle over two consecutive weekends.
                - Unattached EBS gp3 storage volumes were abandoned after previous load tests.
                
                ### Remediation Actions
                - [ ] Set up AWS Lambda cron job to shut down dev/test GPU instances at 7:00 PM daily
                - [ ] Delete 14 orphan EBS volumes currently generating $180/mo in dead waste
                - [ ] Configure AWS Budget Slack webhook alerts at 80% and 100% threshold
            """.trimIndent(),
            mood = "Analytical",
            tags = listOf("finance", "cloud", "devops", "cost"),
            actionItems = listOf(
                "Set up AWS Lambda cron job to shut down dev/test GPU instances at 7:00 PM daily",
                "Delete 14 orphan EBS volumes currently generating $180/mo in dead waste",
                "Configure AWS Budget Slack webhook alerts at 80% and 100% threshold"
            ),
            summary = "Cloud expenses rose 22% due to idle weekend GPU training nodes and unattached EBS storage volumes.",
            reflectionPrompts = listOf(
                "What automated guardrails can prevent developers from leaving high-cost GPU machines running?",
                "Can on-demand GPU training be migrated to spot instances with 60% discount?"
            ),
            isProcessed = true
        ),

        // 7. Books / Systems Thinking
        Note(
            title = "Notes on 'Thinking in Systems' by Donella Meadows",
            content = """
                Key principles and systems thinking models from Donella Meadows' masterwork.
                
                ### Core Insights
                1. **Stocks and Flows**: You cannot fix a backlog (stock) by solely increasing intake (inflow) without addressing throughput and outflow bottlenecks.
                2. **Delays cause oscillations**: When feedback loops are delayed, systems inevitably overshoot and crash. (Matches our software release cycles!)
                3. **Leverage Points**: The highest leverage is not in tweaking numbers, but in changing the *rules*, *goals*, and *paradigm* of the system.
                
                ### Practical Application to Our Team
                - Shorten PR review feedback loops from 48 hours to under 4 hours to eliminate cognitive context switching.
                - [ ] Create a 1-page systems thinking checklist for sprint post-mortems
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("learning", "books", "philosophy", "systems"),
            actionItems = listOf(
                "Create a 1-page systems thinking checklist for sprint post-mortems"
            ),
            summary = "Systems traps cannot be solved by tweaking parameters; feedback loops and information flow delays govern performance.",
            reflectionPrompts = listOf(
                "Where in your current software workflow is a hidden feedback delay causing team oscillation?",
                "What is the single highest leverage point in your daily routine?"
            ),
            isProcessed = true
        ),

        // 8. Fitness / Running
        Note(
            title = "Half-Marathon Tempo Run 14km",
            content = """
                Morning training session in the park.
                
                - Distance: 14.2 km
                - Average Pace: 4:48 /km
                - Average Heart Rate: 156 bpm (Zone 3/4 threshold)
                
                Felt energetic and dialed in throughout. Mile 9-11 felt smooth with zero hip tightness, likely thanks to the 10-minute dynamic warm-up and foam rolling routine started on Tuesday.
                
                ### Follow-up
                - [ ] Replace running shoes (current pair has reached 650 km)
                - [ ] Hydrate with 2L water + electrolytes before tomorrow's recovery walk
            """.trimIndent(),
            mood = "Energized",
            tags = listOf("fitness", "running", "health", "energy"),
            actionItems = listOf(
                "Replace running shoes (current pair has reached 650 km)",
                "Hydrate with 2L water + electrolytes before tomorrow's recovery walk"
            ),
            summary = "Strong 14km tempo run at 4:48/km pace with excellent zone 3 heart rate stability and zero hip tightness.",
            reflectionPrompts = listOf(
                "How can you carry this physical momentum into your afternoon engineering focus blocks?",
                "Are you balancing high-intensity threshold workouts with sufficient low-heart-rate Zone 2 base building?"
            ),
            isProcessed = true
        ),

        // 9. Quick Micro Note / Quote
        Note(
            title = "Seneca quote on anxiety",
            content = """
                "We suffer more often in imagination than in reality." — Seneca, Letters from a Stoic.
                
                Need to remember this whenever anticipation of a difficult stakeholder conversation starts creating phantom stress. 90% of imagined catastrophes never materialize.
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("quote", "stoicism", "mindset"),
            actionItems = emptyList(),
            summary = "Seneca reminder: Imagined anxiety is almost always worse than real-world outcomes.",
            reflectionPrompts = listOf("What specific worry are you currently exaggerating in your mind?"),
            isProcessed = true
        ),

        // 10. Small Everyday / Grocery List
        Note(
            title = "Farmers Market & Saturday Errands",
            content = """
                Weekend grocery list:
                - [ ] Artisan sourdough loaf from bread stall
                - [ ] Fresh basil, rosemary, and heirloom tomatoes
                - [ ] Barista oat milk (2 cartons)
                - [ ] Ethiopian washed coffee beans (medium-light roast)
                - [ ] Greek yogurt and raw honey
                - [ ] Pick up dry cleaning on 5th Ave
            """.trimIndent(),
            mood = "Calm",
            tags = listOf("personal", "grocery", "errands", "weekend"),
            actionItems = listOf(
                "Artisan sourdough loaf from bread stall",
                "Fresh basil, rosemary, and heirloom tomatoes",
                "Barista oat milk (2 cartons)",
                "Ethiopian washed coffee beans (medium-light roast)",
                "Greek yogurt and raw honey",
                "Pick up dry cleaning on 5th Ave"
            ),
            summary = "Weekend shopping checklist for farmers market produce, coffee beans, and dry cleaning.",
            reflectionPrompts = emptyList(),
            isProcessed = true
        ),

        // 11. Coffee Dialing Recipe Log
        Note(
            title = "Dialing in V60: Ethiopia Yirgacheffe Natural",
            content = """
                Recipe log for morning pour-over:
                - Dose: 18.0g coffee (Comandante grinder at 23 clicks)
                - Water: 300g at 93°C (1:16.6 ratio)
                - Bloom: 50g water, 45 seconds gentle swirl
                - Pour: 2 concentric circular pours up to 180g, then slow center pour to 300g
                - Drawdown completed at 2:48
                
                **Cup Profile**: Intensely floral aroma, jasmine, juicy bergamot, sweet peach acidity that rounds into milk chocolate. Very clean cup with zero bitterness.
            """.trimIndent(),
            mood = "Creative",
            tags = listOf("coffee", "v60", "recipe", "craft"),
            actionItems = listOf(
                "Try 22 clicks on grinder tomorrow to see if peach sweetness intensifies"
            ),
            summary = "V60 recipe for Ethiopian Yirgacheffe: 18g/300g at 93°C producing crisp jasmine and peach notes.",
            reflectionPrompts = listOf("What other morning sensory rituals bring you into an immediate state of flow?"),
            isProcessed = true
        ),

        // 12. Late-Night Fleeting Insight
        Note(
            title = "Late night realization at 1:45 AM",
            content = """
                Why do the clearest architectural breakthroughs always strike right as I turn off the bedside lamp?
                
                Just solved the caching dilemma: instead of invalidating the entire vector index on every minor note edit, keep a lightweight dirty-flag ring buffer in memory and flush to SQLite only during Android idle windows.
                
                Jotting this down now so my subconscious stops looping. Time to sleep.
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("thoughts", "insomnia", "architecture", "ideas"),
            actionItems = listOf(
                "Test dirty-flag ring buffer idea in scratch module tomorrow"
            ),
            summary = "Late night architecture insight on optimizing vector index cache updates with a dirty ring buffer.",
            reflectionPrompts = listOf("How can you capture spontaneous night thoughts without disrupting circadian sleep rhythms?"),
            isProcessed = true
        ),

        // 13. Health & Ergonomics Symptom Log
        Note(
            title = "Tension Headache & Neck Strain Log",
            content = """
                Persistent dull throbbing at the base of the neck since 3:30 PM today.
                
                Causes identified:
                - Reviewed 6 complex PRs today while slouching forward over the laptop.
                - Forgot to wear blue light glasses during the 2-hour backend debugging session.
                - Hydration was poor (only 1 glass of water before noon).
                
                Immediate remedies:
                - [ ] Take a 15-minute screen-free walk around the block
                - [ ] Raise external monitor arm by 2.5 inches to eye level
                - [ ] Drink 500ml water with magnesium powder
            """.trimIndent(),
            mood = "Stressed",
            tags = listOf("health", "ergonomics", "body", "symptoms"),
            actionItems = listOf(
                "Take a 15-minute screen-free walk around the block",
                "Raise external monitor arm by 2.5 inches to eye level",
                "Drink 500ml water with magnesium powder"
            ),
            summary = "Neck strain and tension headache caused by poor laptop posture during an extended code review session.",
            reflectionPrompts = listOf("What hourly physical cue or timer can you establish to prevent posture slouching?"),
            isProcessed = true
        ),

        // 14. Personal Gift Brainstorming
        Note(
            title = "Mom's 60th Birthday Surprise Brainstorm",
            content = """
                Brainstorming meaningful gift ideas for Mom's milestone birthday next month.
                
                She explicitly said "no more dust-collecting physical objects", so focusing purely on experiences and nostalgia:
                1. **Digital Memory Capsule**: Reach out to her university roommates and cousins to record 60-second video memories, compiled into a private Vimeo link.
                2. **Weekend Botanical Garden Retreat**: Book the historic bed & breakfast near the glasshouse gardens she visited 30 years ago with Dad.
                3. **Framed Watercolor Commission**: Commission local artist to paint her beloved cottage vegetable garden.
                
                Tasks:
                - [ ] WhatsApp Aunt Clara for old photo albums and phone numbers
                - [ ] Check B&B room availability for the weekend of the 18th
                - [ ] Order custom card and gift wrapping
            """.trimIndent(),
            mood = "Creative",
            tags = listOf("family", "gifts", "personal", "celebration"),
            actionItems = listOf(
                "WhatsApp Aunt Clara for old photo albums and phone numbers",
                "Check B&B room availability for the weekend of the 18th",
                "Order custom card and gift wrapping"
            ),
            summary = "Brainstorming experiential gifts for Mom's 60th birthday: digital video memories, a garden retreat, or custom art.",
            reflectionPrompts = listOf("How can you make this celebration feel deeply personal rather than just transactional?"),
            isProcessed = true
        ),

        // 15. Career Reflection / Imposter Syndrome
        Note(
            title = "Imposter Syndrome in the Staff Engineer Sync",
            content = """
                Honest reflection on today's senior architecture forum.
                
                Found myself hesitating to speak up during the distributed messaging discussion because everyone else in the room held Staff or Principal titles from tier-1 tech giants. Felt that familiar sinking feeling: *'What if they realize I don't know the exact internals of Raft consensus?'*
                
                Reframing:
                - My domain intuition around mobile memory constraints and client resilience is deeply valuable.
                - Seniority is about framing the right questions and identifying system trade-offs, not reciting textbook trivia.
                - Feeling out of depth is the only environment where genuine intellectual growth happens.
                
                Action:
                - [ ] Write a 1-page design RFC on our offline sync state machine by Wednesday
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("career", "mindset", "imposter-syndrome", "growth"),
            actionItems = listOf(
                "Write a 1-page design RFC on our offline sync state machine by Wednesday"
            ),
            summary = "Overcoming self-doubt during a senior technical forum by reframing vulnerability into a catalyst for growth.",
            reflectionPrompts = listOf(
                "What unique perspective do you bring to this team that nobody else has?",
                "What would you build if you were completely unconcerned with being judged?"
            ),
            isProcessed = true
        ),

        // 16. Outdoor Weekend Adventure Plan
        Note(
            title = "Ridge Trail Hike & Camping Gear Plan",
            content = """
                Preparing for the 18km mountain ridge hike with David and Maya this Sunday.
                
                Trail details:
                - Distance: 18.4 km loop
                - Elevation gain: +820 meters
                - Estimated duration: 5.5 hours
                
                Gear Checklist:
                - [ ] Merrell waterproof trail runners (break them in with a short walk on Friday)
                - [ ] 3L hydration bladder with electrolyte powder
                - [ ] High-energy rations: dried figs, almonds, 3 peanut butter squeeze packs
                - [ ] Lightweight emergency bivvy and whistle
                - [ ] Download offline GPS topo maps on Gaia GPS app
            """.trimIndent(),
            mood = "Energized",
            tags = listOf("hiking", "outdoors", "weekend", "adventure"),
            actionItems = listOf(
                "Merrell waterproof trail runners (break them in with a short walk on Friday)",
                "3L hydration bladder with electrolyte powder",
                "High-energy rations: dried figs, almonds, 3 peanut butter squeeze packs",
                "Lightweight emergency bivvy and whistle",
                "Download offline GPS topo maps on Gaia GPS app"
            ),
            summary = "Gear checklist and route plan for Sunday's 18km mountain ridge hike with David and Maya.",
            reflectionPrompts = listOf("How does time spent in nature reset your baseline mental bandwidth for the coming week?"),
            isProcessed = true
        ),

        // 17. Personal Housing / Lease Dilemma
        Note(
            title = "Apartment Decision: Renew Lease vs Relocate",
            content = """
                Current apartment lease ends in 50 days. Landlord proposed an $85/month rent increase.
                
                **Option A: Stay in current 1-bedroom ($1,785/mo)**
                - Pros: Zero moving expense, 10-minute walk to subway, familiar coffee shops.
                - Cons: Kitchen is cramped, living room doubles as office with no physical separation.
                
                **Option B: Move to 2-bedroom on Elm St ($2,050/mo)**
                - Pros: Dedicated home office room with south-facing natural light, in-unit laundry, dishwasher!
                - Cons: $265/mo extra cost, moving weekend logistics.
                
                Decision matrix: The dedicated office would dramatically improve focus and work-life boundaries.
                
                - [ ] Schedule viewing for the Elm St apartment this Thursday at 6:30 PM
                - [ ] Calculate total moving budget (deposit + movers + cleaning)
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("personal", "housing", "finance", "decisions"),
            actionItems = listOf(
                "Schedule viewing for the Elm St apartment this Thursday at 6:30 PM",
                "Calculate total moving budget (deposit + movers + cleaning)"
            ),
            summary = "Weighing whether to absorb an $85 rent increase or upgrade to a 2-bedroom with a dedicated office on Elm St.",
            reflectionPrompts = listOf("Does a dedicated physical boundary between work and sleep justify a 15% increase in rent?"),
            isProcessed = true
        ),

        // 18. Cooking & Family Memory
        Note(
            title = "Grandma's 4-Hour Braised Beef Ragu Recipe",
            content = """
                Preserving Nana's handwritten pasta sauce recipe before the paper fades completely.
                
                ### Ingredients
                - 1.2 kg beef chuck or bone-in short ribs
                - 2 medium yellow onions, 3 carrots, 3 celery stalks (finely diced soffritto)
                - 4 cloves smashed garlic
                - 2 tablespoons tomato paste
                - 1 glass dry Italian red wine (Chianti works best)
                - 2 cans San Marzano whole peeled tomatoes, crushed by hand
                - 2 sprigs fresh rosemary, 3 bay leaves
                
                ### The Secret
                Brown the meat deeply until dark mahogany crust forms. Never rush the soffritto—cook it in butter and olive oil on low heat for 25 minutes until deeply caramelized. Simmer covered on lowest heat for 4 hours until the beef falls apart with a fork.
                
                - [ ] Pick up fresh pappardelle and Parmigiano-Reggiano from the Italian deli
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("cooking", "recipes", "family", "italian"),
            actionItems = listOf(
                "Pick up fresh pappardelle and Parmigiano-Reggiano from the Italian deli"
            ),
            summary = "Nana's slow-cooked 4-hour braised beef short rib ragu with caramelized soffritto and Chianti.",
            reflectionPrompts = listOf("What family culinary traditions do you want to keep alive for future generations?"),
            isProcessed = true
        ),

        // 19. Music & Focus Deep Dive
        Note(
            title = "Album Analysis: Radiohead 'In Rainbows' & Coding Flow",
            content = """
                Listening notes while refactoring the SQLite migration logic tonight.
                
                'In Rainbows' remains the quintessential record for sustained intellectual immersion:
                - *15 Step*: The 5/4 time signature polyrhythm forces just enough cognitive pattern-matching to quiet ambient mental chatter.
                - *Weird Fishes / Arpeggi*: Three interlocking clean guitars playing offset arpeggios that mimic the rhythm of typing code. It creates an almost trance-like sensation of forward momentum.
                - *Reckoner*: Warm tape saturation and intimate falsetto that de-escalates late-night debugging frustration.
            """.trimIndent(),
            mood = "Creative",
            tags = listOf("music", "radiohead", "flow-state", "creativity"),
            actionItems = emptyList(),
            summary = "Analysis of Radiohead's 'In Rainbows' and how its polyrhythms and guitar arpeggios induce deep programming flow.",
            reflectionPrompts = listOf("What auditory environments most reliably unlock your peak creative problem solving?"),
            isProcessed = true
        ),

        // 20. Tech Incident Retrospective
        Note(
            title = "Incident Post-Mortem: Auth Gateway Cascade Failure #891",
            content = """
                Post-mortem analysis for the 24-minute authentication service outage on Tuesday.
                
                ### Timeline of Failure
                - **14:12**: Primary DNS provider experienced packet loss in US-East.
                - **14:15**: Mobile clients attempted exponential retry storm without jitter.
                - **14:19**: Redis token validation cache exceeded max memory limit and entered swap.
                - **14:24**: Gateway health checks failed, taking down the entire API gateway cluster.
                
                ### Core Learnings
                Client-side retries without randomized jitter act as a self-inflicted Distributed Denial of Service (DDoS) attack during partial outages.
                
                ### Immediate Mitigations
                - [ ] Implement full jitter formula: `sleep = rand(0, min(cap, base * 2 ^ attempt))`
                - [ ] Configure Redis eviction policy to `volatile-lru`
                - [ ] Add circuit breaker to mobile network client with 30s open cooldown
            """.trimIndent(),
            mood = "Analytical",
            tags = listOf("postmortem", "incident", "architecture", "devops"),
            actionItems = listOf(
                "Implement full jitter formula: sleep = rand(0, min(cap, base * 2 ^ attempt))",
                "Configure Redis eviction policy to volatile-lru",
                "Add circuit breaker to mobile network client with 30s open cooldown"
            ),
            summary = "Root cause analysis of auth gateway cascade caused by client-side retry storms and Redis memory exhaustion.",
            reflectionPrompts = listOf(
                "How can your client architecture be made antifragile to backend failure?",
                "Are your circuit breaker trip thresholds tested under simulated network dropouts?"
            ),
            isProcessed = true
        ),

        // 21. Love & Relationship Vulnerability
        Note(
            title = "Hard conversation with Elena tonight",
            content = """
                We had our first real argument in months over dinner tonight.
                
                It started small—about dishes left in the sink—but we both knew it wasn't about dishes. She sat down, looked at me with tired eyes, and said: "You're physically sitting on this couch with me, but mentally you're 5,000 miles away debugging servers. I miss my best friend."
                
                Hearing her voice break cut right through my chest. She was completely right. I've been bringing work anxiety into our living room every night.
                
                We turned off our phones, sat on the rug, and held hands in the dark for two hours. I love this woman with everything in me, and I cannot take her presence for granted.
                
                - [ ] No laptop in the living room after 7:30 PM starting tonight
                - [ ] Plan our Friday date night at that candle-lit Italian place she loves
                - [ ] Look into her eyes when she speaks instead of glancing at my Apple Watch
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("love", "relationship", "vulnerability", "presence", "elena"),
            actionItems = listOf(
                "No laptop in the living room after 7:30 PM starting tonight",
                "Plan our Friday date night at that candle-lit Italian place she loves",
                "Look into her eyes when she speaks instead of glancing at my Apple Watch"
            ),
            summary = "Emotional conversation with Elena about work distraction; reconnecting over dinner and committing to evening phone-free presence.",
            reflectionPrompts = listOf(
                "What daily ritual can protect your intimate relationship from professional stress bleed?",
                "How can you actively demonstrate gratitude to the person who supports you behind the scenes?"
            ),
            isProcessed = true
        ),

        // 22. Family Concern & Parents Aging
        Note(
            title = "Noticing Dad's tremor during Sunday lunch",
            content = """
                Visited my parents in the suburbs on Sunday.
                
                Dad was pouring green tea, and I noticed his left hand shaking noticeably as he lifted the pot. He tried to steady it with his other thumb, laughing it off with "Well, getting older isn't for the faint of heart, kiddo."
                
                It hit me like a ton of bricks on the highway driving back. In my mind, Dad is still 42, the invincible giant who taught me how to ride a bicycle without training wheels and stayed up till 2 AM fixing my broken suspension. Seeing his hair completely silver and his steps slower terrifies me in a way I don't know how to articulate.
                
                Life is so fragile and moving so impossibly fast. I have to stop treating visits as an optional calendar item.
                
                - [ ] Call Dad every Tuesday and Thursday during morning commute
                - [ ] Help Mom schedule Dad's neurological checkup appointment
                - [ ] Plan a weekend fishing trip at the lake while he still has full mobility
            """.trimIndent(),
            mood = "Stressed",
            tags = listOf("family", "parents", "dad", "aging", "love", "fear"),
            actionItems = listOf(
                "Call Dad every Tuesday and Thursday during morning commute",
                "Help Mom schedule Dad's neurological checkup appointment",
                "Plan a weekend fishing trip at the lake while he still has full mobility"
            ),
            summary = "Emotional realization of Dad's aging and physical frailty; committing to weekly calls and a special fishing trip.",
            reflectionPrompts = listOf(
                "What unspoken love or appreciation do you need to express to your parents before it's too late?",
                "How are you managing the quiet dread of watching people you love grow old?"
            ),
            isProcessed = true
        ),

        // 23. Startup Failure & Rejection
        Note(
            title = "The investor pass: When your dream gets rejected",
            content = """
                Got the official rejection email from Sequoia for our pre-seed round at 9:15 AM today.
                
                "We deeply admire the technical vision, but the local on-device AI market is too nascent for our current fund thesis."
                
                I walked out to my car in the office parking garage and just cried for twenty minutes. We poured six months of 70-hour weeks, personal credit savings, and emotional sanity into building that prototype. Right now I feel like an absolute failure who let down my co-founder and squandered half a year.
                
                Taking three deep breaths. A VC rejection email is not a moral verdict on my worth as an engineer or human. The app works. 450 beta users love it. If institutional venture won't back us, we will bootstrap it one loyal subscriber at a time.
                
                - [ ] Send honest, resilient update to our beta community
                - [ ] Take Saturday completely off to hike in the mountains and let the grief settle
                - [ ] Review runway calculations for a bootstrapped micro-SaaS model
            """.trimIndent(),
            mood = "Stressed",
            tags = listOf("failure", "startup", "rejection", "resilience", "grief"),
            actionItems = listOf(
                "Send honest, resilient update to our beta community",
                "Take Saturday completely off to hike in the mountains and let the grief settle",
                "Review runway calculations for a bootstrapped micro-SaaS model"
            ),
            summary = "Grieving a major venture capital rejection while finding the emotional resolve to bootstrap independently.",
            reflectionPrompts = listOf(
                "What did this failure teach you about what you genuinely control versus external validation?",
                "How can you separate your identity as a human being from the outcome of a business venture?"
            ),
            isProcessed = true
        ),

        // 24. Loneliness & Yearning for Belonging
        Note(
            title = "Sitting in a crowded coffee shop feeling completely invisible",
            content = """
                It is pouring rain outside on 8th Street. The cafe is packed to the brim with couples laughing over pastries, college friends studying together, groups making weekend plans.
                
                I'm sitting alone in the corner booth with my cold brew, feeling an unbearable ache of loneliness.
                
                Ever since relocating to this city seven months ago, I've used work, code, and side projects as an emotional shield to avoid admitting how isolated I actually am. It's easy to posture as 'disciplined and grinding' when you're really just terrified of walking into an empty apartment every night.
                
                I don't need more GitHub stars or Twitter followers. I need human friends who know my flaws and still invite me over for Sunday dinners.
                
                - [ ] Sign up for the Thursday evening beginner bouldering meetup
                - [ ] Knock on neighbor Marcus's door and invite him over for homemade pasta
                - [ ] Put down the phone and make eye contact with the barista tomorrow
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("loneliness", "emotions", "mental-health", "connection", "vulnerability"),
            actionItems = listOf(
                "Sign up for the Thursday evening beginner bouldering meetup",
                "Knock on neighbor Marcus's door and invite him over for homemade pasta",
                "Put down the phone and make eye contact with the barista tomorrow"
            ),
            summary = "Vulnerable realization of chronic loneliness in a new city; acknowledging that overworking was a shield to hide isolation.",
            reflectionPrompts = listOf(
                "What small vulnerability can you risk today to initiate a genuine human connection?",
                "Where in your life are you mistaking isolation for independence?"
            ),
            isProcessed = true
        ),

        // 25. Fear of Making Mistakes & Perfectionism
        Note(
            title = "Paralyzed by the fear of making the wrong choice",
            content = """
                I have spent three days staring at the schema migration pull request without pressing Merge.
                
                Every time my mouse hovers over the button, an icy voice inside my skull whispers: 'If this script corrupts customer records during peak traffic, your entire reputation is torched. They will know you're not ready for this responsibility.'
                
                Perfectionism is just fear wearing a shiny medal. Trying to guarantee zero failure before acting is a guaranteed way to guarantee zero growth.
                
                I wrote 14 integration test cases. I verified the rollback script twice. Trust the preparation and let go of the illusion of absolute certainty.
                
                - [ ] Run the migration on staging with a simulated 100k user dataset
                - [ ] Hit merge during the scheduled low-traffic window tomorrow at 6:00 AM
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("fear", "perfectionism", "self-doubt", "courage", "decisions"),
            actionItems = listOf(
                "Run the migration on staging with a simulated 100k user dataset",
                "Hit merge during the scheduled low-traffic window tomorrow at 6:00 AM"
            ),
            summary = "Confronting perfectionism and the paralyzing fear of failure before a critical software migration.",
            reflectionPrompts = listOf(
                "What is the realistic worst-case scenario if you fail, and can you survive it?",
                "How does delaying this decision cost you more emotional energy than making an imperfect choice?"
            ),
            isProcessed = true
        ),

        // 26. Pure Gratitude & Quiet Joy
        Note(
            title = "A quiet Sunday morning that felt like pure peace",
            content = """
                Woke up at 7:15 AM without an alarm clock.
                
                Pale amber morning sunlight was cutting across the hardwood floor, illuminating tiny dust motes dancing in the air. Elena was still fast asleep beside me, breathing rhythmically, with the orange cat purring softly between her knees.
                
                I padded barefoot to the kitchen, ground the Ethiopian beans by hand, and watched the neighborhood wake up through the bay window. No Slack notifications. No production alarms. No nagging guilt that I wasn't being 'productive'.
                
                Just fifteen minutes of unfiltered gratitude. We spend our entire youth sprinting toward the next title, the next raise, the next milestone, believing happiness is waiting at the finish line.
                
                Happiness was right here: warm coffee, quiet morning sun, and the breathing of the person I love.
            """.trimIndent(),
            mood = "Calm",
            tags = listOf("gratitude", "peace", "love", "mindfulness", "joy", "elena"),
            actionItems = emptyList(),
            summary = "A peaceful Sunday morning reflection on gratitude, quiet intimacy, and stepping off the achievement treadmill.",
            reflectionPrompts = listOf(
                "What ordinary miracle in your daily life are you currently taking for granted?",
                "How can you protect these unstructured pockets of peace against calendar creep?"
            ),
            isProcessed = true
        ),

        // 27. Grief & Legacy of Lost Loved Ones
        Note(
            title = "Finding Grandpa Leo's pocketknife in an old coat",
            content = """
                Found Grandpa Leo's small brass pocketknife slipped inside the interior pocket of my winter coat today.
                
                The brass casing is worn completely smooth at the edges where his thumb rested for forty years. He died three years ago this November, but holding that heavy little knife brought his deep chuckle back so vividly that my eyes welled up on the subway platform.
                
                I remembered him sitting on the splintered porch steps, whittling cedar chips, telling me: "Kiddo, don't ever rush a job that's worth doing, and never carry a grudge that isn't paying you rent in your head."
                
                I carry so much needless hurry and resentment every week. I needed this reminder from him today more than he could ever know.
                
                - [ ] Polish the brass casing with mineral oil tonight
                - [ ] Call Grandma this evening just to hear her voice and ask about her garden
            """.trimIndent(),
            mood = "Reflective",
            tags = listOf("grief", "family", "memory", "loss", "grandpa", "love"),
            actionItems = listOf(
                "Polish the brass casing with mineral oil tonight",
                "Call Grandma this evening just to hear her voice and ask about her garden"
            ),
            summary = "Remembering Grandpa Leo through his worn brass pocketknife; reflecting on his timeless wisdom about patience and forgiveness.",
            reflectionPrompts = listOf(
                "What values from your ancestors are you actively embodying in your daily choices?",
                "What grudge are you carrying right now that is renting free space in your heart?"
            ),
            isProcessed = true
        ),

        // 28. Exhaustion & Acknowledging Burnout
        Note(
            title = "Admitting to myself that I am completely burnt out",
            content = """
                I have to stop lying to myself: I am totally and utterly exhausted.
                
                For the past three weeks, I've woken up with a knot in my stomach. Tasks that used to take twenty minutes feel like climbing a vertical cliff. I've become short-tempered with teammates and emotionally numb to creative ideas that used to make my heart race.
                
                I kept telling myself 'just push through till after the beta release', but the finish line always moves another month back. Burnout is not an endurance test you win by suffering; it is a warning light flashing on your engine dashboard.
                
                I finally messaged my engineering director today and requested 4 days of PTO next week. No phone. No laptop. Just sleeping, trail walking, and staring at trees.
                
                - [ ] Set up automated out-of-office autoreply on Slack and email
                - [ ] Hand over on-call pager rotation to Derek for next weekend
                - [ ] Leave company laptop locked in desk drawer while away
            """.trimIndent(),
            mood = "Stressed",
            tags = listOf("burnout", "mental-health", "exhaustion", "boundaries", "self-care"),
            actionItems = listOf(
                "Set up automated out-of-office autoreply on Slack and email",
                "Hand over on-call pager rotation to Derek for next weekend",
                "Leave company laptop locked in desk drawer while away"
            ),
            summary = "Confronting severe burnout and emotional exhaustion; setting firm boundaries and taking unplugged PTO.",
            reflectionPrompts = listOf(
                "What boundary are you afraid to set because you worry people will think less of you?",
                "What would happen to your life if you stopped treating your body like an engine and started treating it like a home?"
            ),
            isProcessed = true
        )
    )

    suspend fun seedSampleNotes(repository: NoteRepository) = withContext(Dispatchers.IO) {
        val existingNotes = repository.getAllNotes().firstOrNull() ?: emptyList()
        val existingTitles = existingNotes.map { it.title.trim().lowercase(java.util.Locale.ROOT) }.toSet()
        for (note in sampleNotes) {
            val normalizedTitle = note.title.trim().lowercase(java.util.Locale.ROOT)
            if (!existingTitles.contains(normalizedTitle)) {
                repository.saveNote(note)
            }
        }
        repository.rebuildSynapticGraph()
    }
}
