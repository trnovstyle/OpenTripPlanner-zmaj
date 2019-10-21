package org.opentripplanner.graph_builder.triptransformer;


import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.model.calendar.ServiceDate;


public class TimeUtilTest {
    @Test
    public void isSummerTime() {
        Assert.assertTrue(TimeUtil.isDstSummerTime(2*3600));
        Assert.assertFalse(TimeUtil.isDstSummerTime(2*3600+1));
    }

    @Test
    public void dstToString() {
        Assert.assertEquals("3:00*", TimeUtil.dstToString(2*3600));
        Assert.assertEquals("3:00", TimeUtil.dstToString(3*3600));
    }

    @Test
    public void timeToString() {
        Assert.assertEquals("2:00", TimeUtil.timeToString(2 * 3600));
    }

    @Test
    public void dayBefore() {
        Assert.assertEquals(new ServiceDate(2019, 10, 22), TimeUtil.dayBefore(new ServiceDate(2019, 10, 23)));
    }

    @Test
    public void timeInSec() {
        Assert.assertEquals(0, TimeUtil.timeInSec("0:0"));
        Assert.assertEquals(0, TimeUtil.timeInSec("0:00"));
        Assert.assertEquals(60, TimeUtil.timeInSec("0:01"));
        Assert.assertEquals(60*60, TimeUtil.timeInSec("1:00"));
        Assert.assertEquals(0, TimeUtil.timeInSec("1:00*"));
        Assert.assertEquals((2*60+7)*60, TimeUtil.timeInSec("2:07"));
        Assert.assertEquals((9*60+9)*60, TimeUtil.timeInSec("09:09"));
        Assert.assertEquals((60+7)*60, TimeUtil.timeInSec("2:07*"));
    }
}
