package net.nhs.ems.emailadapter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactPoints {

  private String mobPhone;
  private String homePhone;
}
