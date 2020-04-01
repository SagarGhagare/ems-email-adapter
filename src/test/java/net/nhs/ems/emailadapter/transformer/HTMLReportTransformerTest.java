package net.nhs.ems.emailadapter.transformer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar.Builder;
import net.nhs.ems.emailadapter.model.EncounterReport;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Address.AddressUse;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.CareConnectOrganization;
import org.hl7.fhir.dstu3.model.CareConnectPatient;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.NHSNumberIdentifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.Before;
import org.junit.Test;

public class HTMLReportTransformerTest {

  private HTMLReportTransformer htmlReportTransformer;

  @Before
  public void setup() {
    htmlReportTransformer = new HTMLReportTransformer();
  }

  @Test
  public void transformReport() throws IOException {
    EncounterReport encounterReport = buildEncounterReport();
    String html = htmlReportTransformer.transform(encounterReport);

    FileUtils.write(new File("src/test/resources/output.html"), html, StandardCharsets.UTF_8);
  }

  private EncounterReport buildEncounterReport() {
    Bundle bundle = new Bundle();
    bundle.getMeta().setLastUpdated(Date.from(LocalDateTime.of(
        2020, 04, 01,
        15, 32, 0, 0)
        .toInstant(ZoneOffset.UTC)));

    Organization gp = new CareConnectOrganization()
        .setName("Medway Medical Practice")
        .addAddress(new Address()
            .setUse(AddressUse.HOME)
            .addLine("123 Some Street")
            .addLine("A Town"))
        .addTelecom(new ContactPoint()
            .setUse(ContactPointUse.HOME)
            .setSystem(ContactPointSystem.PHONE)
            .setValue("01234567890"));
    gp.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("Organization/1")
        .setResource(gp));

    Patient patient = new CareConnectPatient()
        .addIdentifier(new NHSNumberIdentifier()
            .setNhsNumberVerificationStatus(new CodeableConcept()
                .addCoding(new Coding("", "02", "Unverified")))
            .setValue("993254128")
        )
        .addName(new HumanName()
            .addPrefix("Mr")
            .addGiven("Joe")
            .setFamily("Bloggs"))
        .setBirthDate(new Builder()
            .setDate(2010, 2, 15)
            .build().getTime())
        .setGender(AdministrativeGender.MALE)
        .addAddress(new Address()
            .setUse(AddressUse.HOME)
            .addLine("123 Some Street")
            .addLine("A Town"))
        .addTelecom(new ContactPoint()
            .setUse(ContactPointUse.HOME)
            .setSystem(ContactPointSystem.PHONE)
            .setValue("01234567890"))
        .addTelecom(new ContactPoint()
            .setUse(ContactPointUse.HOME)
            .setSystem(ContactPointSystem.EMAIL)
            .setValue("joe.bloggs.12345@gmail.com"))
        .addGeneralPractitioner(new Reference("Organization/1"));

    patient.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("Patient/1")
        .setResource(patient));

    Encounter encounter = new Encounter()
        .setSubject(new Reference("Patient/1"))
        .setServiceProvider(new Reference("Organization/1"));
    encounter.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("Encounter/1")
        .setResource(encounter));

    Consent consent = new Consent()
        .addConsentingParty(new Reference("Patient/1"))
        .addAction(new CodeableConcept().addCoding(
            new Coding("", "ptv", "Permission to view")));
    consent.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("Consent/1")
        .setResource(consent));

    return new EncounterReport(bundle);
  }
}