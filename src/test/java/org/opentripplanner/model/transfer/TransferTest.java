package org.opentripplanner.model.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.PREFERRED;

import org.junit.Before;
import org.junit.Test;

public class TransferTest implements TransferTestData {

  private final Transfer TX_A_TO_B     = transfer(STOP_POINT_A, STOP_POINT_B);
  private final Transfer TX_A_TO_R22 = transfer(STOP_POINT_A, ROUTE_POINT_22);
  private final Transfer TX_A_TO_T23 = transfer(STOP_POINT_A, TRIP_POINT_23);
  private final Transfer TX_R11_TO_B = transfer(ROUTE_POINT_11, STOP_POINT_B);
  private final Transfer TX_R11_TO_R22 = transfer(ROUTE_POINT_11, ROUTE_POINT_22);
  private final Transfer TX_T11_TO_R22 = transfer(TRIP_POINT_11, ROUTE_POINT_22);
  private final Transfer TX_T11_TO_T22 = transfer(TRIP_POINT_11, TRIP_POINT_23);

  private final Transfer TX_STAY_SEATED = new Transfer(TRIP_POINT_11, TRIP_POINT_23, ALLOWED, true, false);
  private final Transfer TX_GUARANTIED = new Transfer(TRIP_POINT_11, TRIP_POINT_23, ALLOWED, false, true);
  private final Transfer TX_EVERYTHING = new Transfer(TRIP_POINT_11, TRIP_POINT_23, PREFERRED, true, true);

  @Before
  public void setup() {
    ROUTE_1.setShortName("L1");
    ROUTE_2.setShortName("L2");
    TRIP_1.setRoute(ROUTE_1);
    TRIP_2.setRoute(ROUTE_2);
    TRIP_1.setRoute(ROUTE_1);
    TRIP_2.setRoute(ROUTE_2);
  }

  @Test
  public void getSpecificityRanking() {
    assertEquals(0, TX_A_TO_B.getSpecificityRanking());
    assertEquals(1, TX_R11_TO_B.getSpecificityRanking());
    assertEquals(1, TX_A_TO_R22.getSpecificityRanking());
    assertEquals(2, TX_R11_TO_R22.getSpecificityRanking());
    assertEquals(2, TX_A_TO_T23.getSpecificityRanking());
    assertEquals(3, TX_T11_TO_R22.getSpecificityRanking());
    assertEquals(4, TX_T11_TO_T22.getSpecificityRanking());
  }

  @Test
  public void testOtherAccessors() {
    assertEquals(STOP_POINT_A, TX_A_TO_R22.getFrom());
    assertEquals(ROUTE_POINT_22, TX_A_TO_R22.getTo());
    assertEquals(ALLOWED, TX_A_TO_B.getPriority());
    assertEquals(PREFERRED, TX_EVERYTHING.getPriority());
    assertTrue(TX_GUARANTIED.isGuaranteed());
    assertTrue(TX_STAY_SEATED.isStaySeated());
    assertFalse(TX_GUARANTIED.isStaySeated());
    assertFalse(TX_STAY_SEATED.isGuaranteed());
  }

  @Test
  public void priorityCost() {
    assertEquals(0, Transfer.priorityCost(null));
    assertEquals(-100, Transfer.priorityCost(TX_STAY_SEATED));
    assertEquals(-10, Transfer.priorityCost(TX_GUARANTIED));
  }

  @Test
  public void testToString() {
    assertEquals(
            "Transfer{from: (stop: F:A), to: (stop: F:B)}",
            TX_A_TO_B.toString()
    );
    assertEquals(
            "Transfer{from: (trip: T:1, stopPos: 1), to: (route: R:2, trip: T:2, stopPos: 2)}",
            TX_T11_TO_R22.toString()
    );
  }

  private static Transfer transfer(TransferPoint from, TransferPoint to) {
    return new Transfer(from, to, ALLOWED, false, false);
  }

}