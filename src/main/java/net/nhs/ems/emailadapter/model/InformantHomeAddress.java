package net.nhs.ems.emailadapter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InformantHomeAddress {

  private String firstLine;
  private String secondLine;
  private String city;
  private String postcode;
}
