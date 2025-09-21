-- Create table for tracking OpenAI API usage and costs
CREATE TABLE IF NOT EXISTS openai_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    model_name VARCHAR(100) NOT NULL,
    request_type VARCHAR(100) NOT NULL,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    estimated_cost DECIMAL(10, 6),
    user_id BIGINT,
    domain_name VARCHAR(255),
    meta_ad_id VARCHAR(255),
    request_duration_ms BIGINT,
    success BOOLEAN DEFAULT TRUE,
    error_message TEXT,
    response_size_chars INTEGER,
    INDEX idx_user_timestamp (user_id, request_timestamp),
    INDEX idx_domain_name (domain_name),
    INDEX idx_request_type (request_type),
    INDEX idx_model_name (model_name)
);
