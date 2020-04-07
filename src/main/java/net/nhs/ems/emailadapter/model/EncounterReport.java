package net.nhs.ems.emailadapter.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.nhs.ems.emailadapter.util.DateUtil;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Encounter.EncounterStatus;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;

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

  public String getResponsibleParty() {
    // TODO add org of responsible party
    return getEncounter().stream()
        .flatMap(encounter -> encounter.getEpisodeOfCare().stream())
        .flatMap(eoc -> find(EpisodeOfCare.class, eoc).stream())
        .filter(EpisodeOfCare::hasCareManager)
        .map(EpisodeOfCare::getCareManager)
        .flatMap(ref -> find(Practitioner.class, ref).stream())
        .map(practitioner -> practitioner.getNameFirstRep().getNameAsSingleString())
        .findFirst().orElse("Unknown");
  }
}