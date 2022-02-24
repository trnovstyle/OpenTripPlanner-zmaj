package org.opentripplanner.ext.transmodelapi.mapping;

import org.opentripplanner.model.calendar.ServiceDate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.TimeZone;

public class ServiceDateMapper {
  private final TimeZone timeZone;

  public ServiceDateMapper(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  public Long serviceDateToSecondsSinceEpoch(ServiceDate serviceDate) {
    if (serviceDate == null) {
      return null;
    }

    return LocalDate.of(serviceDate.getYear(), serviceDate.getMonth(), serviceDate.getDay())
        .atStartOfDay(timeZone.toZoneId()).toEpochSecond();
  }

  /**
   * Get ServiceDate from epoch seconds, take consideration of time zone.
   * @param secondsSinceEpoch Seconds since EPOCH
   * @return Service date for given time zone
   */
  public ServiceDate secondsSinceEpochToServiceDate(Long secondsSinceEpoch) {

    var zdt = ZonedDateTime.now(timeZone.toZoneId());

    if (secondsSinceEpoch != null) {
      zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(secondsSinceEpoch), timeZone.toZoneId());
    }

    return new ServiceDate(zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth());
  }

}
