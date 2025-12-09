-- Create account types enum
-- Note: MySQL doesn't support CREATE TYPE, so we'll use ENUM directly in the table definition

-- Create accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    account_type ENUM('CHECKING', 'SAVINGS', 'BUSINESS', 'LOAN', 'CREDIT_CARD') NOT NULL,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status ENUM('ACTIVE', 'INACTIVE', 'FROZEN', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_account_number UNIQUE (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_reference VARCHAR(36) NOT NULL,
    from_account_id BIGINT,
    to_account_id BIGINT,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    transaction_type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'PAYMENT', 'REFUND') NOT NULL,
    status ENUM('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    description TEXT,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_from_account FOREIGN KEY (from_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_transaction_to_account FOREIGN KEY (to_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT uk_transaction_reference UNIQUE (transaction_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for better query performance
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_transactions_from_account ON transactions(from_account_id);
CREATE INDEX idx_transactions_to_account ON transactions(to_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

-- Create a function to generate account numbers
DELIMITER //
CREATE FUNCTION generate_account_number(account_type VARCHAR(20)) 
RETURNS VARCHAR(20)
DETERMINISTIC
BEGIN
    DECLARE prefix VARCHAR(3);
    DECLARE random_part VARCHAR(12);
    DECLARE checksum INT;
    DECLARE i INT DEFAULT 1;
    
    -- Set prefix based on account type
    SET prefix = 
        CASE 
            WHEN account_type = 'CHECKING' THEN 'CHK'
            WHEN account_type = 'SAVINGS' THEN 'SAV'
            WHEN account_type = 'BUSINESS' THEN 'BUS'
            WHEN account_type = 'LOAN' THEN 'LON'
            WHEN account_type = 'CREDIT_CARD' THEN 'CRC'
            ELSE 'ACC'
        END;
    
    -- Generate random 12-digit number
    SET random_part = LPAD(FLOOR(RAND() * 1000000000000), 12, '0');
    
    -- Simple checksum calculation (Luhn algorithm)
    SET @sum = 0;
    SET @double = 0;
    SET i = LENGTH(random_part);
    
    WHILE i > 0 DO
        SET @digit = CAST(SUBSTRING(random_part, i, 1) AS UNSIGNED);
        IF @double = 1 THEN
            SET @digit = @digit * 2;
            IF @digit > 9 THEN
                SET @digit = @digit - 9;
            END IF;
        END IF;
        SET @sum = @sum + @digit;
        SET @double = 1 - @double;
        SET i = i - 1;
    END WHILE;
    
    SET checksum = (10 - (@sum % 10)) % 10;
    
    RETURN CONCAT(prefix, random_part, checksum);
END //
DELIMITER ;

-- Create a procedure to create a new account
DELIMITER //
CREATE PROCEDURE create_account(
    IN p_user_id BIGINT,
    IN p_account_type VARCHAR(20),
    IN p_initial_balance DECIMAL(19, 4),
    IN p_currency VARCHAR(3)
)
BEGIN
    DECLARE v_account_number VARCHAR(20);
    DECLARE v_account_id BIGINT;
    
    -- Generate a unique account number
    SET v_account_number = generate_account_number(p_account_type);
    
    -- Insert the new account
    INSERT INTO accounts (account_number, user_id, account_type, balance, currency, status)
    VALUES (v_account_number, p_user_id, p_account_type, p_initial_balance, p_currency, 'ACTIVE');
    
    -- Get the new account ID
    SET v_account_id = LAST_INSERT_ID();
    
    -- If initial balance is greater than 0, create a deposit transaction
    IF p_initial_balance > 0 THEN
        INSERT INTO transactions (
            transaction_reference,
            to_account_id,
            amount,
            currency,
            transaction_type,
            status,
            description
        ) VALUES (
            UUID(),
            v_account_id,
            p_initial_balance,
            p_currency,
            'DEPOSIT',
            'COMPLETED',
            CONCAT('Initial deposit for account ', v_account_number)
        );
    END IF;
    
    -- Return the new account number
    SELECT v_account_number AS account_number, v_account_id AS account_id;
END //
DELIMITER ;
