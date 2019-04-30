package org.opentripplanner.routing.flex;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.model.Area;

import java.util.Arrays;

import static org.junit.Assert.*;

public class AreaTest {

    @Test
    public void testCoordinatesToWkt() {
        Area area = new Area();
        area.setWkt(Arrays.asList( 1.1, 2.1, 3.1, 4.1 ));
        assertEquals("POLYGON((1.1 2.1, 3.1 4.1))", area.getWkt());
    }

    @Test
    public void testWktToCoordinates() {
        Area area = new Area();
        area.setWkt("POLYGON((1.1 2.1, 3.1 4.1))");
        assertArrayEquals(new Coordinate[] { new Coordinate(1.1, 2.1), new Coordinate(3.1, 4.1)}, area.getCoordinates());
    }
}
