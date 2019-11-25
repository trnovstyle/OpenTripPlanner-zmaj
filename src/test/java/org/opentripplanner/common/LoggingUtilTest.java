package org.opentripplanner.common;


import org.junit.Assert;
import org.junit.Test;

import static org.opentripplanner.common.LoggingUtil.human;

public class LoggingUtilTest {

    @Test
    public void testHumanReadable() {
        Assert.assertEquals("1 byte", human(1));
        Assert.assertEquals("12 bytes", human(12));
        Assert.assertEquals("123 bytes", human(123));
        Assert.assertEquals("1 kb", human(1234));
        Assert.assertEquals("12 kb", human(12345));
        Assert.assertEquals("123 kb", human(123456));
        Assert.assertEquals("1.2 MB", normalize(human(1234567)));
        Assert.assertEquals("12.3 MB", normalize(human(12345678)));
        // Round up
        Assert.assertEquals("123.5 MB", normalize(human(123456789)));
        Assert.assertEquals("1.2 GB", normalize(human(1234567890)));
        Assert.assertEquals("12.3 GB", normalize(human(12345678901L)));
    }

    private static String normalize(String number) {
        return number.replace(',', '.');
    }
}
