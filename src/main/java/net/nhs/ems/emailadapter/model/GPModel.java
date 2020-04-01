package net.nhs.ems.emailadapter.model;

import java.util.Collections;
import java.util.List;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Resource;

public class GPModel {

  private EncounterReport encounterReport;
  private Resource gp;

  public GPModel(EncounterReport encounterReport, Resource gp) {
    this.encounterReport = encounterReport;
    this.gp = gp;
  }

  public boolean isOrganization() {
    return gp instanceof Organization;
  }

  public boolean isPractitioner() {
    return gp instanceof Practitioner;
  }

  public String getName() {
    if (gp instanceof Organization) {
      return ((Organization) gp).getName();
    } else if (gp instanceof Practitioner) {
      return ((Practitioner) gp).getNameFirstRep().getNameAsSingleString();
    } else {
      return "Unknown";
    }
  }

  public Address getAddress() {
    if (gp instanceof Organization) {
      return ((Organization) gp).getAddressFirstRep();
    } else if (gp instanceof Practitioner) {
      return ((Practitioner) gp).getAddressFirstRep();
    } else {
      return null;
    }
  }

  public List<ContactPoint> getTelecom() {
    if (gp instanceof Organization) {
      return ((Organization) gp).getTelecom();
    } else if (gp instanceof Practitioner) {
      return ((Practitioner) gp).getTelecom();
    } else {
      return Collections.emptyList();
    }
  }
}
