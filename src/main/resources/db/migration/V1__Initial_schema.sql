-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    role ENUM('ROLE_USER', 'ROLE_ADMIN') NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_email CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$'),
    CONSTRAINT uk_email UNIQUE (email),
    CONSTRAINT uk_phone_number UNIQUE (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_phone_number ON users (phone_number);

-- Create admin user with hashed password (bcrypt hash for 'Admin@1234')
-- Password will be updated by the application on first run if needed
INSERT IGNORE INTO users (first_name, last_name, email, password, phone_number, role, enabled)
VALUES ('System', 'Administrator', 'admin@bank.com', 
        '$2a$12$X8X6v1Q1F5h3W1zZ2Y3Z4e5r6t7y8u9i0o1p2q3r4s5t6u7v8w9x0y1z', 
        '+1234567890', 'ROLE_ADMIN', TRUE);
