package net.nhs.ems.emailadapter.transformer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar.Builder;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Address.AddressUse;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.CareConnectOrganization;
import org.hl7.fhir.dstu3.model.CareConnectPatient;
import org.hl7.fhir.dstu3.model.ClinicalImpression;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Composition;
import org.hl7.fhir.dstu3.model.Composition.SectionComponent;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.dstu3.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Dosage;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Encounter.EncounterStatus;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.ListResource;
import org.hl7.fhir.dstu3.model.MedicationStatement;
import org.hl7.fhir.dstu3.model.MedicationStatement.MedicationStatementTaken;
import org.hl7.fhir.dstu3.model.NHSNumberIdentifier;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Observation.ObservationStatus;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.Before;
import org.junit.Test;
import net.nhs.ems.emailadapter.model.EncounterReport;

public class HTMLReportTransformerTest {

  public static final String OUTPUT_PATH = "src/test/resources/output2.html";

  private HTMLReportTransformer htmlReportTransformer;

  @Before
  public void setup() {
    htmlReportTransformer = new HTMLReportTransformer();
  }

  @Test
  public void transformReport() throws IOException {
    EncounterReport encounterReport = buildEncounterReport();
    String html = htmlReportTransformer.transform(encounterReport);

    FileUtils.write(new File(OUTPUT_PATH), html, StandardCharsets.UTF_8);
  }

  private java.util.Date date(int month, int day, int hour, int minute) {
    return Date.from(LocalDateTime.of(
        2020, month, day,
        hour, minute, 0, 0)
        .toInstant(ZoneOffset.UTC));
  }

  private EncounterReport buildEncounterReport() {
    Bundle bundle = new Bundle();
    bundle.getMeta().setLastUpdated(Date.from(LocalDateTime.of(
        2020, 9, 1,
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

    Practitioner practitioner = new Practitioner()
        .addName(new HumanName()
            .addPrefix("Dr")
            .setFamily("Frankenstein"));
    practitioner.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("Practitioner/1")
        .setResource(practitioner));

    EpisodeOfCare episodeOfCare = new EpisodeOfCare()
        .setCareManager(new Reference("Practitioner/1"))
        .setManagingOrganization(new Reference("Organization/1"));
    episodeOfCare.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("EpisodeOfCare/1")
        .setResource(episodeOfCare));

    Appointment appointment = new Appointment()
        .setDescription("Appointment Reference")
        .setComment("Date/Time: 01 June 2017 14:00\r\n" + 
            "  Location: CENTRAL LONDON COMMUNITY HEALTHCARE NHS TRUST\r\n" + 
            "  Psychiatric Community Care Service");
    appointment.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("Appointment/1")
        .setResource(appointment));
    
    Encounter encounter = new Encounter()
        .addEpisodeOfCare(new Reference("EpisodeOfCare/1"))
        .setStatus(EncounterStatus.FINISHED)
        .setSubject(new Reference("Patient/1"))
        .setServiceProvider(new Reference("Organization/1"))
        .setPeriod(new Period()
            .setStart(date(4, 1, 13, 0))
            .setEnd(date(4, 1, 14, 32)));
    encounter.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("Encounter/1")
        .setResource(encounter));

    Consent consent = new Consent()
        .addConsentingParty(new Reference("Patient/1"))
        .addAction(new CodeableConcept().addCoding(
            new Coding("", "ptv", "Permission to view")))
        .setPeriod(new Period()
            .setStart(date(1, 1, 0, 0))
            .setEnd(date(12, 31, 23, 59)));
    consent.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("Consent/1")
        .setResource(consent));

    Observation observation = new Observation()
        .setContext(new Reference("Encounter/1"))
        .setStatus(ObservationStatus.PRELIMINARY).setIssued(date(1, 1, 0, 0))
        .setComment("Patient feels dizzy.");
    observation.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("Observation/1")
        .setResource(observation));
    
    Narrative narrative = new Narrative();
    narrative.setDivAsString("Patient feels dizzy.");
    Narrative narrative2 = new Narrative();
    narrative2.setDivAsString("Patient feels dizzy.");
    List<SectionComponent> theSection =  new ArrayList<Composition.SectionComponent>();
    theSection.add(new SectionComponent().setTitle(""));
    theSection.add(new SectionComponent().setTitle(""));
    theSection.add(new SectionComponent().setTitle("Permission to View").setText(narrative2));
    theSection.add(new SectionComponent().setTitle("Primary Reason for Call").setText(narrative));
    theSection.add(new SectionComponent().setTitle("Primary Reason for Call").setText(narrative));
    theSection.add(new SectionComponent().setTitle("Primary Reason for Call").setText(narrative));
    theSection.add(new SectionComponent().setTitle("Primary Reason for Call").setText(narrative));
    theSection.add(new SectionComponent().setTitle("Primary Reason for Call").setText(narrative));
    theSection.add(new SectionComponent().setTitle("Primary Reason for Call").setText(narrative));
    theSection.add(new SectionComponent().setTitle("Primary Reason for Call").setText(narrative));
    theSection.add(new SectionComponent().setTitle("Primary Reason for Call").setText(narrative));
    theSection.add(new SectionComponent().setTitle("Primary Reason for Call").setText(narrative));
    Composition composition = new Composition()
        .setEncounter(new Reference("Encounter/1"))
        .setSection(theSection );
    composition.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("Composition/1")
        .setResource(composition));
    
    RelatedPerson relatedPerson = new RelatedPerson()
        .setPatient(new Reference("Patient/1"))
        .setRelationship(new CodeableConcept().addCoding(
            new Coding("", "sp", "Spouse")))
        .addName(new HumanName()
            .addGiven("Helga")
            .setFamily("Bloggs"))
        .setGender(AdministrativeGender.MALE)
        .addAddress(new Address()
            .setUse(AddressUse.HOME)
            .addLine("43 Summers Avenue")
            .addLine("Somerset Street")
            .setCity("Medway")
            .setPostalCode("ME5 7TY"))
        .addTelecom(new ContactPoint()
            .setUse(ContactPointUse.HOME)
            .setSystem(ContactPointSystem.PHONE)
            .setValue("07886554123"))
        .addTelecom(new ContactPoint()
            .setUse(ContactPointUse.HOME)
            .setSystem(ContactPointSystem.PHONE)
            .setValue("01783678321"));
    relatedPerson.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("RelatedPerson/1")
        .setResource(relatedPerson));
    
    MedicationStatement medication = new MedicationStatement()
        .setSubject(new Reference("Patient/1"))
        .setContext(new Reference("Encounter/1"))
        .setTaken(MedicationStatementTaken.UNK)
        .addDosage(new Dosage().setText("Medications"));
    medication.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("MedicationStatement/1")
        .setResource(medication));
    
    AllergyIntolerance allergyIntolerance = new AllergyIntolerance()
        .setPatient(new Reference("Patient/1"))
        .addNote(new Annotation().setText("No known allergies or adverse reaction."));
    allergyIntolerance.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("allergyIntolerance/1")
        .setResource(allergyIntolerance));
    
    DiagnosticReport diagnosticReport = new DiagnosticReport()
        .setSubject(new Reference("Patient/1"))
        .setConclusion("An injury or other health problem was described.The individual was described as breathing and conscious.Loss of at least a mugful of blood in the last 30 minutes was not described.An illness or other health problem was described. - Dizziness Fighting for breath was not described.A heart attack, chest/upper back pain, recent probable stroke, recent fit/seizure or suicide attempt was not described as the main call reason.Dizziness and nausea was described.");
    diagnosticReport.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("DiagnosticReport/1")
        .setResource(diagnosticReport));
    
    ClinicalImpression clinicalImpression = new ClinicalImpression()
        .setSubject(new Reference("Patient/1"))
        .setDescription("Patient fels nausea and dizzy after waking up. Lasted 30 minutes.");
    clinicalImpression.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("ClinicalImpression/1")
        .setResource(clinicalImpression));
    
    ListResource list = new ListResource();
    list.addEntry(new ListEntryComponent().setItem(new Reference("Observation/1")));
    list.addEntry(new ListEntryComponent().setItem(new Reference("ClinicalImpression/1")));
    list.addEntry(new ListEntryComponent().setItem(new Reference("DiagnosticReport/1")));
    list.setId("1");
    bundle.addEntry(new BundleEntryComponent()
        .setFullUrl("List/1")
        .setResource(list));
    
    return new EncounterReport(bundle);
  }
}