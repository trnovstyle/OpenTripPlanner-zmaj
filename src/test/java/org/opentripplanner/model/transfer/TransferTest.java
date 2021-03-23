package org.opentripplanner.model.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;

public class TransferTest {

  private final Stop STOP_A = Stop.stopForTest("A", 60.0, 11.0);
  private final Stop STOP_B = Stop.stopForTest("B", 60.0, 11.0);
  private final Stop STOP_ANY = Stop.stopForTest("ANY", 60.0, 11.0);

  private final Route ROUTE_1 = new Route(new FeedScopedId("R", "1"));
  private final Route ROUTE_2 = new Route(new FeedScopedId("R", "2"));

  private final Trip TRIP_1 = new Trip(new FeedScopedId("T", "1"));
  private final Trip TRIP_2 = new Trip(new FeedScopedId("T", "2"));
  private final Trip TRIP_ANY = new Trip(new FeedScopedId("T", "ANY"));

  private final Transfer TX_A_TO_B     = transfer(STOP_A, STOP_B,    na(),    na(),   na(),   na());
  private final Transfer TX_R1A_TO_B   = transfer(STOP_A, STOP_B, ROUTE_1,    na(),   na(),   na());
  private final Transfer TX_A_TO_R2B   = transfer(STOP_A, STOP_B,    na(), ROUTE_2,   na(),   na());
  private final Transfer TX_R1A_TO_R2B = transfer(STOP_A, STOP_B, ROUTE_1, ROUTE_2,   na(),   na());
  private final Transfer TX_T1A_TO_B   = transfer(STOP_A, STOP_B,    na(),    na(), TRIP_1,   na());
  private final Transfer TX_A_TO_T2B   = transfer(STOP_A, STOP_B,    na(),    na(),   na(), TRIP_2);
  private final Transfer TX_T1A_TO_R2B = transfer(STOP_A, STOP_B,    na(), ROUTE_2, TRIP_1,   na());
  private final Transfer TX_R1A_TO_T2B = transfer(STOP_A, STOP_B, ROUTE_1,    na(),   na(), TRIP_2);
  private final Transfer TX_T1A_TO_T2B = transfer(STOP_A, STOP_B,    na(),    na(), TRIP_1, TRIP_2);

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
  public void testToStringStopToStop() {
    assertEquals("Transfer{from: <Stop F:A>, to: <Stop F:B>}", TX_A_TO_B.toString());
  }

  @Test
  public void testToStringRouteToStop() {
    assertEquals("Transfer{from: (<Stop F:A>, <Route R:1 null>), to: <Stop F:B>}", TX_R1A_TO_B.toString());
  }

  @Test
  public void testToStringStopToRoute() {
    assertEquals("Transfer{from: <Stop F:A>, to: (<Stop F:B>, <Route R:2 null>)}", TX_A_TO_R2B.toString());
  }

  @Test
  public void testToStringRouteToRoute() {
    assertEquals("Transfer{from: (<Stop F:A>, <Route R:1 null>), to: (<Stop F:B>, <Route R:2 null>)}", TX_R1A_TO_R2B.toString());
  }

  @Test
  public void testToStringTripToStop() {
    assertEquals("Transfer{from: (<Stop F:A>, <Trip T:1>), to: <Stop F:B>}", TX_T1A_TO_B.toString());
  }

  @Test
  public void testToStringStopToTrip() {
    assertEquals("Transfer{from: <Stop F:A>, to: (<Stop F:B>, <Trip T:2>)}", TX_A_TO_T2B.toString());
  }

  @Test
  public void testToStringTripToRoute() {
    assertEquals("Transfer{from: (<Stop F:A>, <Trip T:1>), to: (<Stop F:B>, <Route R:2 null>)}", TX_T1A_TO_R2B.toString());
  }

  @Test
  public void testToStringRouteToTrip() {
    assertEquals("Transfer{from: (<Stop F:A>, <Route R:1 null>), to: (<Stop F:B>, <Trip T:2>)}", TX_R1A_TO_T2B.toString());
  }

  @Test
  public void testToStringTripToTrip() {
    assertEquals("Transfer{from: (<Stop F:A>, <Trip T:1>), to: (<Stop F:B>, <Trip T:2>)}", TX_T1A_TO_T2B.toString());
  }

  @Nullable
  private static <T> T na() {
    return null;
  }
  
  private static Transfer transfer(Stop frStop, Stop toStop, Route frRoute, Route toRoute, Trip frTrip, Trip toTrip) {
    var from = point(frStop, frRoute, frTrip);
    var to = point(toStop, toRoute, toTrip);

    return new Transfer(from, to, TransferPriority.ALLOWED, false, false);
  }

  private static TransferPoint point(Stop s, Route r, Trip t) {
    if(r == null && t == null) { return new StopTransferPoint(s); }
    if(r == null) { return new TripTransferPoint(t, 1); }
    else { return new RouteTransferPoint(r, t, 1); }
  }
}