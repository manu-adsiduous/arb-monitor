-- Migration V3: Restore original domains that were lost during schema recreation

-- Insert demo user if not exists
INSERT INTO users (id, email, name, subscription_status, cognito_sub, created_at, updated_at) 
SELECT 1, 'demo@example.com', 'Demo User', 'ACTIVE', 'demo-123', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);

-- Restore original domains
INSERT INTO domains (domain_name, user_id, status, processing_status, processing_message, share_token, created_at, updated_at)
SELECT 'furilia.com', 1, 'ACTIVE', 'PENDING', 'Domain restored from migration', 
       CONCAT('restored-', REPLACE(RANDOM_UUID(), '-', '')), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM domains WHERE domain_name = 'furilia.com');

INSERT INTO domains (domain_name, user_id, status, processing_status, processing_message, share_token, created_at, updated_at)
SELECT 'mywebanswers.com', 1, 'ACTIVE', 'PENDING', 'Domain restored from migration',
       CONCAT('restored-', REPLACE(RANDOM_UUID(), '-', '')), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM domains WHERE domain_name = 'mywebanswers.com');

INSERT INTO domains (domain_name, user_id, status, processing_status, processing_message, share_token, created_at, updated_at)
SELECT 'needtheinfo.com', 1, 'ACTIVE', 'PENDING', 'Domain restored from migration',
       CONCAT('restored-', REPLACE(RANDOM_UUID(), '-', '')), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM domains WHERE domain_name = 'needtheinfo.com');

INSERT INTO domains (domain_name, user_id, status, processing_status, processing_message, share_token, created_at, updated_at)
SELECT 'livelocally.net', 1, 'ACTIVE', 'PENDING', 'Domain restored from migration',
       CONCAT('restored-', REPLACE(RANDOM_UUID(), '-', '')), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM domains WHERE domain_name = 'livelocally.net');

