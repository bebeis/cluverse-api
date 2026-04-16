package cluverse.member.client;

import cluverse.member.config.StudentVerificationMailProperties;
import cluverse.member.exception.MemberExceptionMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MailStudentVerificationEmailClient implements StudentVerificationEmailClient {

    private final ObjectProvider<MailSender> mailSenderProvider;
    private final StudentVerificationMailProperties mailProperties;

    @Override
    public void sendVerificationCode(String email, String code, LocalDateTime expiresAt) {
        MailSender mailSender = getRequiredMailSender();
        try {
            mailSender.send(createMessage(email, code, expiresAt));
        } catch (MailException e) {
            throw new IllegalStateException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_MAIL_SEND_FAILED.getMessage(),
                    e
            );
        }
    }

    private MailSender getRequiredMailSender() {
        MailSender mailSender = mailSenderProvider.getIfAvailable();
        validateMailSenderConfigured(mailSender);
        return mailSender;
    }

    private void validateMailSenderConfigured(MailSender mailSender) {
        if (mailSender == null) {
            throw new IllegalStateException(
                    MemberExceptionMessage.STUDENT_VERIFICATION_MAIL_SENDER_NOT_CONFIGURED.getMessage()
            );
        }
    }

    private SimpleMailMessage createMessage(String email, String code, LocalDateTime expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.fromAddress());
        message.setTo(email);
        message.setSubject(mailProperties.subjectText());
        message.setText(mailProperties.bodyText(code, expiresAt));
        return message;
    }
}
