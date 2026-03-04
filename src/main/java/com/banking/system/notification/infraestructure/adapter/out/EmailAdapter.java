package com.banking.system.notification.infraestructure.adapter.out;

import com.banking.system.notification.domain.exception.EmailDeliveryException;
import com.banking.system.notification.domain.model.EmailNotification;
import com.banking.system.notification.domain.port.out.EmailSenderPort;
import com.banking.system.notification.domain.port.out.TemplateRenderedPort;
import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAdapter implements EmailSenderPort {
    private final JavaMailSender javaMailSender;
    private final TemplateRenderedPort templateRenderedPort;

    @Value("${spring.mail.from-address:no-reply@localhost}")
    private String fromAddress;

    @Value("${spring.mail.from-name:Core Bank}")
    private String fromName;

    @Override
    public void sendEmail(EmailNotification notification) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(notification.to());
            helper.setSubject(notification.subject());

            String htmlContent = templateRenderedPort.render(
                    notification.type(),
                    notification.templateVariables()
            );

            helper.setText(htmlContent, true);
            javaMailSender.send(message);

            log.info("Email sent successfully to {}", notification.to());
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Error sending email to: {}", notification.to(), e);
            throw new EmailDeliveryException("Failed to send email to " + notification.to(), e);
        }
    }
}
