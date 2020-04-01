package net.nhs.ems.emailadapter.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.apache.commons.lang3.time.DateFormatUtils;

public class DateUtil {

  public static String formatDate(Date date) {
    return DateFormatUtils.format(date, "dd-MMM-yyyy");
  }

  public static String formatDateTime(Date date) {
    ZonedDateTime zonedDateTime = date.toInstant().atZone(ZoneId.of("Europe/London"));
    return DateTimeFormatter.ofPattern("dd-MMM-yyyy, HH:mm z").format(zonedDateTime);
  }
}
