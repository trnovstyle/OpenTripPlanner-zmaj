package org.opentripplanner.netex.mapping.calendar;

import org.junit.Test;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.netex.mapping.calendar.DayOfWeekMapper.mapDayOfWeek;
import static org.opentripplanner.netex.mapping.calendar.DayOfWeekMapper.mapDayOfWeeks;

public class DayOfWeekMapperTest {

  @Test
  public void mapDayOfWeekAllValuesTest() {
    assertEquals(Set.of(DayOfWeek.MONDAY), mapDayOfWeek(DayOfWeekEnumeration.MONDAY));
    assertEquals(Set.of(DayOfWeek.TUESDAY), mapDayOfWeek(DayOfWeekEnumeration.TUESDAY));
    assertEquals(Set.of(DayOfWeek.WEDNESDAY), mapDayOfWeek(DayOfWeekEnumeration.WEDNESDAY));
    assertEquals(Set.of(DayOfWeek.THURSDAY), mapDayOfWeek(DayOfWeekEnumeration.THURSDAY));
    assertEquals(Set.of(DayOfWeek.FRIDAY), mapDayOfWeek(DayOfWeekEnumeration.FRIDAY));
    assertEquals(Set.of(DayOfWeek.SATURDAY), mapDayOfWeek(DayOfWeekEnumeration.SATURDAY));
    assertEquals(Set.of(DayOfWeek.SUNDAY), mapDayOfWeek(DayOfWeekEnumeration.SUNDAY));
    assertEquals(Set.of(), mapDayOfWeek(DayOfWeekEnumeration.NONE));
    assertEquals(
        Set.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        ),
        mapDayOfWeek(DayOfWeekEnumeration.WEEKDAYS)
    );
    assertEquals(
        Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        mapDayOfWeek(DayOfWeekEnumeration.WEEKEND)
    );
    assertEquals(
        EnumSet.allOf(DayOfWeek.class),
        mapDayOfWeek(DayOfWeekEnumeration.EVERYDAY));
  }

  @Test
  public void mapDayOfWeekExistForAllValues() {
    for (DayOfWeekEnumeration it : DayOfWeekEnumeration.values()) {
      var result = mapDayOfWeek(it);

      if(it == DayOfWeekEnumeration.NONE) {
        assertEquals(Set.of(), result);
      }
      else {
        assertTrue(result.size() > 0);
        assertFalse(result.contains(null));
      }
    }
  }

  @Test
  public void mapDayOfWeeksTest() {
    assertEquals(
        Set.of(DayOfWeek.MONDAY),
        mapDayOfWeeks(List.of(DayOfWeekEnumeration.MONDAY))
    );
    assertEquals(
        Set.of(DayOfWeek.MONDAY),
        mapDayOfWeeks(List.of(DayOfWeekEnumeration.MONDAY, DayOfWeekEnumeration.MONDAY))
    );
    assertEquals(
        Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        mapDayOfWeeks(List.of(
            DayOfWeekEnumeration.WEEKEND, DayOfWeekEnumeration.WEDNESDAY
        ))
    );
  }
}