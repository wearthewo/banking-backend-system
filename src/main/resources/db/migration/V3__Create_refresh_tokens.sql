-- Create refresh tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_token VARCHAR(255) NULL,
    revoked_at TIMESTAMP NULL,
    created_by_ip VARCHAR(45) NULL,
    revoked_by_ip VARCHAR(45) NULL,
    reason_revoked VARCHAR(255) NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_refresh_token_token UNIQUE (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for better query performance
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Create a procedure to revoke all refresh tokens for a user (except the current one if provided)
DELIMITER //
CREATE PROCEDURE revoke_user_refresh_tokens(
    IN p_user_id BIGINT,
    IN p_except_token VARCHAR(255)
)
BEGIN
    UPDATE refresh_tokens
    SET 
        revoked = TRUE,
        revoked_at = CURRENT_TIMESTAMP,
        reason_revoked = 'Revoked by system'
    WHERE user_id = p_user_id
    AND (p_except_token IS NULL OR token != p_except_token)
    AND revoked = FALSE;
END //
DELIMITER ;
