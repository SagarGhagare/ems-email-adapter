package net.nhs.ems.emailadapter.service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.dstu3.model.Patient;
import net.nhs.ems.emailadapter.model.EmailSettings;


public class OutgoingEmailBuilder {

  private String buildName(Patient patient) {
    var nhsNumber = patient.getIdentifierFirstRep().getValue();
    var name = patient.getNameFirstRep().getFamily().toUpperCase();
    var dob = DateFormatUtils.format(patient.getBirthDate(), "YYYYMMdd");
    return nhsNumber + "_" + name + "_" + dob + ".pdf";
  }

  public MimeMessage buildEmail(EmailSettings settings, String fileName, byte[] pdfData)
      throws MessagingException {

    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage message = new MimeMessage(session);

    message.setSubject(settings.getSubject(), "UTF-8");
    message.setFrom(new InternetAddress(settings.getSender()));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(settings.getRecipient()));

    // adapted from following example:
    // https://docs.aws.amazon.com/ses/latest/DeveloperGuide/send-email-raw.html
    // mime parts structured as follows:
//    mixed: called "mixed" as it contains a mix of email body and attachments
//    - alternative: called "alternative" as it can contain alternative/equivalent body types
//      - text
//      // - html: not currently using this
//    - attachment

    MimeBodyPart alternativePart = new MimeBodyPart();
    alternativePart.setText(settings.getBody(), StandardCharsets.UTF_8.name());

    MimeBodyPart attachmentPart = new MimeBodyPart();
    var dataSource = new ByteArrayDataSource(pdfData, "application/pdf");
    attachmentPart.setDataHandler(new DataHandler(dataSource));
    attachmentPart.setFileName(fileName);//buildName(patient));

    MimeMultipart mixedPart = new MimeMultipart("mixed", alternativePart, attachmentPart);

    message.setContent(mixedPart);
    return message;
  }

  public MimeMessage buildEmail(EmailSettings settings, Patient patient, byte[] pdfData) throws AddressException, MessagingException {
    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage message = new MimeMessage(session);

    message.setSubject(settings.getSubject(), "UTF-8");
    message.setFrom(new InternetAddress(settings.getSender()));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(settings.getRecipient()));

    // adapted from following example:
    // https://docs.aws.amazon.com/ses/latest/DeveloperGuide/send-email-raw.html
    // mime parts structured as follows:
//    mixed: called "mixed" as it contains a mix of email body and attachments
//    - alternative: called "alternative" as it can contain alternative/equivalent body types
//      - text
//      // - html: not currently using this
//    - attachment

    MimeBodyPart alternativePart = new MimeBodyPart();
    alternativePart.setText(settings.getBody(), StandardCharsets.UTF_8.name());

    MimeBodyPart attachmentPart = new MimeBodyPart();
    var dataSource = new ByteArrayDataSource(pdfData, "application/pdf");
    attachmentPart.setDataHandler(new DataHandler(dataSource));
    attachmentPart.setFileName(buildName(patient));

    MimeMultipart mixedPart = new MimeMultipart("mixed", alternativePart, attachmentPart);

    message.setContent(mixedPart);
    return message;
  }
}
