package com.Carwash.test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    public void sendVerificationEmail(String to, String token, String baseUrl) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setFrom("your-email@your-domain.com");
        helper.setTo(to);
        helper.setSubject("Complete Your Registration");
        
        String content = """
            <html>
                <body>
                    <h2>Verify Your Email Address</h2>
                    <p>Thank you for registering with our Car Wash Service. Please click the link below to complete your registration:</p>
                    <a href=\"%s/user/verify?token=%s\">Verify Email Address</a>
                    <p>This link will expire in 24 hours.</p>
                </body>
            </html>
        """.formatted(baseUrl, token);
        
        helper.setText(content, true);
        mailSender.send(message);
    }
    
    public void sendPasswordResetEmail(String to, String token, String baseUrl) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setFrom("your-email@your-domain.com");
        helper.setTo(to);
        helper.setSubject("Password Reset Request");
        
        String content = """
            <html>
                <body>
                    <h2>Reset Your Password</h2>
                    <p>We received a request to reset your password for your Car Wash Service account. Click the link below to create a new password:</p>
                    <a href=\"%s/user/reset-password?token=%s\">Reset Password</a>
                    <p>This link will expire in 1 hour. If you did not request a password reset, please ignore this email.</p>
                </body>
            </html>
        """.formatted(baseUrl, token);
        
        helper.setText(content, true);
        mailSender.send(message);
    }
}


