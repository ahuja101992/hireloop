CREATE TABLE IF NOT EXISTS company_intel (
    id SERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    source VARCHAR(100),
    raw_data TEXT,
    interview_rounds TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_name, source)
);

CREATE TABLE IF NOT EXISTS topic_universe (
    id SERIAL PRIMARY KEY,
    category VARCHAR(100) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    global_frequency NUMERIC(5, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(category, topic)
);

CREATE TABLE IF NOT EXISTS company_topic_frequency (
    id SERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    topic_id INTEGER NOT NULL REFERENCES topic_universe(id) ON DELETE CASCADE,
    frequency NUMERIC(5, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_name, topic_id)
);

CREATE TABLE IF NOT EXISTS topic_coverage (
    id SERIAL PRIMARY KEY,
    topic_id INTEGER NOT NULL UNIQUE REFERENCES topic_universe(id) ON DELETE CASCADE,
    status VARCHAR(50) DEFAULT 'NOT_STARTED',
    notes TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS prep_readiness (
    id SERIAL PRIMARY KEY,
    company_name VARCHAR(255),
    dsa_score NUMERIC(5, 2) DEFAULT 0,
    system_design_score NUMERIC(5, 2) DEFAULT 0,
    behavioral_score NUMERIC(5, 2) DEFAULT 0,
    overall_score NUMERIC(5, 2) DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_name)
);

CREATE TABLE IF NOT EXISTS job (
    id SERIAL PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    jd_url VARCHAR(500),
    jd_text TEXT,
    fit_score NUMERIC(5, 2),
    status VARCHAR(50) DEFAULT 'PENDING',
    confirmed BOOLEAN DEFAULT FALSE,
    tailored_resume_json TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS application (
    id SERIAL PRIMARY KEY,
    job_id INTEGER NOT NULL REFERENCES job(id) ON DELETE CASCADE,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    pipeline_status VARCHAR(50) DEFAULT 'APPLIED',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resume_master (
    id SERIAL PRIMARY KEY,
    user_id INTEGER,
    resume_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_job_company ON job(company_name);
CREATE INDEX idx_job_status ON job(status);
CREATE INDEX idx_application_job ON application(job_id);
CREATE INDEX idx_application_status ON application(pipeline_status);
