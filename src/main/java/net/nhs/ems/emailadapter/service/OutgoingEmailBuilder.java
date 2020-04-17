package net.nhs.ems.emailadapter.service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import net.nhs.ems.emailadapter.model.EmailSettings;

public class OutgoingEmailBuilder {

  public MimeMessage buildEmail(EmailSettings settings, Document doc, byte[] pdfData)
      throws MessagingException, ParseException {

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
    attachmentPart.setFileName(createFileName(doc));

    MimeMultipart mixedPart = new MimeMultipart("mixed", alternativePart, attachmentPart);

    message.setContent(mixedPart);
    return message;
  }
  
  private String createFileName(Document doc) throws ParseException {
    Element elementById = doc.getElementById("patientBanner");
    Elements select = elementById.select("table").first().select("td");
    String name = select.get(0).text().split(" ")[1];
    String dobString = select.get(1).text().split(" ")[1];
    SimpleDateFormat format2 = new SimpleDateFormat("dd-MMM-yyyy");
    Date date = format2.parse(dobString);
    String dob = DateFormatUtils.format(date, "YYYYMMdd");
    String[] split = select.get(3).text().split(" ");
    String nhsNumber = split[3] + split[4] + split[5];
    return nhsNumber + "_" + name + "_" + dob + ".pdf";
  }
}