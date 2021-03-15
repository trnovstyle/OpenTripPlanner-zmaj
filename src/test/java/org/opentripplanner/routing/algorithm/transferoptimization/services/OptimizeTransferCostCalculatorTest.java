package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.util.time.DurationUtils.duration;

import org.junit.Test;

public class OptimizeTransferCostCalculatorTest {
  private static final double EPSILON = 0.01;

  final int d12s = duration("12s");
  final int d24s = duration("24s");
  final int d1m = duration("1m");
  final int d2m = duration("2m");
  final int d4m = duration("4m");
  final int d5m = duration("5m");
  final int d10m = duration("10m");
  final int d20m = duration("20m");
  final int d50m = duration("50m");
  final int d5d = 5 * duration("24h");

  private OptimizeTransferCostCalculator subject;


  /**
   * verify initial condition with a few typical values:
   * <pre>
   *   f(0) = N * t0
   *   f(t0) = t0
   * </pre>
   */
  @Test
  public void calculateTxCostInitialConditions() {
    int zero = 0;
    int[] ts = { d1m, d5m, d50m };
    double[] ns = { 1.0, 2.0, 10.0 };

    for (double n : ns) {
      subject = new OptimizeTransferCostCalculator(1.0, n);

      for (int t0 : ts) {
        subject.setMinSafeTransferTime(t0);
        String testCase = String.format("t0=%d, n=%.1f", t0, n);

        assertEquals("f(0) with " + testCase, n * t0, subject.calculateWaitCost(zero), EPSILON);
        assertEquals("f(t0) with " + testCase, t0, subject.calculateWaitCost(t0), EPSILON);
      }
    }
  }

  @Test
  public void calculateTxCost_sample_A() {
    subject = new OptimizeTransferCostCalculator(1.0, 2.0);
    subject.setMinSafeTransferTime(d2m);

    assertEquals(236.64, subject.calculateWaitCost(1), EPSILON);
    assertEquals(207.15, subject.calculateWaitCost(d12s), EPSILON);
    assertEquals(185.27, subject.calculateWaitCost(d24s), EPSILON);
    assertEquals(148.14, subject.calculateWaitCost(d1m), EPSILON);
    assertEquals(96.39, subject.calculateWaitCost(d4m), EPSILON);
    assertEquals( 73.60, subject.calculateWaitCost(d10m), EPSILON);
    assertEquals( 24.67, subject.calculateWaitCost(d5d), EPSILON);
  }

  @Test
  public void calculateTxCost_sample_B() {
    subject = new OptimizeTransferCostCalculator(1.0, 5.0);
    subject.setMinSafeTransferTime(d10m);

    assertEquals(2966.07, subject.calculateWaitCost(1), EPSILON);
    assertEquals(1835.69, subject.calculateWaitCost(d1m), EPSILON);
    assertEquals(1375.15, subject.calculateWaitCost(d2m), EPSILON);
    assertEquals(861.96, subject.calculateWaitCost(d5m), EPSILON);
    assertEquals(431.06, subject.calculateWaitCost(d20m), EPSILON);
    assertEquals(298.70, subject.calculateWaitCost(d50m), EPSILON);
    assertEquals(101.74, subject.calculateWaitCost(d5d), EPSILON);
  }

  @Test(expected = IllegalStateException.class)
  public void calculateTxCostWithNoMinSafeTxTimeThrowsException() {
    var subject = new OptimizeTransferCostCalculator(1.0, 2.0);
    subject.calculateWaitCost(d20m);
  }
}