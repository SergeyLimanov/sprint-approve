-- Migration script for adding Security to Sprint Approve
-- Run this on team-service database

-- Add password column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Set default password for existing users
-- Password: "changeme" (BCrypt hashed)
UPDATE users 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE password IS NULL OR password = '';

-- Make password column NOT NULL after setting defaults
ALTER TABLE users ALTER COLUMN password SET NOT NULL;

-- Verify migration
SELECT id, email, name, role, 
       CASE 
           WHEN password IS NOT NULL THEN 'Password set' 
           ELSE 'No password' 
       END as password_status
FROM users;
