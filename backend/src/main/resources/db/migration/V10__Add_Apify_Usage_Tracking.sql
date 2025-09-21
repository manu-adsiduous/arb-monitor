-- Create table for tracking Apify API usage and costs
CREATE TABLE IF NOT EXISTS apify_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_id VARCHAR(255) NOT NULL,
    run_id VARCHAR(255),
    request_type VARCHAR(100) NOT NULL,
    compute_units DECIMAL(10, 4),
    estimated_cost DECIMAL(10, 6),
    user_id BIGINT,
    domain_name VARCHAR(255),
    ads_scraped INTEGER,
    request_duration_ms BIGINT,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    data_size_mb DECIMAL(10, 2),
    memory_mb INTEGER,
    cpu_seconds DECIMAL(10, 2),
    INDEX idx_user_timestamp (user_id, request_timestamp),
    INDEX idx_domain_name (domain_name),
    INDEX idx_request_type (request_type),
    INDEX idx_actor_id (actor_id),
    INDEX idx_run_id (run_id)
);
