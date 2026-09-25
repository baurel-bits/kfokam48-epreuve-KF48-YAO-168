package com.example.app.infrastructure.email;

public interface EmailService {

    void sendSimpleEmail(String to, String subject, String content);

    void sendHtmlEmail(String to, String subject, String htmlContent);
}
