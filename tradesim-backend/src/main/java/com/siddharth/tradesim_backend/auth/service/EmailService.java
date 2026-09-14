package com.siddharth.tradesim_backend.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String to, String otp, String subject) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            try {
                helper.setFrom(fromEmail, "TradeSim");
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(fromEmail);
            }

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildHtmlTemplate(otp), true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email");
        }
    }

    private String buildHtmlTemplate(String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="color-scheme" content="light dark">
                <meta name="supported-color-schemes" content="light dark">
                <style>
                  body { font-family: system-ui, -apple-system, sans-serif; background-color: #f3f4f6; color: #111827; margin: 0; padding: 40px; }
                  .container { max-width: 500px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 16px; border: 1px solid #e5e7eb; text-align: center; }
                  .header { margin-bottom: 25px; }
                  .logo { display: inline-block; }
                  .title { font-size: 24px; font-weight: 700; margin: 10px 0 5px 0; color: #111827; }
                  .subtitle { font-size: 14px; color: #6b7280; margin-bottom: 25px; }
                  .otp-box { background-color: #f8fafc; border: 1px solid #e2e8f0; padding: 15px; border-radius: 8px; font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #2563eb; margin: 25px 0; }
                  .warning { font-size: 13px; color: #6b7280; margin-bottom: 30px; line-height: 1.5; }
                  .footer { border-top: 1px solid #e5e7eb; padding-top: 20px; font-size: 12px; color: #9ca3af; }
                  .accent { color: #2563eb; }
                  p { color: #374151; }
                </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <div class="logo">
                        <svg viewBox="0 0 100 100" width="60" height="60" xmlns="[http://www.w3.org/2000/svg](http://www.w3.org/2000/svg)">
                            <path d="M 10 20 H 90 L 70 40 H 50 V 60 L 30 80 V 40 H 10 Z" fill="#111827" />
                            <path d="M 38 80 L 52 66 V 80 Z" fill="#2563eb" />
                            <path d="M 57 80 V 61 L 71 47 V 80 Z" fill="#2563eb" />
                            <path d="M 76 80 V 42 L 90 28 V 80 Z" fill="#2563eb" />
                        </svg>
                      </div>
                      <div class="title">Trade<span class="accent">Sim</span></div>
                    </div>
                    <div class="subtitle">Secure Verification Request</div>
                    <p>Your one-time password (OTP) is:</p>
                    <div class="otp-box">%s</div>
                    <p class="warning">This code will expire in 10 minutes. Please do not share this code with anyone. If you did not request this, you can safely ignore this email.</p>
                    <div class="footer">
                      © 2026 TradeSim - Your Market Sandbox. All rights reserved.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(otp);
    }
}