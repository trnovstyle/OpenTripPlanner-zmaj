package org.opentripplanner.model.transfers;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.transfers.TransferType.RECOMMENDED;

public class TransferTest {
  private final int MIN_TX_TIME = -1;

  private final Stop STOP_A = Stop.stopForTest("A", 60.0, 11.0);
  private final Stop STOP_B = Stop.stopForTest("B", 60.0, 11.0);
  private final Stop STOP_ANY = Stop.stopForTest("ANY", 60.0, 11.0);

  private final Route ROUTE_1 = new Route(new FeedScopedId("R", "1"));
  private final Route ROUTE_2 = new Route(new FeedScopedId("R", "2"));
  private final Route ROUTE_ANY = new Route(new FeedScopedId("R", "ANY"));
  private final Route R_NA = null;

  private final Trip TRIP_1 = new Trip(new FeedScopedId("T", "1"));
  private final Trip TRIP_2 = new Trip(new FeedScopedId("T", "2"));
  private final Trip TRIP_ANY = new Trip(new FeedScopedId("T", "ANY"));
  private final Trip T_NA = null;

  private final Transfer TX_A_TO_B = new Transfer(STOP_A, STOP_B, R_NA, R_NA, T_NA, T_NA, RECOMMENDED, MIN_TX_TIME);
  private final Transfer TX_R1A_TO_B = new Transfer(STOP_A, STOP_B, ROUTE_1, R_NA, T_NA, T_NA, RECOMMENDED, MIN_TX_TIME);
  private final Transfer TX_A_TO_R2B = new Transfer(STOP_A, STOP_B, R_NA, ROUTE_2, T_NA, T_NA, RECOMMENDED, MIN_TX_TIME);
  private final Transfer TX_R1A_TO_R2B = new Transfer(STOP_A, STOP_B, ROUTE_1, ROUTE_2, T_NA, T_NA, RECOMMENDED, MIN_TX_TIME);
  private final Transfer TX_T1A_TO_B = new Transfer(STOP_A, STOP_B, R_NA, R_NA, TRIP_1, T_NA, RECOMMENDED, MIN_TX_TIME);
  private final Transfer TX_A_TO_T2B = new Transfer(STOP_A, STOP_B, R_NA, R_NA, T_NA, TRIP_2, RECOMMENDED, MIN_TX_TIME);
  private final Transfer TX_T1A_TO_R2B = new Transfer(STOP_A, STOP_B, R_NA, ROUTE_2, TRIP_1, T_NA, RECOMMENDED, MIN_TX_TIME);
  private final Transfer TX_R1A_TO_T2B = new Transfer(STOP_A, STOP_B, ROUTE_1, R_NA, T_NA, TRIP_2, RECOMMENDED, MIN_TX_TIME);
  private final Transfer TX_T1A_TO_T2B = new Transfer(STOP_A, STOP_B, R_NA, R_NA, TRIP_1, TRIP_2, RECOMMENDED, MIN_TX_TIME);

  private final List<Transfer> ALL_TRANSFERS = List.of(
      TX_A_TO_B, TX_R1A_TO_B, TX_A_TO_R2B, TX_R1A_TO_R2B,
      TX_T1A_TO_B,TX_A_TO_T2B, TX_T1A_TO_R2B, TX_R1A_TO_T2B, TX_T1A_TO_T2B
  );


  @Before
  public void setup() {
    TRIP_1.setRoute(ROUTE_1);
    TRIP_2.setRoute(ROUTE_2);
  }

  @Test
  public void matches() {
    assertTrue(TX_R1A_TO_B.matches(STOP_A, STOP_B, TRIP_1, TRIP_2));

    // All TRANSFERS should match going from specified Stop A, Trip 1 to Stop B Trip 2.
    for (Transfer tx : ALL_TRANSFERS) {
      assertTrue(tx.toString(), tx.matches(STOP_A, STOP_B, TRIP_1, TRIP_2));

      // Fail is stop do not match
      assertFalse(tx.matches(STOP_ANY, STOP_B, TRIP_1, TRIP_2));
      assertFalse(tx.matches(STOP_A, STOP_ANY, TRIP_1, TRIP_2));
    }

    // All trips matches when only stops are defined
    assertTrue(TX_A_TO_B.matches(STOP_A, STOP_B, TRIP_ANY, TRIP_ANY));

    // Does not match any trip when to Trip or Route is set
    assertFalse(TX_A_TO_R2B.matches(STOP_A, STOP_B, TRIP_1, TRIP_ANY));
    assertFalse(TX_A_TO_T2B.matches(STOP_A, STOP_B, TRIP_1, TRIP_ANY));

    // Does not match any trip when from Trip or Route is set
    assertFalse(TX_R1A_TO_B.matches(STOP_A, STOP_B, TRIP_ANY, TRIP_2));
    assertFalse(TX_T1A_TO_B.matches(STOP_A, STOP_B, TRIP_ANY, TRIP_2));
  }

  @Test
  public void getSpecificityRanking() {
    assertEquals(0, TX_A_TO_B.getSpecificityRanking());
    assertEquals(1, TX_R1A_TO_B.getSpecificityRanking());
    assertEquals(1, TX_A_TO_R2B.getSpecificityRanking());
    assertEquals(2, TX_R1A_TO_R2B.getSpecificityRanking());
    assertEquals(2, TX_T1A_TO_B.getSpecificityRanking());
    assertEquals(2, TX_A_TO_T2B.getSpecificityRanking());
    assertEquals(3, TX_T1A_TO_R2B.getSpecificityRanking());
    assertEquals(3, TX_R1A_TO_T2B.getSpecificityRanking());
    assertEquals(4, TX_T1A_TO_T2B.getSpecificityRanking());
  }

  @Test
  public void testToString() {
    assertEquals("<Transfer stop(F:A ~ F:B)>", TX_A_TO_B.toString());
    assertEquals("<Transfer stop(F:A ~ F:B) route(R:1 ~ )>", TX_R1A_TO_B.toString());
    assertEquals("<Transfer stop(F:A ~ F:B) route( ~ R:2)>", TX_A_TO_R2B.toString());
    assertEquals("<Transfer stop(F:A ~ F:B) route(R:1 ~ R:2)>", TX_R1A_TO_R2B.toString());
    assertEquals("<Transfer stop(F:A ~ F:B) trip(T:1 ~ )>", TX_T1A_TO_B.toString());
    assertEquals("<Transfer stop(F:A ~ F:B) trip( ~ T:2)>", TX_A_TO_T2B.toString());
    assertEquals("<Transfer stop(F:A ~ F:B) route( ~ R:2) trip(T:1 ~ )>", TX_T1A_TO_R2B.toString());
    assertEquals("<Transfer stop(F:A ~ F:B) route(R:1 ~ ) trip( ~ T:2)>", TX_R1A_TO_T2B.toString());
    assertEquals("<Transfer stop(F:A ~ F:B) trip(T:1 ~ T:2)>", TX_T1A_TO_T2B.toString());
  }
}