package com.bjtu.review.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class VerifyCodeService {

    private static final long CODE_TTL_SECONDS = 300; // 5 minutes
    private static final long RESEND_INTERVAL_SECONDS = 60; // 1 minute
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    private final Map<String, CodeEntry> codeCache = new ConcurrentHashMap<>();

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public VerifyCodeService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendCode(String email) {
        CodeEntry existing = codeCache.get(email);
        if (existing != null && !existing.isExpired()) {
            long sinceLast = java.time.Duration.between(existing.sendTime, LocalDateTime.now()).getSeconds();
            if (sinceLast < RESEND_INTERVAL_SECONDS) {
                throw new RuntimeException("验证码已发送，请稍后再试");
            }
        }

        String code = generateCode();
        codeCache.put(email, new CodeEntry(code, LocalDateTime.now()));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom.contains("your-email") ? "noreply@bjtu.edu.cn" : mailFrom);
            helper.setTo(email);
            helper.setSubject("BJTU课程评价系统 - 邮箱验证码");
            helper.setText("<h3>您的邮箱验证码</h3><p style=\"font-size:24px;font-weight:bold;color:#1a73e8;\">" + code + "</p>" +
                    "<p>验证码有效期为5分钟，请勿泄露给他人。</p><p>如果这不是您本人的操作，请忽略此邮件。</p>", true);
            mailSender.send(message);
            log.info("验证码已发送至 {}: {}", email, code);
        } catch (Exception e) {
            log.warn("邮件发送失败（验证码 {} -> {}）：{}", email, code, e.getMessage());
            log.info("提示：配置 application.yml 中 spring.mail 的 host/username/password 即可启用真实邮件发送");
            // 发送失败仍保留缓存中的 code，仅记录日志
        }
    }

    public void verifyCode(String email, String code) {
        CodeEntry entry = codeCache.get(email);
        if (entry == null) {
            throw new RuntimeException("请先获取验证码");
        }
        if (entry.isExpired()) {
            codeCache.remove(email);
            throw new RuntimeException("验证码已过期，请重新获取");
        }
        if (!entry.code.equals(code)) {
            throw new RuntimeException("验证码错误");
        }
        codeCache.remove(email);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private static class CodeEntry {
        final String code;
        final LocalDateTime sendTime;

        CodeEntry(String code, LocalDateTime sendTime) {
            this.code = code;
            this.sendTime = sendTime;
        }

        boolean isExpired() {
            return java.time.Duration.between(sendTime, LocalDateTime.now()).getSeconds() >= CODE_TTL_SECONDS;
        }
    }
}
