package com.Carwash.test.service;

import com.Carwash.test.model.User;
import com.Carwash.test.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
} 