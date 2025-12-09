package com.banking.service.impl;

import com.banking.exception.NotFoundException;
import com.banking.model.Account;
import com.banking.model.User;
import com.banking.repository.AccountRepository;
import com.banking.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public String getUserEmail(String accountNumber) {
        log.debug("Fetching user email for account: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new NotFoundException("Account not found with number: " + accountNumber));
            
        User user = account.getUser();
        if (user == null) {
            throw new NotFoundException("User not found for account: " + accountNumber);
        }
        
        return user.getEmail();
    }
}
