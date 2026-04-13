package org.example.e_fashion.service;

import jakarta.mail.MessagingException;

public interface EmailService {
    void sendVerifyAccountEmail(String to, String link) throws MessagingException;

    void sendResetPasswordEmail(String to, String link) throws MessagingException;
}
