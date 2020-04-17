package net.nhs.ems.emailadapter.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.apache.commons.mail.util.MimeMessageParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.nhs.ems.emailadapter.model.EmailSettings;
import net.nhs.ems.emailadapter.service.OutgoingEmailBuilder;
import net.nhs.ems.emailadapter.service.StagedStopwatch;
import net.nhs.ems.emailadapter.transformer.PDFTransformer;

public class EmailHandler implements RequestHandler<SNSEvent, Object> {

  private OutgoingEmailBuilder emailBuilder = new OutgoingEmailBuilder();
  private AmazonSimpleEmailServiceAsync ses = AmazonSimpleEmailServiceAsyncClient.asyncBuilder().build();
  private AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build();
  protected Logger log = LoggerFactory.getLogger(getClass());

  public String handleRequest(SNSEvent request, Context context) {
    LambdaLogger logger = context.getLogger();
    var stopwatch = StagedStopwatch.start(logger);
    String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
    logger.log("Invocation started: " + timeStamp);
    String message = request.getRecords().get(0).getSNS().getMessage();
    logger.log(message);

    JsonObject jsonMessage = new JsonParser().parse(message).getAsJsonObject();
    JsonObject receipt = jsonMessage.get("receipt").getAsJsonObject();
    JsonObject action = receipt.get("action").getAsJsonObject();
    String objectKey = action.get("objectKey").getAsString();
    logger.log(objectKey);
    String bucketName = action.get("bucketName").getAsString();
    logger.log(bucketName);

    S3Object object = s3Client.getObject(bucketName, objectKey);
    stopwatch.finishStage("Getting s3 object");
    
    S3ObjectInputStream objectContent = object.getObjectContent();
    try {
      MimeMessage mes = getMimeMessageForRawEmailString(objectContent);
      MimeMessageParser parser = new MimeMessageParser(mes);
      String htmlContent = parser.parse().getHtmlContent();
      logger.log("Body: " + htmlContent);
      String plain = parser.parse().getPlainContent();
      logger.log("plain: " + plain);
      List<DataSource> attachmentList = parser.getAttachmentList();
      InputStream inputStream = attachmentList.get(0).getInputStream();
      stopwatch.finishStage("Getting html attchement from s3");
      
      String htmlString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      logger.log(htmlString);

      var pdfData = new PDFTransformer().transform(Jsoup.parse(htmlString).html());
      stopwatch.finishStage("pdf transformation");

      sendEmail(System.getenv(), pdfData, Jsoup.parse(htmlString));
      stopwatch.finishStage("sending email");

    } catch (Exception e) {
      logger.log(e.getMessage());
      return "ERROR";
    }
    timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
    logger.log("Invocation completed: " + timeStamp);
    return "SUCCESS";
  }

  private void sendEmail(Map<String, String> envVar, byte[] pdfData, Document document)
      throws MessagingException, IOException, ParseException {
    MimeMessage mimeMessage = emailBuilder.buildEmail(EmailSettings.from(envVar), document, pdfData);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    mimeMessage.writeTo(outputStream);
    RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

    SendRawEmailRequest request = new SendRawEmailRequest(rawMessage);
    log.trace("sending email");
    SendRawEmailResult sendRawEmail = ses.sendRawEmail(request);
    log.trace("SendRawEmailResult ", sendRawEmail);
  }

  private MimeMessage getMimeMessageForRawEmailString(S3ObjectInputStream objectContent)
      throws MessagingException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    MimeMessage message = new MimeMessage(session, objectContent);
    return message;
  }
}