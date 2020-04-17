package net.nhs.ems.emailadapter.service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import net.nhs.ems.emailadapter.model.EmailSettings;

public class OutgoingEmailBuilder {

  public MimeMessage buildEmail(EmailSettings settings, String fileName, byte[] pdfData)
      throws MessagingException {

    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage message = new MimeMessage(session);

    message.setSubject(settings.getSubject(), "UTF-8");
    message.setFrom(new InternetAddress(settings.getSender()));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(settings.getRecipient()));

    MimeBodyPart alternativePart = new MimeBodyPart();
    alternativePart.setText(settings.getBody(), StandardCharsets.UTF_8.name());

    MimeBodyPart attachmentPart = new MimeBodyPart();
    var dataSource = new ByteArrayDataSource(pdfData, "application/pdf");
    attachmentPart.setDataHandler(new DataHandler(dataSource));
    attachmentPart.setFileName(fileName);

    MimeMultipart mixedPart = new MimeMultipart("mixed", alternativePart, attachmentPart);

    message.setContent(mixedPart);
    return message;
  }
}
