package net.nhs.ems.emailadapter.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.nhs.ems.emailadapter.util.DateUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;

public class PatientModel {

  private EncounterReport encounterReport;
  private Optional<Patient> patient;

  public PatientModel(EncounterReport encounterReport, Optional<Patient> patient) {
    this.patient = patient;
    this.encounterReport = encounterReport;
  }

  public String getName() {
    return patient
        .map(patient -> patient
            .getNameFirstRep()
            .getNameAsSingleString())
        .orElse("Unknown");
  }

  public String getBorn() {
    return patient
        .filter(Patient::hasBirthDate)
        .map(patient -> DateUtil.formatDate(patient.getBirthDate()))
        .orElse("Unknown");
  }

  public String getGender() {
    return patient
        .filter(Patient::hasGender)
        .map(patient -> patient.getGender().getDisplay())
        .orElse("Unknown");
  }

  public List<PatientIdentifierModel> getIdentifiers() {
    return patient.map(patient ->
        patient.getIdentifier().stream()
            .map(PatientIdentifierModel::new)
            .collect(Collectors.toList()))
        .orElseGet(Collections::emptyList);
  }

  public List<Address> getAddress() {
    return patient
        .map(Patient::getAddress)
        .orElseGet(Collections::emptyList);
  }

  public List<ContactPoint> getTelecom() {
    return patient
        .map(Patient::getTelecom)
        .orElseGet(Collections::emptyList);
  }

  public List<GPModel> getGeneralPractitioner() {
    return patient.filter(Patient::hasGeneralPractitioner)
        .map(patient -> patient.getGeneralPractitioner().stream()
            .map(ref -> encounterReport.find(Resource.class, ref))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(gp -> new GPModel(encounterReport, gp))
            .collect(Collectors.toList())
        ).orElseGet(Collections::emptyList);
  }
}
