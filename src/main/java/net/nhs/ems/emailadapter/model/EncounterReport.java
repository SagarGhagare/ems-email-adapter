package net.nhs.ems.emailadapter.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.AllergyIntolerance;
import org.hl7.fhir.dstu3.model.Annotation;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ClinicalImpression;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Composition;
import org.hl7.fhir.dstu3.model.Composition.SectionComponent;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Dosage;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Encounter.EncounterStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.MedicationStatement;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.Resource;
import net.nhs.ems.emailadapter.util.DateUtil;

public class EncounterReport {

  private Bundle bundle;

  public EncounterReport(Bundle bundle) {
    this.bundle = bundle;
  }

  public String getTitle() {
    return "Encounter Report";
  }

  @SafeVarargs
  public final <T extends Resource> Optional<T> find(Class<T> klass, Predicate<T>... filter) {
    Stream<T> s = bundle.getEntry().stream()
        .map(BundleEntryComponent::getResource)
        .filter(klass::isInstance)
        .map(klass::cast);

    for (Predicate<T> predicate : filter) {
      s = s.filter(predicate);
    }
    return s.findFirst();
  }

  public <T extends Resource> Optional<T> find(Class<T> klass, Reference reference) {
    return find(klass, resource ->
        resource.getIdElement().getIdPart().equals(reference.getReferenceElement().getIdPart()));
  }

  public Optional<Encounter> getEncounter() {
    return find(Encounter.class);
  }

  public Optional<Composition> getComposition() {
    return find(Composition.class);
  }
  
  public PatientModel getPatient() {
    return new PatientModel(this, getEncounter()
        .map(Encounter::getSubject)
        .flatMap(ref -> find(Patient.class, ref)));
  }

  public String getCreated() {
    return bundle.getMeta().hasLastUpdated()
        ? DateUtil.formatDateTime(bundle.getMeta().getLastUpdated())
        : "Unknown";
  }

  public Optional<Organization> getServiceProvider() {
    return getEncounter()
        .filter(Encounter::hasServiceProvider)
        .map(Encounter::getServiceProvider)
        .flatMap(ref -> find(Organization.class, ref));
  }

  public String getOwner() {
    return getServiceProvider()
        .map(Organization::getName)
        .orElse("Unknown");
  }

  public String getConsentStatus() {
    return find(Consent.class)
        .filter(Consent::hasAction)
        .map(consent -> consent.getAction().stream()
            .map(action -> action.getCodingFirstRep().getDisplay())
            .collect(Collectors.joining(","))
        )
        .orElse("Unknown");
  }

  private Optional<Period> getConsentPeriod() {
    return find(Consent.class)
        .filter(Consent::hasPeriod)
        .map(Consent::getPeriod);
  }

  public String getConsentObtained() {
    return getConsentPeriod()
        .filter(Period::hasStart)
        .map(Period::getStart)
        .map(DateUtil::formatDate)
        .orElse("Unknown");
  }

  public String getConsentExpires() {
    return getConsentPeriod()
        .filter(Period::hasEnd)
        .map(Period::getEnd)
        .map(DateUtil::formatDate)
        .orElse("Unknown");
  }

  public String getEncounterPeriod() {
    return getEncounter()
        .filter(Encounter::hasPeriod)
        .map(Encounter::getPeriod)
        .map(period -> {
          StringBuilder sb = new StringBuilder();
          if (period.hasStart()) {
            sb.append(DateUtil.formatDateTime(period.getStart()));
          }
          if (period.hasEnd()) {
            sb
                .append(" to ")
                .append(DateUtil.formatDateTime(period.getEnd()));
          }
          return sb.toString();
        })
        .orElse("Unknown");
  }

  public String getEncounterId() {
    return getEncounter()
        .map(Encounter::getId)
        .orElse("Unknown");
  }

  public List<Identifier> getEncounterIdentifiers() {
    return getEncounter()
        .map(Encounter::getIdentifier)
        .orElseGet(Collections::emptyList);
  }

  public String getEncounterStatus() {
    return getEncounter()
        .map(Encounter::getStatus)
        .map(EncounterStatus::getDisplay)
        .orElse("Unknown");
  }

  public List<Location> getEncounterLocation() {
    return getEncounter().stream()
        .flatMap((Encounter encounter) -> encounter.getLocation().stream())
        .map(locationComponent -> find(Location.class, locationComponent.getLocation()))
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
  }

  public String getCareSettingType() {
    return getEncounterLocation().stream()
        .map(Location::getType)
        .map(CodeableConcept::getCodingFirstRep)
        .map(Coding::getDisplay)
        .findFirst()
        .orElse("Unknown");
  }
  
  public String getResponsibleParty() {
    return getEncounter().stream()
        .flatMap(encounter -> encounter.getEpisodeOfCare().stream())
        .flatMap(eoc -> find(EpisodeOfCare.class, eoc).stream())
        .filter(EpisodeOfCare::hasCareManager)
        .map(EpisodeOfCare::getCareManager)
        .flatMap(ref -> find(Practitioner.class, ref).stream())
        .map(practitioner -> practitioner.getNameFirstRep().getNameAsSingleString())
        .findFirst().orElse("Unknown");
  }
  
  public String getResponsiblePartyOrg() {
    return getEncounter().stream()
        .flatMap(encounter -> encounter.getEpisodeOfCare().stream())
        .flatMap(eoc -> find(EpisodeOfCare.class, eoc).stream())
        .filter(EpisodeOfCare::hasManagingOrganization)
        .map(EpisodeOfCare::getManagingOrganization)
        .flatMap(ref -> find(Organization.class, ref).stream())
        .map(org -> org.getName())
        .findFirst().orElse("Unknown");
  }
  
  public String getAppointmentDesc() {
    return find(Appointment.class)
        .map(Appointment::getDescription)
        .orElse("Unknown");
  }
  
  public String getAppointmentComment() {
    return find(Appointment.class)
        .map(Appointment::getComment)
        .orElse("Unknown");
  }
  
  public String getSection1Title() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    return sections.get().get(2).getTitle();
  }
  
  public String getSection1Text() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    String divAsString = sections.get().get(2).getText().getDivAsString()
        .replace("<tbody xmlns=\"http://www.w3.org/1999/xhtml\">", "<tbody>")
        .replace("<th>", "<th class=\"default\">");
    Document doc = Jsoup.parse(divAsString, "", Parser.xmlParser());
    return doc.html();
  }
  
  public String getSection2Title() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    return sections.get().get(3).getTitle();
  }
  
  public String getSection2Text() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    Document doc = Jsoup.parse(sections.get().get(3).getText().getDivAsString(), "", Parser.xmlParser());
    return doc.select("div").text();
  }
  
  public String getSection3Title() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    return sections.get().get(4).getTitle();
  }
  
  public String getSection3Text() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    Document doc = Jsoup.parse(sections.get().get(4).getText().getDivAsString(), "", Parser.xmlParser());
    return doc.select("div").text();
  }
  
  public String getSection4Title() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    return sections.get().get(5).getTitle();
  }
  
  public String getSection4Text() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    Document doc = Jsoup.parse(sections.get().get(5).getText().getDivAsString(), "", Parser.xmlParser());
    return doc.select("div").text();
  }
  
  public String getSection5Title() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    return sections.get().get(6).getTitle();
  }
  
  public String getSection5Text() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    Document doc = Jsoup.parse(sections.get().get(6).getText().getDivAsString(), "", Parser.xmlParser());
    return doc.select("div").text();
  }
  
  public String getSection6Title() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    return sections.get().get(7).getTitle();
  }
  
  public String getSection6Text() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    Document doc = Jsoup.parse(sections.get().get(7).getText().getDivAsString(), "", Parser.xmlParser());
    return doc.select("div").text();
  }
  
  public String getSection7Title() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    return sections.get().get(8).getTitle();
  }
  
  public String getSection7Text() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    Document doc = Jsoup.parse(sections.get().get(8).getText().getDivAsString(), "", Parser.xmlParser());
    return doc.select("div").text();
  }
  
  public String getSection8Title() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    return sections.get().get(9).getTitle();
  }
  
  public String getSection8Text() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    Document doc = Jsoup.parse(sections.get().get(9).getText().getDivAsString(), "", Parser.xmlParser());
    return doc.select("div").text();
  }
  
  public String getSection9Title() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    return sections.get().get(10).getTitle();
  }
  
  public String getSection9Text() {
    Optional<List<SectionComponent>> sections = getComposition().map(Composition::getSection);
    Document doc = Jsoup.parse(sections.get().get(10).getText().getDivAsString(), "", Parser.xmlParser());
    return doc.select("div").text();
  }
  
  public String getObservation() {
    return find(Observation.class)
        .filter(Observation::hasComment)
        .map(Observation::getComment)
        .orElse("Unknown");
  }
  
  public String getInformant() {
    HumanName nameFirstRep = find(RelatedPerson.class).get().getNameFirstRep();
    StringBuilder sb = new StringBuilder();
    sb.append(nameFirstRep.getGivenAsSingleString())
    .append(" ")
    .append(nameFirstRep.getFamily())
    .append(" - ")
    .append(find(RelatedPerson.class).get().getRelationship().getCodingFirstRep().getDisplay());
    return sb.toString();
    //return find(RelatedPerson.class).get().getNameFirstRep().getNameAsSingleString();
  }

  public InformantHomeAddress getInformantHomeAddress() {
    Address addressFirstRep = find(RelatedPerson.class).get().getAddressFirstRep();
    InformantHomeAddress add = new InformantHomeAddress();
    add.setFirstLine(addressFirstRep.getLine().get(0).asStringValue());
    add.setSecondLine(addressFirstRep.getLine().get(1).asStringValue());
    add.setCity(addressFirstRep.getCity());
    add.setPostcode(addressFirstRep.getPostalCode());
    return add;
  }
  
  public ContactPoints getContactPoints() {
    List<ContactPoint> contactPoint = find(RelatedPerson.class).get().getTelecom();
    ContactPoints contactPoints = new ContactPoints();
    contactPoints.setMobPhone(contactPoint.get(0).getValue());
    contactPoints.setHomePhone(contactPoint.get(0).getValue());
    return contactPoints;
  }
  
  public String getMedicationStatement() {
    return find(MedicationStatement.class)
        .map(MedicationStatement::getDosageFirstRep)
        .map(Dosage::getText)
        .orElse("Unknown");
  }
  
  public String getAllergyIntolerance() {
    return find(AllergyIntolerance.class)
        .map(AllergyIntolerance::getNoteFirstRep)
        .map(Annotation::getText)
        .orElse("Unknown");
  }
  
  public String getTriageReport() {
    return find(DiagnosticReport.class)
        .map(DiagnosticReport::getConclusion)
        .orElse("Unknown");
  }
  
  public String getClinicalImpression() {
    return find(ClinicalImpression.class)
        .filter(ClinicalImpression::hasDescription)
        .map(ClinicalImpression::getDescription)
        .orElse("Unknown");
  }
}