package org.opentripplanner.ext.transmodelapi.model;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.model.modes.TransitMainMode;

public class TransportModeSlackTest {

    @Test
    public void mapToApiList() {
        // Given
        Map<TransitMainMode, Integer> domain = Map.of(
                TransitMainMode.FUNICULAR, 600,
                TransitMainMode.CABLE_CAR, 600,
                TransitMainMode.RAIL, 1800,
                TransitMainMode.AIRPLANE, 3600
        );

        // When
        List<TransportModeSlack> result = TransportModeSlack.mapToApiList(domain);

        Assert.assertEquals(600, result.get(0).slack);
        Assert.assertTrue(result.get(0).modes.contains(TransitMainMode.CABLE_CAR));
        Assert.assertTrue(result.get(0).modes.contains(TransitMainMode.FUNICULAR));

        Assert.assertEquals(1800, result.get(1).slack);
        Assert.assertTrue(result.get(1).modes.contains(TransitMainMode.RAIL));

        Assert.assertEquals(3600, result.get(2).slack);
        Assert.assertTrue(result.get(2).modes.contains(TransitMainMode.AIRPLANE));
    }

    @Test
    public void mapToDomain() {
        // Given
        List<Object> apiSlackInput = List.of(
                Map.of(
                        "slack", 600,
                        "modes", List.of(TransitMainMode.FUNICULAR, TransitMainMode.CABLE_CAR)
                ),
                Map.of(
                        "slack", 1800,
                        "modes", List.of(TransitMainMode.RAIL)
                ),
                Map.of(
                        "slack", 3600,
                        "modes", List.of(TransitMainMode.AIRPLANE)
                )
        );


        Map<TransitMainMode, Integer> result;

        // When
        result = TransportModeSlack.mapToDomain(apiSlackInput);

        // Then
        Assert.assertNull(result.get(TransitMainMode.BUS)  );
        Assert.assertEquals(Integer.valueOf(600), result.get(TransitMainMode.FUNICULAR));
        Assert.assertEquals(Integer.valueOf(600), result.get(TransitMainMode.CABLE_CAR));
        Assert.assertEquals(Integer.valueOf(1800), result.get(TransitMainMode.RAIL));
        Assert.assertEquals(Integer.valueOf(3600), result.get(TransitMainMode.AIRPLANE));
    }
}