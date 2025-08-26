-- Demo data for testing Meta API integration
-- Insert demo user
INSERT INTO users (id, email, name, subscription_status, cognito_sub, created_at, updated_at) 
VALUES (1, 'demo@example.com', 'Demo User', 'ACTIVE', 'demo-123', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert demo domain (bestoptions.net)
INSERT INTO domains (id, domain_name, user_id, status, share_token, created_at, updated_at) 
VALUES (2, 'bestoptions.net', 1, 'ACTIVE', 'c8064dc4-fbfd-4521-bb7e-586facae1073', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert another demo domain
INSERT INTO domains (id, domain_name, user_id, status, share_token, created_at, updated_at) 
VALUES (3, 'example.com', 1, 'ACTIVE', 'a1b2c3d4-e5f6-7890-abcd-123456789012', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);




