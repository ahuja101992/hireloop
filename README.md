# HireLoop - Intelligent Job Opportunity & Interview Prep Manager

An AI-powered application that helps you discover, score, and prepare for job opportunities using Claude AI for intelligent analysis.

## 📋 End-to-End User Flow

### 1. **Job Discovery & Management**
   - **Input**: Job description (manual entry or scraped from job sites)
   - **System**: Jobs stored with company name, title, job description, and URL
   - **Output**: Job database ready for analysis

### 2. **Resume-Job Fit Scoring**
   - **Input**: Your resume + Job description
   - **Claude AI Analysis**: Evaluates resume against job requirements
   - **Output**: 
     - Fit score (0-100)
     - Key skill matches
     - Skill gaps
     - Recommendation (strong/moderate/weak fit)
   - **Storage**: Score saved to job record with status "SCORED"

### 3. **Resume Adaptation**
   - **Input**: Your original resume + Target job description
   - **Claude AI Transformation**: 
     - Tailors professional summary for the role
     - Highlights relevant skills and experiences
     - Adds keyword alignment from job posting
     - Suggests additional projects/skills to emphasize
   - **Output**: Tailored resume JSON ready for application

### 4. **Interview Preparation**
   - **Track Readiness**: Update prep scores for each company
     - DSA (Data Structures & Algorithms): 40% weight
     - System Design: 40% weight
     - Behavioral: 20% weight
   - **Global Readiness**: View overall prep across all target companies
   - **Company-Specific Tracking**: Monitor progress per company

### 5. **Application Pipeline**
   - **Create Application**: Link job to application submission
   - **Track Status**: Monitor pipeline status (Applied, Interviewing, Offer, Rejected, etc.)
   - **History**: Maintain record of all applications

### 6. **Email Notifications** *(Enabled)*
   - New job opportunity alerts
   - Interview prep readiness reports (weekly, automated)
   - Application status updates (via Gmail inbox scan every 5 min)

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+** (tested with Java 17.0.17)
- **Maven 3.9+**
- **PostgreSQL 15+** (running locally)
- **Claude API Key** from Anthropic

### 1. Database Setup

Create PostgreSQL user and database:

```bash
psql -h localhost -U postgres

# In psql:
CREATE USER hireloop_user WITH PASSWORD 'hireloop_password';
ALTER ROLE hireloop_user WITH CREATEDB;
CREATE DATABASE hireloop OWNER hireloop_user;
GRANT USAGE ON SCHEMA public TO hireloop_user;
```

Or use the provided init script:

```bash
chmod +x /tmp/init_db.sh
/tmp/init_db.sh
```

### 2. Environment Setup

Create `~/.hireloop/.env` file with credentials:

```bash
mkdir -p ~/.hireloop
cat > ~/.hireloop/.env << 'EOF'
export DB_URL=jdbc:postgresql://localhost:5432/hireloop
export DB_USERNAME=hireloop_user
export DB_PASSWORD=hireloop_password
export CLAUDE_API_KEY=your_anthropic_api_key_here
EOF

source ~/.hireloop/.env
```

### 3. Build the Application

```bash
cd /Users/akshitahuja/IdeaProjects/hireloop
source ~/.hireloop/.env
mvn clean package -DskipTests
```

### 4. Run the Application

```bash
source ~/.hireloop/.env
java -jar target/hireloop-1.0.0.jar
```

The application will start on **http://localhost:8080**

---

## 🖥️ Dashboard (UI)

A React-style dashboard (served as static JSX from `src/main/resources/static/`) is available at **http://localhost:8080** (`index.html`). Components:

| Component | Purpose |
|-----------|---------|
| `Layout.jsx` | Shell/nav across Dashboard, Jobs, Pipeline, Settings |
| `JobList.jsx` | Discovered jobs, scores, confirm/skip actions |
| `ReadinessDashboard.jsx` | Global + per-company prep readiness |
| `TopicTracker.jsx` | DSA/System Design/Behavioral topic coverage |
| `PipelineView.jsx` | Application pipeline status |
| `SettingsModal.jsx` | Resume upload, target companies, job filters, email preferences, auto-apply toggle |

---

## 📡 API Endpoints

### Jobs Management

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/jobs` | List all jobs |
| GET | `/api/jobs/{id}` | Get job details |
| POST | `/api/jobs` | Create new job |
| POST | `/api/jobs/{id}/score` | Score job against resume |
| POST | `/api/jobs/{id}/confirm-apply` | Mark job for application |
| POST | `/api/jobs/{id}/skip` | Skip job |

**Create Job Example:**
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "Google",
    "title": "Senior Backend Engineer",
    "jdText": "Job description here..."
  }'
```

**Score Job Example:**
```bash
curl -X POST http://localhost:8080/api/jobs/1/score \
  -H "Content-Type: application/json" \
  -d '{
    "resume": "Your resume text here..."
  }'
```

### Interview Preparation

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/readiness` | Get global + per-company readiness |
| GET | `/api/readiness/{company}` | Get company-specific readiness |
| POST | `/api/readiness/{company}` | Update readiness scores |

**Update Readiness Example:**
```bash
curl -X POST http://localhost:8080/api/readiness/Google \
  -H "Content-Type: application/json" \
  -d '{
    "dsa": 75,
    "system_design": 80,
    "behavioral": 85
  }'
```

### Applications Pipeline

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/applications` | List all applications |
| GET | `/api/applications/{id}` | Get application details |
| POST | `/api/applications/{id}/update-status` | Update application status |

### Apply Engine

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/apply/{jobId}` | Run ATS auto-apply for a confirmed job |
| POST | `/api/apply/batch` | Run auto-apply across all eligible jobs |
| GET | `/api/apply/status` | Get apply engine status |
| POST | `/api/apply/toggle` | Enable/disable auto-apply |

ATS strategies implemented: **Greenhouse, Lever, Workday**, plus a generic `CustomApplier` fallback.

### Interview Intel

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/intel/scrape-all` | Trigger intel scrape across target companies |
| GET | `/api/intel/{company}` | Get scraped intel for a company |
| GET | `/api/intel/aggregate` | Get aggregated topic universe |
| POST | `/api/intel/brief/{company}` | Generate a company prep brief |
| GET | `/api/intel/brief/{company}` | Fetch a generated company prep brief |

### Resume

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/resume/status` | Check whether master resume is loaded |
| POST | `/api/resume/upload` | Upload/parse resume (DOCX → JSON) |

### Configuration

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/config` | Get full config (filters, targets, LLM provider) |
| GET/POST | `/api/config/apply-engine` | Get/update auto-apply settings |
| POST | `/api/config/filters` | Update job filter rules |
| POST | `/api/config/targets` | Update target companies |
| GET/POST | `/api/config/email-preferences` | Get/update email notification preferences |
| GET/POST | `/api/config/target-companies` | Get/update target companies (settings UI) |
| GET/POST | `/api/config/job-filters` | Get/update job filters (settings UI) |

---

## 🔧 Configuration

### application.yml

Key settings in `src/main/resources/application.yml`:

```yaml
llm:
  provider: claude
  claude:
    api-key: ${CLAUDE_API_KEY}
    model: claude-sonnet-4-6  # or claude-haiku-4-5, claude-opus-4-7

gmail:
  enabled: true  # Email notifications + inbox tracking active
  service-account-key-path: ${GMAIL_OAUTH_CREDENTIALS}
  user-email: ${GMAIL_USER_EMAIL}
```

### Gmail Setup (required — enabled by default)

1. Set up Gmail API service account / OAuth2 credentials
2. Download the credentials JSON
3. Set in `.env`:
   ```bash
   export GMAIL_OAUTH_CREDENTIALS=/path/to/credentials.json
   export GMAIL_USER_EMAIL=your-email@gmail.com
   ```
4. To disable, set `gmail.enabled: false` in `application.yml`

---

## 📊 Performance & Costs

### LLM Model Comparison

For 500 job scores/month:

| Model | Score Accuracy | Cost/Month | Notes |
|-------|---|---|---|
| Haiku 4.5 | 75/100 | $4.24 | Fast, economical |
| **Sonnet 4.6** | **97/100** | **$12.71** | ✅ **Best balance** |
| Opus 4.7 | 92/100 | $21.19 | Premium, detailed |

**Current Configuration**: Claude Sonnet 4.6 (optimal cost-quality balance)

---

## ⏰ Scheduled Jobs

| Schedule | Job | Class |
|----------|-----|-------|
| Daily 8:00 AM | Poll target companies for new jobs | `JobPollerService` |
| Every 5 min | Scan Gmail inbox, classify replies/status emails | `GmailTrackerService` |
| Weekly (Sun midnight) | Scrape company interview intel | `IntelScheduler` |
| Weekly (Mon 9:00 AM) | Recalculate + email readiness report | `ReadinessScheduler` |

---

## 🗂️ Database Schema

### Core Tables

| Table | Purpose |
|-------|---------|
| `job` | Job postings with fit scores |
| `application` | Job applications submitted |
| `prep_readiness` | Interview prep scores by company |
| `resume_master` | Resume versions and adaptations |
| `topic_universe` | Interview topics (DSA, System Design, etc.) |
| `topic_coverage` | Topic coverage tracking |
| `company_intel` | Company research and insights |
| `company_topic_frequency` | Topic frequency by company |

---

## ✅ What's Working

- ✅ Job creation, storage, and ATS polling (Greenhouse/Lever/Workday) via `JobPollerService`
- ✅ Resume-job fit scoring via Claude AI (`FitScorerService`)
- ✅ Resume parsing (DOCX → JSON via Apache POI) and Claude-driven tailoring (`ResumeParserService`, `ResumeAdapterService`)
- ✅ Interview prep readiness tracking (DSA, System Design, Behavioral) with weekly report scheduler
- ✅ Topic coverage tracking (`TopicCoverageService`)
- ✅ Job confirmation/skip workflow
- ✅ **Apply Engine** — automated ATS submission with Greenhouse/Lever/Workday/custom strategies, gated by human confirmation
- ✅ **Gmail integration** — inbox scanning every 5 min, reply detection, application status classification (`GmailTrackerService`)
- ✅ **Interview intel scraping** — weekly company intel scrape + on-demand company briefs (`IntelScrapeService`, `BriefService`)
- ✅ **React-style dashboard UI** — Jobs, Readiness, Topics, Pipeline, and Settings (resume upload, target companies, job filters, email preferences, auto-apply toggle)
- ✅ PostgreSQL persistence
- ✅ Full REST API (jobs, applications, apply, config, intel, resume, readiness, topics)
- ✅ Claude Sonnet 4.6 integration (optimal cost/performance)

---

## 🚧 What's Missing / Not Yet Built

- [ ] LinkedIn / Indeed job scrapers (currently Greenhouse, Lever, Workday only)
- [ ] Job deduplication across sources
- [ ] Cover letter generation
- [ ] AI mock interviews
- [ ] Performance analytics / offer comparison / salary negotiation guidance
- [ ] Career path recommendations, peer benchmarking

---

## 🐛 Testing

### Run Tests
```bash
source ~/.hireloop/.env
mvn test
```

### Manual Testing (End-to-End)

```bash
# Create a job
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "TechCorp",
    "title": "Backend Engineer",
    "jdText": "We are looking for a backend engineer..."
  }'

# Score your resume
curl -X POST http://localhost:8080/api/jobs/1/score \
  -H "Content-Type: application/json" \
  -d '{
    "resume": "Your resume content here..."
  }'

# Check score
curl http://localhost:8080/api/jobs/1
```

---

## 📝 Current Status

- **Version**: 1.0.0
- **Last Updated**: July 24, 2026
- **Status**: Core pipeline + Apply Engine + Gmail tracking + Interview Intel + Dashboard UI implemented; job source coverage limited to Greenhouse/Lever/Workday
- **LLM Model**: Claude Sonnet 4.6
- **Database**: PostgreSQL with Hibernate ORM

---

## 🔐 Security Notes

- Store `CLAUDE_API_KEY` in environment variables, never commit to git
- Database credentials in `~/.hireloop/.env` (not committed)
- Email credentials use service account JSON (not committed)
- Add `.env` and `*.key` to `.gitignore`

---

## 📚 Tech Stack

- **Backend**: Spring Boot 3.3.0, Java 17
- **Database**: PostgreSQL 15 with JPA/Hibernate
- **AI/LLM**: Claude API (Sonnet 4.6)
- **Build**: Maven 3.9+
- **Architecture**: REST API, Service-Repository pattern

---

## 🤝 Contributing

When adding features:
1. Update API endpoints in controllers
2. Add corresponding services for business logic
3. Update this README with new endpoints
4. Test with CLI or curl
5. Commit changes with meaningful messages

---

## 📞 Support

For issues:
1. Check application logs: `tail -f /tmp/hireloop.log`
2. Verify database connection: `psql -h localhost -U hireloop_user -d hireloop`
3. Confirm Claude API key is valid and set
4. Check if port 8080 is available

---

## 📄 License

Private project for interview preparation automation.
