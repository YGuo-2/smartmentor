package com.tricia.smartmentor.service;

import com.tricia.smartmentor.common.BusinessException;
import com.tricia.smartmentor.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final RedisUtil redisUtil;

    private static final String CAPTCHA_PREFIX = "captcha:email:";
    private static final long CAPTCHA_TTL_MINUTES = 5;
    private static final long CAPTCHA_INTERVAL_SECONDS = 60; // 发送间隔

    /**
     * 发送邮箱验证码
     */
    public void sendCaptcha(String email) {
        // 检查发送频率（60秒内只能发一次）
        String intervalKey = CAPTCHA_PREFIX + "interval:" + email;
        try {
            if (Boolean.TRUE.equals(redisUtil.hasKey(intervalKey))) {
                throw new BusinessException(429, "验证码发送过于频繁，请60秒后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 检查发送频率失败，跳过频率限制: {}", e.getMessage());
        }

        // 生成6位数字验证码
        String code = generateCode();

        // 存入Redis，5分钟过期
        try {
            redisUtil.set(CAPTCHA_PREFIX + email, code, CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);
            redisUtil.set(intervalKey, "1", CAPTCHA_INTERVAL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis 存储验证码失败: {}", e.getMessage());
            throw new BusinessException(500, "验证码服务暂时不可用，请稍后再试");
        }

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl
                    ? ((org.springframework.mail.javamail.JavaMailSenderImpl) mailSender).getUsername()
                    : "noreply@smartmentor.com");
            message.setTo(email);
            message.setSubject("【SmartMentor 智学导师】邮箱验证码");
            message.setText("您好！\n\n"
                    + "您的验证码为：" + code + "\n\n"
                    + "验证码有效期为5分钟，请尽快使用。\n"
                    + "如果这不是您的操作，请忽略此邮件。\n\n"
                    + "—— SmartMentor 智学导师");
            mailSender.send(message);
            log.info("验证码邮件已发送至: {}", email);
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage());
            // 发送失败时清除Redis中的验证码
            try {
                redisUtil.delete(CAPTCHA_PREFIX + email);
                redisUtil.delete(intervalKey);
            } catch (Exception ignored) { }
            throw new BusinessException(500, "邮件发送失败，请检查邮箱地址是否正确");
        }
    }

    /**
     * 验证验证码
     */
    public boolean verifyCaptcha(String email, String code) {
        if (email == null || code == null || code.isBlank()) {
            return false;
        }
        try {
            Object cached = redisUtil.get(CAPTCHA_PREFIX + email);
            if (cached != null && code.equals(cached.toString())) {
                // 验证成功后删除验证码（一次性使用）
                redisUtil.delete(CAPTCHA_PREFIX + email);
                return true;
            }
        } catch (Exception e) {
            log.error("Redis 验证码校验失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
