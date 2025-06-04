package com.Carwash.test.util;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SessionManager {
    private static final String USER_SESSION_KEY = "USER_SESSION";
    private static final String ADMIN_SESSION_KEY = "ADMIN_SESSION";
    
    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession(true);
    }
    
    public void setUserSession(String username) {
        getSession().setAttribute(USER_SESSION_KEY, username);
    }
    
    public String getLoggedInUser() {
        return (String) getSession().getAttribute(USER_SESSION_KEY);
    }
    
    public boolean isLoggedIn() {
        return getLoggedInUser() != null;
    }
    
    public void logout() {
        getSession().removeAttribute(USER_SESSION_KEY);
    }
    
    // Admin session methods
    public void setAdminSession() {
        getSession().setAttribute(ADMIN_SESSION_KEY, true);
    }
    
    public boolean isAdminLoggedIn() {
        return getSession().getAttribute(ADMIN_SESSION_KEY) != null;
    }
    
    public void logoutAdmin() {
        getSession().removeAttribute(ADMIN_SESSION_KEY);
    }
} 