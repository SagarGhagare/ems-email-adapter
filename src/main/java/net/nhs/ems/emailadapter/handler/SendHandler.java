package net.nhs.ems.emailadapter.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import net.nhs.ems.emailadapter.config.FHIRConfig;
import net.nhs.ems.emailadapter.model.EmailSettings;
import net.nhs.ems.emailadapter.model.EncounterReport;
import net.nhs.ems.emailadapter.service.OutgoingEmailBuilder;
import net.nhs.ems.emailadapter.service.StagedStopwatch;
import net.nhs.ems.emailadapter.transformer.HTMLReportTransformer;
import net.nhs.ems.emailadapter.transformer.PDFTransformer;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.CareConnectPatient;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.IdType;

public class SendHandler implements RequestHandler<Map<String, String>, String> {

  private static final String ENCOUNTER_ID_PARAM = "encounterId";

  private AmazonSimpleEmailServiceAsync ses =
      AmazonSimpleEmailServiceAsyncClient.asyncBuilder().build();
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private OutgoingEmailBuilder emailBuilder = new OutgoingEmailBuilder();

  @Override
  public String handleRequest(Map<String, String> event, Context context) {
    LambdaLogger logger = context.getLogger();
    logger.log("Env variables: " + gson.toJson(System.getenv()) + '\n');
    logger.log("Event: " + gson.toJson(event) + '\n');
    logger.log("Context: " + gson.toJson(context) + '\n');

    var encounterId = event.get(ENCOUNTER_ID_PARAM);
    var stopwatch = StagedStopwatch.start(logger);

    Bundle encounterReportBundle = getEncounterReport(encounterId);
    stopwatch.finishStage("retrieving encounter report bundle");

    EncounterReport encounterReport = new EncounterReport(encounterReportBundle);
    stopwatch.finishStage("building encounter report model");

    try {
      var html = new HTMLReportTransformer().transform(encounterReport);
      stopwatch.finishStage("html transformation");

      var pdfData = new PDFTransformer().transform(html);
      stopwatch.finishStage("pdf transformation");

      sendEmail(System.getenv(), encounterReportBundle, pdfData);
      stopwatch.finishStage("sending email");

      return "200 OK";
    } catch (Exception e) {
      logger.log(e.getMessage());
      return "500 Server Error";
    }
  }

  private void sendEmail(Map<String, String> envVar, Bundle encounterReportBundle, byte[] pdfData)
      throws MessagingException, IOException {
    var patient = getEntryByType(encounterReportBundle);

    MimeMessage mimeMessage = emailBuilder.buildEmail(EmailSettings.from(envVar), patient, pdfData);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    mimeMessage.writeTo(outputStream);
    RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));

    SendRawEmailRequest request = new SendRawEmailRequest(rawMessage);
    ses.sendRawEmail(request);
  }

  private Bundle getEncounterReport(String encounterId) {
    var id = new IdType(encounterId);
    return FHIRConfig.fhirContext()
        .newRestfulGenericClient(id.getBaseUrl())
        .search()
        .forResource(Encounter.class)
        .where(Encounter.RES_ID.exactly().identifier(id.getIdPart()))
        .include(Encounter.INCLUDE_ALL.asRecursive())
        .revInclude(Encounter.INCLUDE_ALL.asRecursive())
        .returnBundle(Bundle.class)
        .execute();
  }

  private CareConnectPatient getEntryByType(Bundle bundle) {
    return bundle.getEntry()
        .stream()
        .map(BundleEntryComponent::getResource)
        .filter(CareConnectPatient.class::isInstance)
        .findFirst()
        .map(CareConnectPatient.class::cast)
        .orElseThrow();
  }
}
