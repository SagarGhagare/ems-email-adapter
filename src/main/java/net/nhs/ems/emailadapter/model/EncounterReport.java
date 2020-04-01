package net.nhs.ems.emailadapter.model;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.nhs.ems.emailadapter.util.DateUtil;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
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
        .map(consent -> consent.getActionFirstRep().getCodingFirstRep().getDisplay())
        .orElse("Unknown");
  }
}