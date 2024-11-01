package bashkirov.store_original.service;

import bashkirov.store_original.dto.EmailDto;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender javaMailSender;

    public void sendEmail(EmailDto emailDto) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(emailDto.getEmail());
        message.setSubject(emailDto.getSubject());
        message.setText(emailDto.getText());

        javaMailSender.send(message);
    }

    @SneakyThrows
    public void sendActivationEmail(String email, String activationCode) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(
                mimeMessage, true, "UTF-8");
        mimeMessageHelper.setTo(email);
        mimeMessageHelper.setSubject("Account activation");

        String htmlContent =
                "<html>"
                        + "<body>"
                        + "<h1 style='color: green; text-align: center;'>Активація акаунта</h1>"
                        + "<a href='http://localhost:8080/activate/" + activationCode + "' style='display: inline-block; "
                        + " padding: 10px 20px; margin-top: 20px; color: white; background-color: #4CAF50; text-decoration: none;"
                        + " border-radius: 5px; text-align: center'>Активувати</a>"
                        + "</body>"
                        + "</html>";
        mimeMessageHelper.setText(htmlContent, true);

        javaMailSender.send(mimeMessage);
    }
}
