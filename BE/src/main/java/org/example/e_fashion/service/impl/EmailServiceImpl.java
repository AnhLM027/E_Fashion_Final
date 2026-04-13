package org.example.e_fashion.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.example.e_fashion.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendVerifyAccountEmail(String to, String link) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Kích hoạt tài khoản");

        String html = """
        <div style="font-family:Arial;padding:30px">
            <h2>Kích hoạt tài khoản</h2>

            <p>Cảm ơn bạn đã đăng ký.</p>

            <p>Nhấn vào nút bên dưới để kích hoạt tài khoản:</p>

            <a href="%s"
               style="display:inline-block;
               padding:12px 24px;
               background:black;
               color:white;
               text-decoration:none;
               border-radius:6px">
               Kích hoạt tài khoản
            </a>

            <p style="margin-top:20px;color:gray">
                Link sẽ hết hạn sau 24 giờ
            </p>
        </div>
        """.formatted(link);

        helper.setText(html, true);

        mailSender.send(message);
    }

    @Override
    public void sendResetPasswordEmail(String to, String link) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Đặt lại mật khẩu");

        String html = """
        <div style="font-family: Arial, sans-serif; background:#f4f4f4; padding:40px;">
          <div style="max-width:500px; margin:auto; background:white; padding:30px; border-radius:8px; text-align:center;">
            
            <h2 style="color:#111;">Đặt lại mật khẩu</h2>
        
            <p style="color:#555; font-size:14px;">
              Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.
            </p>
        
            <p style="color:#555; font-size:14px;">
              Nhấn vào nút bên dưới để tạo mật khẩu mới:
            </p>
        
            <a href="%s"
               style="
                 display:inline-block;
                 margin-top:20px;
                 padding:12px 24px;
                 background:#000;
                 color:#fff;
                 text-decoration:none;
                 border-radius:6px;
                 font-weight:bold;
               ">
               Reset Password
            </a>
        
            <p style="margin-top:25px; font-size:12px; color:#888;">
              Link này sẽ hết hạn sau 15 phút.
            </p>
        
            <p style="font-size:12px; color:#aaa;">
              Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
            </p>
        
          </div>
        </div>
        """.formatted(link);

        helper.setText(html, true);

        mailSender.send(message);
    }
}
