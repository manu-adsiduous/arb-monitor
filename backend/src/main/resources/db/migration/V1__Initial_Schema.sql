-- Initial schema migration
-- This file represents the baseline schema

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    subscription_status VARCHAR(50) DEFAULT 'ACTIVE',
    cognito_sub VARCHAR(255),
    meta_api_key VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Domains table
CREATE TABLE IF NOT EXISTS domains (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain_name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    compliance_score DOUBLE,
    last_checked TIMESTAMP,
    share_token VARCHAR(255) UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    processing_message TEXT,
    rac_parameter VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'PAUSED', 'INACTIVE')),
    CONSTRAINT chk_processing_status CHECK (processing_status IN ('PENDING', 'FETCHING_ADS', 'PAUSED', 'SCANNING_COMPLIANCE', 'COMPLETED', 'FAILED'))
);

-- Scraped ads table
CREATE TABLE IF NOT EXISTS scraped_ads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meta_ad_id VARCHAR(255) NOT NULL UNIQUE,
    domain_name VARCHAR(255) NOT NULL,
    headline TEXT,
    primary_text TEXT,
    description TEXT,
    call_to_action VARCHAR(255),
    display_url VARCHAR(500),
    landing_page_url VARCHAR(1000),
    page_name VARCHAR(255),
    page_id VARCHAR(255),
    funding_entity VARCHAR(255),
    ad_creation_date TIMESTAMP,
    ad_delivery_start_date TIMESTAMP,
    ad_delivery_stop_date TIMESTAMP,
    last_updated TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    impressions_range_lower BIGINT,
    impressions_range_upper BIGINT,
    spend_range_lower INTEGER,
    spend_range_upper INTEGER,
    ad_format VARCHAR(100),
    target_demographics TEXT,
    target_locations TEXT,
    raw_data TEXT,
    scrape_source VARCHAR(50) DEFAULT 'APIFY',
    scraped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ad analyses table
CREATE TABLE IF NOT EXISTS ad_analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meta_ad_id VARCHAR(255) NOT NULL,
    domain_id BIGINT NOT NULL,
    headline TEXT,
    primary_text TEXT,
    image_url VARCHAR(1000),
    compliance_score DOUBLE,
    compliance_status VARCHAR(50),
    analysis_notes TEXT,
    ad_creative_compliant BOOLEAN,
    ad_creative_reason TEXT,
    landing_page_relevant BOOLEAN,
    landing_page_reason TEXT,
    rac_relevant BOOLEAN,
    rac_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (domain_id) REFERENCES domains(id) ON DELETE CASCADE,
    CONSTRAINT chk_compliance_status CHECK (compliance_status IN ('COMPLIANT', 'WARNING', 'CRITICAL'))
);

-- Compliance violations table
CREATE TABLE IF NOT EXISTS compliance_violations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_id BIGINT NOT NULL,
    violation_type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (analysis_id) REFERENCES ad_analyses(id) ON DELETE CASCADE,
    CONSTRAINT chk_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Compliance rules table
CREATE TABLE IF NOT EXISTS compliance_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(100) NOT NULL,
    description TEXT,
    pattern VARCHAR(1000),
    severity VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_rule_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_domains_user_id ON domains(user_id);
CREATE INDEX IF NOT EXISTS idx_scraped_ads_domain_name ON scraped_ads(domain_name);
CREATE INDEX IF NOT EXISTS idx_scraped_ads_meta_ad_id ON scraped_ads(meta_ad_id);
CREATE INDEX IF NOT EXISTS idx_ad_analyses_domain_id ON ad_analyses(domain_id);
CREATE INDEX IF NOT EXISTS idx_ad_analyses_meta_ad_id ON ad_analyses(meta_ad_id);
CREATE INDEX IF NOT EXISTS idx_compliance_violations_analysis_id ON compliance_violations(analysis_id);

