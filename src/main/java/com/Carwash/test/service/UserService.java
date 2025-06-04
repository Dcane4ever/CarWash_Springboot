package com.Carwash.test.service;

import com.Carwash.test.model.User;

public interface UserService {
    User findByUsername(String username);
} 