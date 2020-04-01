package net.nhs.ems.emailadapter.model;

import java.util.Optional;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.NHSNumberIdentifier;

public class PatientIdentifierModel {

  private Identifier identifier;

  public PatientIdentifierModel(Identifier identifier) {
    this.identifier = identifier;
  }

  public Optional<NHSNumberIdentifier> nhsNumberIdentifier() {
    return Optional.of(identifier)
        .filter(NHSNumberIdentifier.class::isInstance)
        .map(NHSNumberIdentifier.class::cast);
  }

  public String getNhsNumber() {
    return nhsNumberIdentifier()
        .map(id -> formatNhsNumber(id.getValue()))
        .orElse("Unknown");
  }

  public String getValue() {
    return identifier.getValue();
  }

  private String formatNhsNumber(String value) {
    if (value.length() < 7) {
      return value;
    }
    return String.format("%s %s %s",
        value.substring(0, 4), value.substring(4, 7), value.substring(7));
  }

  public boolean isVerified() {
    return nhsNumberIdentifier()
        .map(NHSNumberIdentifier::getNhsNumberVerificationStatus)
        .filter(status -> "01".equals(status.getCodingFirstRep().getCode()))
        .isPresent();
  }

  public boolean isUnverified() {
    return nhsNumberIdentifier()
        .map(NHSNumberIdentifier::getNhsNumberVerificationStatus)
        .filter(status -> "02".equals(status.getCodingFirstRep().getCode()))
        .isPresent();
  }

  public boolean isLocal() {
    return !nhsNumberIdentifier().isPresent();
  }
}
