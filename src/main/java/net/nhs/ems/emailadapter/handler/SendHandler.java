package net.nhs.ems.emailadapter.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ClinicalImpression;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.MedicationStatement;
import org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.Resource;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import net.nhs.ems.emailadapter.config.FHIRConfig;
import net.nhs.ems.emailadapter.model.EmailSettings;
import net.nhs.ems.emailadapter.model.EncounterReport;
import net.nhs.ems.emailadapter.service.OutgoingEmailBuilder;
import net.nhs.ems.emailadapter.service.StagedStopwatch;
import net.nhs.ems.emailadapter.transformer.HTMLReportTransformer;
import net.nhs.ems.emailadapter.transformer.PDFTransformer;

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
    addObservationToBundle(encounterReportBundle, encounterId);
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
      e.printStackTrace();
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

  private Patient getEntryByType(Bundle bundle) {
    return bundle.getEntry()
        .stream()
        .map(BundleEntryComponent::getResource)
        .filter(Patient.class::isInstance)
        .findFirst()
        .map(Patient.class::cast)
        .orElseThrow();
  }
  
  private Bundle addObservationToBundle(Bundle bundle, String encounterId) {
    String observationRef = null;
    Observation observation = null;
    String consentRef = null;
    Consent consent = null;
    String clinicalImpressionRef = null;
    ClinicalImpression clinicalImpression = null;
    String allergyIntoleranceRef = null;
    AllergyIntolerance allergyIntolerance = null;
    String diagnosticReportRef = null;
    DiagnosticReport diagnosticReport = null;
    String medicationStatementRef = null;
    MedicationStatement medicationStatement = null;
    String relatedPersonRef = null;
    RelatedPerson relatedPerson = null;
    List<BundleEntryComponent> entry = bundle.getEntry();
    for (BundleEntryComponent bundleEntryComponent : entry) {
      if (bundleEntryComponent.getResource().getResourceType().name().equals("List")) {
        ListResource resource = (ListResource) bundleEntryComponent.getResource();
        List<ListEntryComponent> ListEntry = resource.getEntry();
        for (ListEntryComponent ListEntryComponent : ListEntry) {
          if (ListEntryComponent.getItem().getReference().contains("Observation")) {
            observationRef = ListEntryComponent.getItem().getReference();
            observation = (Observation) fetchResourceFromUrl(encounterId, observationRef, "Observation");
          } else if (ListEntryComponent.getItem().getReference().contains("Consent")) {
            consentRef = ListEntryComponent.getItem().getReference();
            consent = (Consent) fetchResourceFromUrl(encounterId, consentRef, "Consent");
          } else if (ListEntryComponent.getItem().getReference().contains("ClinicalImpression")) {
            clinicalImpressionRef = ListEntryComponent.getItem().getReference();
            clinicalImpression = (ClinicalImpression) fetchResourceFromUrl(encounterId, clinicalImpressionRef, "ClinicalImpression");
          } else if (ListEntryComponent.getItem().getReference().contains("AllergyIntolerance")) {
            allergyIntoleranceRef = ListEntryComponent.getItem().getReference();
            allergyIntolerance = (AllergyIntolerance) fetchResourceFromUrl(encounterId, allergyIntoleranceRef, "AllergyIntolerance");
          } else if (ListEntryComponent.getItem().getReference().contains("DiagnosticReport")) {
            diagnosticReportRef = ListEntryComponent.getItem().getReference();
            diagnosticReport = (DiagnosticReport) fetchResourceFromUrl(encounterId, diagnosticReportRef, "DiagnosticReport");
          } else if (ListEntryComponent.getItem().getReference().contains("MedicationStatement")) {
            medicationStatementRef = ListEntryComponent.getItem().getReference();
            medicationStatement = (MedicationStatement) fetchResourceFromUrl(encounterId, medicationStatementRef, "MedicationStatement");
          } else if (ListEntryComponent.getItem().getReference().contains("RelatedPerson")) {
            relatedPersonRef = ListEntryComponent.getItem().getReference();
            relatedPerson = (RelatedPerson) fetchResourceFromUrl(encounterId, relatedPersonRef, "RelatedPerson");
          } else {}
      }
    }
  }
  bundle.getEntry().add(new BundleEntryComponent().setFullUrl(observationRef).setResource(observation));
  bundle.getEntry().add(new BundleEntryComponent().setFullUrl(consentRef).setResource(consent));
  bundle.getEntry().add(new BundleEntryComponent().setFullUrl(clinicalImpressionRef).setResource(clinicalImpression));
  bundle.getEntry().add(new BundleEntryComponent().setFullUrl(allergyIntoleranceRef).setResource(allergyIntolerance));
  bundle.getEntry().add(new BundleEntryComponent().setFullUrl(diagnosticReportRef).setResource(diagnosticReport));
  bundle.getEntry().add(new BundleEntryComponent().setFullUrl(medicationStatementRef).setResource(medicationStatement));
  bundle.getEntry().add(new BundleEntryComponent().setFullUrl(relatedPersonRef).setResource(relatedPerson));
  return bundle;
}
  
  private Resource fetchResourceFromUrl(String encounterId, String theUrl, String refType) {
    var id = new IdType(encounterId);
    IGenericClient client = FHIRConfig.fhirContext().newRestfulGenericClient(id.getBaseUrl());
    if (refType.equals("Observation")) {
      return client.fetchResourceFromUrl(Observation.class, theUrl);
    } else if (refType.equals("Consent")) {
      return client.fetchResourceFromUrl(Consent.class, theUrl);
    } else if (refType.equals("ClinicalImpression")) {
      return client.fetchResourceFromUrl(ClinicalImpression.class, theUrl);
    } else if (refType.equals("AllergyIntolerance")) {
      return client.fetchResourceFromUrl(AllergyIntolerance.class, theUrl);
    } else if (refType.equals("DiagnosticReport")) {
      return client.fetchResourceFromUrl(DiagnosticReport.class, theUrl);
    } else if (refType.equals("MedicationStatement")) {
      return client.fetchResourceFromUrl(MedicationStatement.class, theUrl);
    } else if (refType.equals("RelatedPerson")) {
      return client.fetchResourceFromUrl(RelatedPerson.class, theUrl);
    }
    return null;
  }
}
