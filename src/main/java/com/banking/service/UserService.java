package com.banking.service;

public interface UserService {
    
    /**
     * Get user email by account number
     * @param accountNumber the account number
     * @return the email address of the account owner
     * @throws com.banking.exception.NotFoundException if account or user not found
     */
    String getUserEmail(String accountNumber);
}
