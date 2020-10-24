package org.opentripplanner.netex.mapping.calendar;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DayTypeAndServiceJourneyIdTest {
  private static final String DT_ID = "DT-1";
  private static final String SJ_ID = "SJ-1";
  private static final String DT_OTHER_ID = "DT-99";
  private static final String SJ_OTHER_ID = "SJ-99";

  @Test
  public void testToStringAndGetters() {
    // To string should be easy to read for debugging
    DayTypeAndServiceJourneyId subject = new DayTypeAndServiceJourneyId(DT_ID, SJ_ID);

    assertEquals(SJ_ID, subject.serviceJourneyId());
    assertEquals(DT_ID, subject.dayTypeId());
    assertTrue(subject.toString().contains("sj=" + SJ_ID));
    assertTrue(subject.toString().contains("dt=" + DT_ID));
  }

  @SuppressWarnings("SimplifiableAssertion")
  @Test
  public void testEquals() {
    // Assert is safe to use in a Set or HashMap
    var a = new DayTypeAndServiceJourneyId(DT_ID, SJ_ID);
    var same = new DayTypeAndServiceJourneyId(DT_ID, SJ_ID);
    var sjDiff = new DayTypeAndServiceJourneyId(DT_OTHER_ID, SJ_ID);
    var dtDiff = new DayTypeAndServiceJourneyId(DT_ID, SJ_OTHER_ID);

    assertEquals(a.hashCode(), same.hashCode());
    assertEquals(a, same);

    assertFalse(a.equals(sjDiff));
    assertFalse(a.equals(dtDiff));

    assertTrue(a.hashCode() != sjDiff.hashCode());
    assertTrue(a.hashCode() != dtDiff.hashCode());
  }
}