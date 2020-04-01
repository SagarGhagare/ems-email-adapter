package net.nhs.ems.emailadapter.service;

import ca.uhn.fhir.context.FhirContext;
import lombok.RequiredArgsConstructor;
import net.nhs.ems.emailadapter.model.EncounterReport;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IIdType;

@RequiredArgsConstructor
public class EncounterReportService {

  private final FhirContext fhirContext;

  public EncounterReport fetchEncounterReport(Reference encounterRef) {
    IIdType ref = encounterRef.getReferenceElement();
    String baseUrl = ref.getBaseUrl();
    String encounterId = ref.getIdPart();

    Bundle bundle = fhirContext.newRestfulGenericClient(baseUrl)
        .search().forResource(Encounter.class)
        .where(Encounter.RES_ID.exactly().identifier(encounterId))
        .include(Encounter.INCLUDE_ALL)
        .revInclude(Encounter.INCLUDE_ALL)
        .returnBundle(Bundle.class)
        .execute();

    return new EncounterReport(bundle);
  }
}
