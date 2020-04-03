package net.nhs.ems.emailadapter.model;

import java.util.Map;
import java.util.stream.Stream;
import lombok.Value;

@Value
public class EmailSettings {
  private final static String SENDER_KEY = "EMS_REPORT_SENDER";
  private final static String RECIPIENT_KEY = "EMS_REPORT_RECIPIENT";
  private final static String SUBJECT_KEY = "EMS_REPORT_SUBJECT";
  private final static String BODY_KEY = "EMS_REPORT_BODY";

  String sender;
  String recipient;
  String subject;
  String body;

  public static EmailSettings from(Map<String, String> map) {
    if (!Stream.of(SENDER_KEY, RECIPIENT_KEY, SUBJECT_KEY, BODY_KEY).allMatch(map::containsKey)) {
      throw new IllegalArgumentException("Given map does not contain all necessary properties");
    }

    return new EmailSettings(
        map.get(SENDER_KEY),
        map.get(RECIPIENT_KEY),
        map.get(SUBJECT_KEY),
        map.get(BODY_KEY));
  }
}
