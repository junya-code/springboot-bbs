package com.example.bbs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.bbs.dto.ContactForm;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String adminAddress;

    public EmailService(JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromAddress,
            // 必要に応じて adminAddress を独立した設定に分離する
            @Value("${spring.mail.username}") String adminAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.adminAddress = adminAddress;
    }

    public void sendEmail(String to, String subject, String text) {

        log.info("Sending email. to={}, subject={}", to, subject);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom(fromAddress);

            mailSender.send(message);

            log.info("Email sent successfully. to={}", to);

        } catch (MailException e) {
            log.error("Email sending failed. to={}, subject={}", to, subject, e);
            throw e;
        }
    }

    public void sendUserEmail(ContactForm form) {
        String subject = "お問い合わせありがとうございます。";
        String text = String.format(
                "お問い合わせありがとうございます。\n以下の内容で受け付けました。\n\n[お名前]: %s\n[お問い合わせ内容]:\n%s",
                form.getName(), form.getMessage());
        sendEmail(form.getEmail(), subject, text);
    }

    public void sendAdminEmail(ContactForm form) {
        String subject = "新しいお問い合わせが届きました。";
        String text = String.format(
                "新しいお問い合わせが届きました\n\n[お名前]: %s\n[メールアドレス]: %s\n[お問い合わせ内容]: \n%s",
                form.getName(), form.getEmail(), form.getMessage());
        sendEmail(adminAddress, subject, text);
    }

}
