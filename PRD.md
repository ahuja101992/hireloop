# HireLoop — PRD
> Java Spring Boot career transition platform. Runs on Mac. No UI unless specified. Claude API for LLM. Postgres for state. Config-driven rules engine.

---

## 1. GOAL
Automate Principal SWE job search for Big Tech + Austin market. Guide prep → discover jobs → tailor resume → track applications. Human confirms before every apply.

## 2. NON-GOALS
- No chat UI (CLI or notification-driven interaction only)
- No LinkedIn/Indeed direct apply
- No autonomous apply (always confirm first)
- No real-time intel (batch scrape only)

---

## 3. TECH STACK
| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Scheduler | Spring `@Scheduled` |
| LLM | Claude API (claude-sonnet-4-20250514) via HTTP (RestTemplate/WebClient) |
| Browser automation | Playwright for Java |
| Database | Postgres (via Spring Data JPA) |
| Migrations | Flyway |
| Config | YAML (`application.yml` + `targets.yml` + `filters.yml`) |
| Gmail | Gmail API v1 (OAuth2) |
| Notifications | JavaMail (SMTP → user's email) |
| Resume input | DOCX (Apache POI to parse → store as JSON) |
| Build | Maven |

---

## 4. PROJECT STRUCTURE
```
hireloop/
├── src/main/java/com/hireloop/
│   ├── HireLoopApplication.java
│   ├── config/
│   │   ├── AppConfig.java              # loads targets.yml, filters.yml
│   │   ├── ClaudeConfig.java           # Claude API config
│   │   └── GmailConfig.java            # Gmail OAuth2
│   ├── model/                          # JPA entities
│   │   ├── Job.java
│   │   ├── Application.java
│   │   ├── CompanyIntel.java
│   │   ├── TopicCoverage.java
│   │   └── PrepReadiness.java
│   ├── repository/                     # Spring Data repos
│   ├── service/
│   │   ├── intel/
│   │   │   ├── IntelScrapeService.java
│   │   │   └── PrepTrackerService.java
│   │   ├── jobs/
│   │   │   ├── JobPollerService.java
│   │   │   ├── JobFilterService.java
│   │   │   └── FitScorerService.java
│   │   ├── resume/
│   │   │   ├── ResumeParserService.java
│   │   │   └── ResumeAdapterService.java
│   │   ├── apply/
│   │   │   └── ApplyEngine.java
│   │   ├── gmail/
│   │   │   └── GmailTrackerService.java
│   │   └── notification/
│   │       └── NotificationService.java
│   └── scheduler/
│       ├── IntelScheduler.java
│       ├── JobPollScheduler.java
│       └── GmailScanScheduler.java
├── src/main/resources/
│   ├── application.yml
│   ├── config/
│   │   ├── targets.yml
│   │   └── filters.yml
│   └── db/migration/                   # Flyway SQL scripts
├── resume/
│   └── resume.docx                     # user drops file here
└── pom.xml
```

---

## 5. CONFIGURATION FILES

### filters.yml
```yaml
filters:
  max_age_days: 7
  min_fit_score: 75
  require_direct_apply: true
  target_levels: ["Principal", "Staff", "Senior Staff"]
  locations: ["Austin, TX", "Remote"]
  salary_min: 0                         # 0 = ignore
  exclude_keywords: []
  require_keywords: ["distributed systems"]
  apply_readiness_threshold_default: 80 # global threshold to activate polling
```

### targets.yml
```yaml
companies:
  - name: Apple
    ats: workday
    careers_url: https://jobs.apple.com
    priority: high
    apply_readiness_threshold: 80

  - name: Google
    ats: greenhouse
    api_url: https://boards-api.greenhouse.io/v1/boards/google/jobs
    priority: high
    apply_readiness_threshold: 85

  - name: Stripe
    ats: lever
    api_url: https://api.lever.co/v0/postings/stripe
    priority: high
    apply_readiness_threshold: 85

  - name: Indeed Austin
    ats: lever
    api_url: https://api.lever.co/v0/postings/indeed
    priority: medium
    apply_readiness_threshold: 78

intel_sources:
  - glassdoor
  - blind
  - reddit_cscareerquestions
  - reddit_experienceddevs
  - levels_fyi
  - leetcode_discuss
```

---

## 6. DATABASE SCHEMA

```sql
-- V1__init.sql

CREATE TABLE company_intel (
  id BIGSERIAL PRIMARY KEY,
  company_name VARCHAR(100),
  source VARCHAR(50),
  raw_data TEXT,
  interview_rounds JSONB,       -- [{round, format, topics[], difficulty}]
  scraped_at TIMESTAMP,
  UNIQUE(company_name, source)
);

CREATE TABLE topic_universe (
  id BIGSERIAL PRIMARY KEY,
  category VARCHAR(50),         -- DSA | SYSTEM_DESIGN | BEHAVIORAL
  topic VARCHAR(100),
  global_frequency DECIMAL,     -- 0.0-1.0 aggregate across all companies
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE company_topic_frequency (
  id BIGSERIAL PRIMARY KEY,
  company_name VARCHAR(100),
  topic_id BIGINT REFERENCES topic_universe(id),
  frequency DECIMAL,            -- 0.0-1.0 for this specific company
  UNIQUE(company_name, topic_id)
);

CREATE TABLE topic_coverage (
  id BIGSERIAL PRIMARY KEY,
  topic_id BIGINT REFERENCES topic_universe(id),
  status VARCHAR(20),           -- NOT_STARTED | IN_PROGRESS | COVERED | WEAK
  notes TEXT,
  updated_at TIMESTAMP
);

CREATE TABLE prep_readiness (
  id BIGSERIAL PRIMARY KEY,
  company_name VARCHAR(100),    -- NULL = global
  score DECIMAL,
  dsa_score DECIMAL,
  sd_score DECIMAL,
  behavioral_score DECIMAL,
  calculated_at TIMESTAMP
);

CREATE TABLE job (
  id BIGSERIAL PRIMARY KEY,
  company_name VARCHAR(100),
  title VARCHAR(200),
  jd_url VARCHAR(500),
  jd_text TEXT,
  ats_type VARCHAR(50),
  posted_at TIMESTAMP,
  discovered_at TIMESTAMP,
  fit_score DECIMAL,
  status VARCHAR(30),           -- NEW | SCORED | NOTIFIED | CONFIRMED | APPLIED | SKIPPED
  tailored_resume_json JSONB
);

CREATE TABLE application (
  id BIGSERIAL PRIMARY KEY,
  job_id BIGINT REFERENCES job(id),
  applied_at TIMESTAMP,
  gmail_thread_id VARCHAR(100),
  pipeline_status VARCHAR(30),  -- APPLIED | ACKNOWLEDGED | RECRUITER_SCREEN | INTERVIEW | OFFER | REJECTED
  last_updated TIMESTAMP
);
```

---

## 7. PHASES & MODULES

### PHASE 1a — Aggregate Intel Scrape
**Class:** `IntelScrapeService`
**Trigger:** Manual CLI arg `--scrape-intel` OR `@Scheduled(cron = "0 0 0 * * SUN")` (weekly)
**Logic:**
1. For each source in `intel_sources`, use Playwright to scrape interview reports filtered by role=Principal/Staff SWE
2. Send raw text to Claude API with prompt: extract structured interview rounds, topics, frequency signals
3. Persist to `company_intel` and aggregate into `topic_universe` + `company_topic_frequency`
4. Email user: "Intel refresh complete. Topic universe updated. X new topics added."

**Claude prompt pattern:**
```
Given these interview reports for [COMPANY] Principal SWE role:
[RAW_TEXT]

Extract as JSON:
{
  "rounds": [{"name":"","format":"","topics":[],"difficulty":""}],
  "topic_frequencies": [{"topic":"","category":"DSA|SD|BEHAVIORAL","frequency":0.0}]
}
Return JSON only.
```

### PHASE 1b — Prep Tracker
**Class:** `PrepTrackerService`
**Trigger:** CLI args only
- `--update-topic "Dynamic Programming" COVERED "Did 15 problems, feel strong"`
- `--update-topic "Payment Systems" WEAK "Read chapter, need more practice"`
- `--show-readiness` → prints global + per-company readiness to console + emails report

**Readiness Calculation:**
```
companyScore = Σ (topic.frequency_for_company × topic.coverage_weight) / Σ topic.frequency_for_company

coverage_weight: COVERED=1.0, IN_PROGRESS=0.5, WEAK=0.3, NOT_STARTED=0.0

globalScore = Σ (topic.global_frequency × topic.coverage_weight) / Σ topic.global_frequency
```

**Auto-activation logic (in `JobPollScheduler`):**
```java
// Before polling a company, check:
PrepReadiness r = prepReadinessRepo.findByCompanyName(company.getName());
double threshold = company.getApplyReadinessThreshold(); // from targets.yml
if (r.getScore() < threshold) {
    log.info("Skipping {} — readiness {}/{}", company.getName(), r.getScore(), threshold);
    return;
}
```

**Readiness Report (emailed + console):**
```
=== HireLoop Readiness Report — [DATE] ===

GLOBAL: 67/100
  DSA:           71%
  System Design: 55%  ← focus here
  Behavioral:    80%

PER COMPANY:
  Apple   : 84/80 ✅ READY TO APPLY
  Google  : 71/85 ❌ Gap: Payment Systems, Rate Limiting
  Stripe  : 68/85 ❌ Gap: Distributed consensus, Idempotency
  Indeed  : 79/78 ✅ READY TO APPLY

TOP 3 TOPICS TO STUDY NOW (highest ROI across gaps):
  1. Payment Systems    → unlocks Google + Stripe
  2. Rate Limiting      → unlocks Google
  3. Idempotency        → unlocks Stripe
```

### PHASE 1c — Company Intel Brief
**Class:** `IntelScrapeService.generateBrief(companyName)`
**Trigger:** CLI `--brief Apple` OR auto-triggered when company readiness crosses threshold
**Output:** Email with structured brief (rounds, recent reports, suggested LC problems, SD cases)

### PHASE 2 — Job Discovery + Fit Scoring + Resume Tailoring

**`JobPollerService`**
- Runs `@Scheduled(cron = "0 0 8 * * *")` daily
- For each company in targets.yml where readiness >= threshold:
  - Greenhouse/Lever: hit public API, parse JSON
  - Workday/others: Playwright scrape careers page
- Filter by `filters.yml` rules in `JobFilterService`:
  - Drop if `posted_at` > `max_age_days` old
  - Drop if title doesn't match `target_levels`
  - Drop if `exclude_keywords` found in JD
  - Drop if `require_keywords` missing from JD
- Persist surviving jobs with status=NEW

**`FitScorerService`**
- For each NEW job, send (resume JSON + JD text) to Claude API
- Claude returns: `{"score": 82, "matching_points": [], "gaps": [], "resume_changes": []}`
- Jobs scoring < `min_fit_score` → status=SKIPPED
- Jobs scoring >= `min_fit_score` → trigger `ResumeAdapterService`

**Claude fit score prompt:**
```
Master resume: [RESUME_JSON]
Job description: [JD_TEXT]

Return JSON only:
{
  "score": 0-100,
  "matching_points": ["..."],
  "gaps": ["..."],
  "resume_changes": [{"section":"","original":"","suggested":"","reason":""}],
  "change_magnitude": "MINOR|MAJOR"  // MINOR < 15% bullets changed
}
```

**`ResumeAdapterService`**
- Apply suggested changes to resume JSON
- Store tailored version in `job.tailored_resume_json`
- Render to DOCX via Apache POI
- Trigger `NotificationService`

**`NotificationService`**
```
MINOR change jobs → single digest email daily:
  "3 jobs ready to apply. Resume changes are minor."
  [Job 1: Apple — Senior Staff SWE — Score 88 — 2 bullet tweaks — Reply APPLY-1]
  [Job 2: Indeed — Principal SWE — Score 79 — 1 section reorder — Reply APPLY-2]

MAJOR change jobs → individual email immediately:
  "Resume needs significant changes for this role. Review before applying."
  [Full diff attached]
  [Reply APPLY or SKIP]
```

User replies to email → `GmailTrackerService` detects reply → triggers `ApplyEngine`

### PHASE 3 — Gmail Tracker

**`GmailTrackerService`**
- Runs `@Scheduled(fixedDelay = 300000)` every 5 min
- Gmail API: search `label:inbox` for new messages
- Two functions:
  1. **User reply detection:** watch for APPLY-N / SKIP replies → update job status → trigger apply
  2. **Application tracking:** match sender domain to applied companies → classify email:
     - Confirmation → status=ACKNOWLEDGED
     - Recruiter outreach → status=RECRUITER_SCREEN → auto-trigger company brief email
     - Interview invite → status=INTERVIEW
     - Rejection → status=REJECTED

### PHASE 4 — Apply Engine

**`ApplyEngine`**
- Playwright for Java
- Strategy pattern: `GreenhouseApplyStrategy`, `LeverApplyStrategy`, `WorkdayApplyStrategy`
- Each strategy: navigate to job URL → fill form fields → attach tailored resume DOCX → submit
- After submit: update `application` table, start Gmail tracking for that thread
- **Always requires confirmed=true on job record before executing**

---

## 8. RESUME HANDLING

**On startup (or CLI `--load-resume`):**
1. Apache POI reads `resume/resume.docx`
2. Extract structured JSON:
```json
{
  "name": "", "email": "", "phone": "",
  "summary": "",
  "experience": [
    {
      "company": "", "title": "", "dates": "",
      "bullets": ["..."]
    }
  ],
  "skills": [],
  "education": []
}
```
3. Store in `resume_master` table (single row, upsert)
4. This JSON is what Claude operates on — never the raw DOCX

---

## 9. LLM PROVIDER STRATEGY (Runtime Pluggable)

**Architecture:** Strategy pattern allows swapping LLM providers at runtime via config. No code changes needed.

**`LlmProvider.java`** — interface
```java
public interface LlmProvider {
    LlmResponse score(String resume, String jd);
    LlmResponse adaptResume(String resume, String jd, List<String> changes);
    LlmResponse extractIntel(String rawText);
    LlmResponse generateBrief(String intelJson);
}
```

**Implementations:**
- `ClaudeProvider.java` — POST to Claude API (default)
- `GeminiProvider.java` — POST to Gemini API
- `OpenAiProvider.java` — POST to OpenAI API (future)

**`LlmProviderFactory.java`**
```java
@Component
public class LlmProviderFactory {
    @Value("${llm.provider:claude}")
    private String provider;
    
    public LlmProvider getProvider() {
        return switch(provider) {
            case "claude" -> new ClaudeProvider(claudeConfig);
            case "gemini" -> new GeminiProvider(geminiConfig);
            default -> throw new IllegalArgumentException("Unknown provider: " + provider);
        };
    }
}
```

**Usage in any service:**
```java
@Service
public class FitScorerService {
    @Autowired private LlmProviderFactory factory;
    
    public void scoreJob(Job job) {
        LlmProvider llm = factory.getProvider();
        LlmResponse response = llm.score(resumeJson, jobJd);
    }
}
```

**Config in `application.yml`:**
```yaml
llm:
  provider: claude                    # Switch: claude | gemini | openai
  
  claude:
    api-key: ${CLAUDE_API_KEY}
    model: claude-sonnet-4-20250514
    base-url: https://api.anthropic.com/v1/messages
    max-tokens: 2000
    
  gemini:
    api-key: ${GEMINI_API_KEY}
    model: gemini-2.0-flash
    base-url: https://generativelanguage.googleapis.com/v1beta/models
    max-tokens: 2000
    
  openai:
    api-key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
    base-url: https://api.openai.com/v1/chat/completions
    max-tokens: 2000
```

**Prompts — centralized in `Prompts.java`**
```java
public class Prompts {
    public static final String SYSTEM_JSON = 
        "You are a precise JSON extractor. Return ONLY valid JSON. No markdown, no preamble.";
    
    public static final String SCORE_JOB = 
        "Master resume:\n%s\n\nJob description:\n%s\n\n" +
        "Return JSON: {\"score\": 0-100, \"matching_points\": [], \"gaps\": [], " +
        "\"resume_changes\": [{\"section\":\"\",\"original\":\"\",\"suggested\":\"\"}], " +
        "\"change_magnitude\": \"MINOR|MAJOR\"}";
    
    public static final String ADAPT_RESUME = 
        "Master resume JSON:\n%s\n\nJob description:\n%s\n\nChanges to apply:\n%s\n\n" +
        "Return modified resume JSON only. Preserve structure.";
    
    public static final String EXTRACT_INTEL = 
        "Interview reports for %s Principal SWE:\n%s\n\n" +
        "Return JSON: {\"rounds\": [{\"name\":\"\",\"format\":\"\",\"topics\":[],\"difficulty\":\"\"}], " +
        "\"topic_frequencies\": [{\"topic\":\"\",\"category\":\"DSA|SD|BEHAVIORAL\",\"frequency\":0.0}]}";
    
    public static final String GENERATE_BRIEF = 
        "Intel data:\n%s\n\nResume JSON:\n%s\n\n" +
        "Generate prep brief for this company. Return JSON: " +
        "{\"rounds\": [], \"interview_tips\": [], \"suggested_lc_problems\": [], " +
        "\"system_design_cases\": [], \"behavioral_focus\": []}";
}
```

**Cost comparison (monthly):**
```
Claude API:  ~$0.85 (or $0 if using existing Pro $5 credit)
Gemini:      $0.00 (free tier, unlimited calls ≤60/min)
OpenAI:      ~$2.50

To switch: change llm.provider in application.yml + restart
```

---

---

## 15. REST API LAYER

**Base URL:** `http://localhost:8080/api`

All endpoints return JSON. No HTML.

### Topic & Readiness Endpoints

```
GET  /api/topics
     Response: [{id, category, topic, global_frequency, coverage_status, notes, updated_at}, ...]

POST /api/topics/{id}/update
     Body: {status: "COVERED|IN_PROGRESS|WEAK|NOT_STARTED", notes: "..."}
     Response: {id, status, updated_at}

GET  /api/readiness
     Response: {
       global: {score: 67, dsa: 71, sd: 55, behavioral: 80, 
                 next_topics: [{topic, roi_score}]},
       by_company: [{name, score, threshold, ready: true/false}, ...]
     }

GET  /api/readiness/{company}
     Response: {company, score, threshold, breakdown_by_category, gaps: []}
```

### Job Endpoints

```
GET  /api/jobs?status=NEW,NOTIFIED,APPLIED
     Response: [{id, company, title, score, status, posted_at, tailored_resume}, ...]

GET  /api/jobs/{id}
     Response: {id, company, title, jd_url, score, status, 
                tailored_resume_json, resume_diff, resume_change_magnitude}

POST /api/jobs/{id}/confirm-apply
     Body: {confirmed: true}
     Response: {id, status: "CONFIRMED", scheduled_apply_time}

POST /api/jobs/{id}/skip
     Response: {id, status: "SKIPPED"}

POST /api/jobs/{id}/view-diff
     Response: {original_resume_section: "...", tailored_section: "...", changes: []}
```

### Application Pipeline Endpoints

```
GET  /api/applications
     Response: [{id, job_id, company, title, applied_at, 
                 pipeline_status, last_email_subject, last_email_date}, ...]

GET  /api/applications/{id}/emails
     Response: [{from, subject, body, received_at, classification}]
     Classifications: CONFIRMATION | RECRUITER_OUTREACH | INTERVIEW_INVITE | REJECTION
```

### Configuration Endpoints

```
GET  /api/config
     Response: {filters: {...}, targets: [...], llm_provider: "claude"}

POST /api/config/filters
     Body: {max_age_days: 7, min_fit_score: 75, ...}
     Response: {updated: true, filters: {...}}

POST /api/config/targets
     Body: [{name: "Apple", ats: "workday", ...}, ...]
     Response: {updated: true, targets: [...]}

POST /api/config/llm
     Body: {provider: "claude"|"gemini"}
     Response: {provider: "...", requires_restart: true}
```

### Intel Endpoints

```
GET  /api/intel/{company}
     Response: {company, rounds: [...], topic_frequencies: [...], 
                last_scraped_at, sources: [...]}

POST /api/intel/scrape-all
     Response: {status: "SCHEDULED", scheduled_for: "..."}

GET  /api/intel/aggregate
     Response: {topic_universe: [...], global_patterns: {...}}
```

---

## 16. REACT DASHBOARD (Single Page App)

**Location:** `src/main/resources/static/` (served by Spring Boot on `/ui`)

**Stack:** React 18, TypeScript, Tailwind CSS, Axios (HTTP client)

**Core Components:**

### 1. `TopicTracker.tsx`
Displays all topics with coverage status. User can click to edit coverage.

```tsx
interface Topic {
  id: number;
  category: "DSA" | "SD" | "BEHAVIORAL";
  topic: string;
  global_frequency: number;
  coverage_status: "NOT_STARTED" | "IN_PROGRESS" | "COVERED" | "WEAK";
  notes: string;
}

// UI: table with columns [Category, Topic, Frequency, Status, Notes, Action]
// Status cells are clickable dropdowns
// Notes are inline editable text areas
// On change: POST /api/topics/{id}/update
```

### 2. `ReadinessDashboard.tsx`
Live readiness scores — global + per-company.

```tsx
// Top section: Global score 67/100 with breakdown by category (pie chart or bars)
// Mid section: Next topics to study (ranked by ROI)
// Bottom section: Per-company readiness table
//   [Company, Readiness Score, Threshold, Status (READY/NOT READY), Top Gap]
//   Color coding: green if ready, red if not
```

### 3. `JobList.tsx`
All discovered jobs with fit scores and actions.

```tsx
interface Job {
  id: number;
  company: string;
  title: string;
  score: number;
  status: "NEW" | "SCORED" | "NOTIFIED" | "CONFIRMED" | "APPLIED" | "SKIPPED";
  posted_at: Date;
  change_magnitude: "MINOR" | "MAJOR";
}

// UI: table with columns [Company, Title, Score, Status, Posted, Action]
// Rows are color-coded by status
// "CONFIRMED" jobs show apply timestamp + countdown to apply
// Action buttons: VIEW_DIFF | CONFIRM_APPLY | SKIP
// Clicking VIEW_DIFF opens side panel showing resume changes
```

### 4. `PipelineView.tsx`
All applications with pipeline stage and latest email.

```tsx
interface Application {
  id: number;
  job_id: number;
  company: string;
  title: string;
  applied_at: Date;
  pipeline_status: "APPLIED" | "ACKNOWLEDGED" | "RECRUITER_SCREEN" | "INTERVIEW" | "OFFER" | "REJECTED";
  last_email: {subject: string; date: Date; classification: string};
}

// UI: Kanban board with columns [APPLIED, RECRUITER_SCREEN, INTERVIEW, OFFER, REJECTED]
// Each card: company + title + date + click to see all emails
// Clicking card opens email thread view
```

### 5. `SettingsModal.tsx`
Edit filters, targets, LLM provider.

```tsx
// Tabs: [Filters, Companies, LLM]
// Filters tab: editable YAML/form for max_age, min_fit_score, keywords, levels, locations
// Companies tab: table of target companies + edit/delete rows
// LLM tab: dropdown to switch between claude|gemini, show active model + last cost
// Save button POSTs to /api/config/{section}
```

### 6. `Layout.tsx` (Shell)
Top nav with logo, tabs for [Dashboard, Jobs, Pipeline, Settings]

---

## 17. CLI COMMANDS (Spring Boot `ApplicationRunner`)

```
--load-resume              Parse resume.docx → store as JSON
--scrape-intel             Run full intel scrape for all companies
--brief [company]          Generate + email company prep brief
--update-topic "[topic]" [STATUS] "[notes]"
--show-readiness           Print + email readiness report
--list-jobs                Show all NEW/NOTIFIED jobs with scores
--confirm-apply [job-id]   Manually confirm apply for a job
--show-pipeline            Show all applications + gmail status
```

---

## 18. PHASED DELIVERY ORDER

```
BACKEND:
Step 1 : Flyway migrations — full schema
Step 2 : Config loading — AppConfig reads targets.yml + filters.yml
Step 3 : ResumeParserService — DOCX → JSON
Step 4 : LlmProviderFactory + ClaudeProvider — pluggable LLM
Step 5 : IntelScrapeService — scrape + LLM extraction → DB
Step 6 : PrepTrackerService — topic update + readiness calc
Step 7 : NotificationService — email infra
Step 8 : REST controllers for topics + readiness + config
Step 9 : JobPollerService — ATS polling (Greenhouse/Lever API first)
Step 10: JobFilterService — filters.yml rule engine
Step 11: FitScorerService — LLM scoring
Step 12: ResumeAdapterService — LLM tailoring → DOCX render
Step 13: REST controllers for jobs
Step 14: GmailTrackerService — inbox scan + reply detection
Step 15: REST controllers for applications
Step 16: ApplyEngine — Greenhouse strategy first
Step 17: WorkdayApplyStrategy — Playwright scraping

FRONTEND:
Step 18: React app scaffold + Layout component
Step 19: TopicTracker component + wire to /api/topics
Step 20: ReadinessDashboard component + wire to /api/readiness
Step 21: JobList component + wire to /api/jobs
Step 22: PipelineView component + wire to /api/applications
Step 23: SettingsModal component + wire to /api/config
Step 24: Email diff viewer in JobList side panel
```

---

## 19. ENVIRONMENT VARIABLES

Set these in your shell or `.env` file. Recommended: save to `~/.hireloop/.env` and source it.

```bash
export CLAUDE_API_KEY="sk-ant-v0-YOUR_KEY"
export GEMINI_API_KEY="AIzaSy_YOUR_KEY"
export GMAIL_ADDRESS="your.email@gmail.com"
export GMAIL_APP_PASSWORD="xxxx xxxx xxxx xxxx"
export GMAIL_OAUTH_CREDENTIALS="/Users/akshitahuja/.hireloop/credentials.json"
export DB_URL="jdbc:postgresql://localhost:5432/hireloop"
export DB_USERNAME="hireloop_user"
export DB_PASSWORD="hireloop_password"
export LLM_PROVIDER="claude"
export HIRELOOP_HOME="/Users/akshitahuja/IdeaProjects/hireloop"
```

**Load on startup:**
```bash
# Add to ~/.zshrc:
source ~/.hireloop/.env
```

---

## 20. KEY DESIGN RULES FOR CLAUDE CODE

**General Architecture:**
- All LLM calls return JSON only — system prompt enforces this on every request
- All prompts centralized in `Prompts.java` — no magic strings scattered in code
- Config-driven everywhere — no hardcoded values, thresholds, or company names
- Strategy pattern for ATS types — adding a new ATS = new Strategy class only
- Strategy pattern for LLM providers — swap Claude ↔ Gemini via config, zero code changes

**Database & State:**
- JobFilterService reads filters.yml at runtime — rule changes don't require restart
- Readiness gate in JobPollScheduler — company only polled if readiness >= threshold
- Human confirmation gate in ApplyEngine — `confirmed` flag must be true, no exceptions
- All dates stored as TIMESTAMP in UTC, rendered as local time in UI

**API & REST:**
- All endpoints return 200/error JSON, never HTML
- Error responses: `{"error": "message", "timestamp": "ISO8601"}`
- Validation: request bodies validated via Jakarta Bean Validation
- CORS enabled for localhost:3000 (React dev server)

**Frontend:**
- React single-page app, no routing (all views in tabs)
- Components fetch from /api on mount and on user action
- Optimistic UI updates — show change immediately, rollback if API fails
- Tailwind CSS for styling (utility-first, no custom CSS)
- Responsive: works on laptop + iPad (min width 768px)

**External Integrations:**
- Gmail API: OAuth2 flow, store refresh_token in secure config
- Playwright: headless=true on Mac, use `BrowserType.LAUNCH_OPTIONS`
- ATS APIs (Greenhouse/Lever): public JSON APIs, no auth needed
- Scraping (Glassdoor/Blind/Reddit): use Playwright with random delays 2-5 sec between requests

**Notifications:**
- Email via JavaMail to user's Gmail account
- Subject lines include [HireLoop] prefix + actionable tokens (APPLY-N, SKIP-N)
- Job notification digest sent daily if NEW or NOTIFIED jobs exist
- Readiness reports sent weekly or on-demand

**Testing (Optional, but recommended):**
- Unit tests for FitScorerService (mock LLM responses)
- Integration tests for JobFilterService (test filters.yml rules)
- E2E test for full flow: job discovery → score → notify (with testcontainers for Postgres)

---

## 21. ON STARTUP

1. Load `targets.yml` and `filters.yml` into memory
2. Create Postgres tables via Flyway (auto-run on startup)
3. Parse `resume/resume.docx` and store as JSON in DB
4. Start Spring schedulers:
   - Weekly: intel scrape (Sunday midnight)
   - Daily: job polling (8am)
   - Every 5 min: Gmail inbox scan
5. Serve React dashboard on `http://localhost:8080/ui`
6. Log: "HireLoop started. Dashboard: http://localhost:8080/ui"

---

## 22. SWITCHING LLMs AT RUNTIME

**To switch from Claude to Gemini:**
1. Set env var: `LLM_PROVIDER=gemini`
2. Restart application
3. All LLM calls route to Gemini automatically
4. No code changes needed

**To test Gemini before switching:**
1. In SettingsModal, use dropdown to switch provider
2. System will request restart
3. Restart application (or API will fail gracefully on next LLM call)

---

## 23. DELIVERABLE CHECKLIST

- [x] application.yml with all configs
- [x] targets.yml template
- [x] filters.yml template
- [x] Database schema (Flyway migrations)
- [x] Spring Boot app with all services
- [x] LLM provider abstraction (Claude + Gemini)
- [x] REST API (15+ endpoints)
- [x] React dashboard (5 components)
- [x] Email notifications
- [x] Gmail integration
- [x] ApplyEngine (Greenhouse first)
- [x] CLI commands
- [x] README.md with setup instructions
- [x] pom.xml with all dependencies
