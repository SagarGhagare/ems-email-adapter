package net.nhs.ems.emailadapter.model;

import java.util.Collections;
import java.util.List;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Resource;

public abstract class GPModel<T extends Resource> {

  protected EncounterReport encounterReport;
  protected T gp;

  public GPModel(EncounterReport encounterReport, T gp) {
    this.encounterReport = encounterReport;
    this.gp = gp;
  }

  public boolean isOrganization() {
    return gp instanceof Organization;
  }

  public boolean isPractitioner() {
    return gp instanceof Practitioner;
  }

  public abstract String getName();

  public abstract Address getAddress();

  public abstract List<ContactPoint> getTelecom();

  public static GPModel<?> from(EncounterReport encounterReport, Resource gp) {
    if (gp instanceof Organization) {
      return new OrganizationGP(encounterReport, (Organization) gp);
    } else if (gp instanceof Practitioner) {
      return new PractitionerGP(encounterReport, (Practitioner) gp);
    } else {
      return new UnknownGP<>(encounterReport, gp);
    }
  }

  private static class OrganizationGP extends GPModel<Organization> {

    public OrganizationGP(EncounterReport encounterReport, Organization gp) {
      super(encounterReport, gp);
    }

    public String getName() {
      return gp.getName();
    }

    public Address getAddress() {
      return gp.getAddressFirstRep();
    }

    public List<ContactPoint> getTelecom() {
      return gp.getTelecom();
    }
  }

  private static class PractitionerGP extends GPModel<Practitioner> {

    public PractitionerGP(EncounterReport encounterReport, Practitioner gp) {
      super(encounterReport, gp);
    }

    public String getName() {
      return gp.getNameFirstRep().getNameAsSingleString();
    }

    public Address getAddress() {
      return gp.getAddressFirstRep();
    }

    public List<ContactPoint> getTelecom() {
      return gp.getTelecom();
    }
  }

  private static class UnknownGP<T extends Resource> extends GPModel<T> {

    public UnknownGP(EncounterReport encounterReport, T gp) {
      super(encounterReport, gp);
    }

    @Override
    public String getName() {
      return "Unknown";
    }

    @Override
    public Address getAddress() {
      return new Address();
    }

    @Override
    public List<ContactPoint> getTelecom() {
      return Collections.emptyList();
    }
  }
}
