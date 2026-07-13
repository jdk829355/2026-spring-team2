package team2.goodsmap.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public String generateAuthCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(code);
    }

    public void sendAuthCode(String toEmail, String authCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[goodsmap] 이메일 인증번호");
        message.setText("인증번호: " + authCode + "\n\n인증번호는 2분간 유효합니다.");
        mailSender.send(message);
    }
}