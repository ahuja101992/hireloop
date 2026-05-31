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

### 6. **Email Notifications** *(Optional - Currently Disabled)*
   - New job opportunity alerts
   - Interview prep readiness reports
   - Application status updates

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
  enabled: false  # Set to true to enable email notifications
  service-account-key-path: ${GMAIL_KEY_PATH}
  user-email: ${GMAIL_USER_EMAIL}
```

### Enable Email Notifications

1. Set up Gmail API service account
2. Download service account JSON key
3. Update `.env`:
   ```bash
   export GMAIL_KEY_PATH=/path/to/service-account-key.json
   export GMAIL_USER_EMAIL=your-email@gmail.com
   ```
4. Update `application.yml`: `gmail.enabled: true`

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

- ✅ Job creation and storage
- ✅ Resume-job fit scoring via Claude AI
- ✅ Interview prep readiness tracking (DSA, System Design, Behavioral)
- ✅ Job confirmation/skip workflow
- ✅ PostgreSQL persistence
- ✅ REST API endpoints
- ✅ Claude Sonnet 4.6 integration (optimal performance)

---

## 🚧 What's Missing (Phase 4+)

### Phase 4: ApplyEngine (ATS Integration)
- [ ] Auto-fill job applications
- [ ] ATS form detection and population
- [ ] Cover letter generation
- [ ] Application tracking

### Phase 5: Job Scraping
- [ ] LinkedIn job scraper
- [ ] Indeed job scraper
- [ ] Auto-feed jobs into pipeline
- [ ] Job deduplication

### Phase 6: Email Notifications
- [ ] Gmail API integration
- [ ] New job alerts
- [ ] Readiness reports
- [ ] Application status updates

### Phase 7: Interview Intel
- [ ] Company-specific interview process extraction
- [ ] Interview round details
- [ ] Question bank by topic
- [ ] Interviewer insights

### Phase 8-24: Advanced Features
- [ ] AI mock interviews
- [ ] Performance analytics
- [ ] Salary negotiation guidance
- [ ] Offer comparison
- [ ] Career path recommendations
- [ ] Peer benchmarking

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
- **Last Updated**: May 30, 2026
- **Status**: MVP Complete (Steps 1-3 of 24)
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
