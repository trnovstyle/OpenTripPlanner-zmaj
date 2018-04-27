package org.opentripplanner.netex.mapping;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.model.TransmodelTransportSubmode;
import org.rutebanken.netex.model.AirSubmodeEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.FunicularSubmodeEnumeration;
import org.rutebanken.netex.model.MetroSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TelecabinSubmodeEnumeration;
import org.rutebanken.netex.model.TramSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

public class TransportModeMapperTest {


    private TransportModeMapper modeMapper = new TransportModeMapper();

    @Test
    public void mapTransportSubMode_allAirSubmodeValuesShouldBeMapped() {
        for (AirSubmodeEnumeration submode : AirSubmodeEnumeration.values()) {
            TransportSubmodeStructure submodeStructure = new TransportSubmodeStructure();
            submodeStructure.setAirSubmode(submode);
            verifyMapping(submodeStructure,submode.value());
        }
    }

    @Test
    public void mapTransportSubMode_allBusSubmodeValuesShouldBeMapped() {
        for (BusSubmodeEnumeration submode : BusSubmodeEnumeration.values()) {
            TransportSubmodeStructure submodeStructure = new TransportSubmodeStructure();
            submodeStructure.setBusSubmode(submode);
            verifyMapping(submodeStructure,submode.value());
        }
    }

    @Test
    public void mapTransportSubMode_allRailSubmodeValuesShouldBeMapped() {
        for (RailSubmodeEnumeration submode : RailSubmodeEnumeration.values()) {
            TransportSubmodeStructure submodeStructure = new TransportSubmodeStructure();
            submodeStructure.setRailSubmode(submode);
            verifyMapping(submodeStructure,submode.value());
        }
    }

    @Test
    public void mapTransportSubMode_allFuniciularSubmodeValuesShouldBeMapped() {
        for (FunicularSubmodeEnumeration submode : FunicularSubmodeEnumeration.values()) {
            TransportSubmodeStructure submodeStructure = new TransportSubmodeStructure();
            submodeStructure.setFunicularSubmode(submode);
            verifyMapping(submodeStructure,submode.value());
        }
    }

    @Test
    public void mapTransportSubMode_allTelecabinSubmodeValuesShouldBeMapped() {
        for (TelecabinSubmodeEnumeration submode : TelecabinSubmodeEnumeration.values()) {
            TransportSubmodeStructure submodeStructure = new TransportSubmodeStructure();
            submodeStructure.setTelecabinSubmode(submode);
            verifyMapping(submodeStructure,submode.value());
        }
    }

    @Test
    public void mapTransportSubMode_allTramSubmodeValuesShouldBeMapped() {
        for (TramSubmodeEnumeration submode : TramSubmodeEnumeration.values()) {
            TransportSubmodeStructure submodeStructure = new TransportSubmodeStructure();
            submodeStructure.setTramSubmode(submode);
            verifyMapping(submodeStructure,submode.value());
        }
    }

    @Test
    public void mapTransportSubMode_allWaterSubmodeValuesShouldBeMapped() {
        for (WaterSubmodeEnumeration submode : WaterSubmodeEnumeration.values()) {
            TransportSubmodeStructure submodeStructure = new TransportSubmodeStructure();
            submodeStructure.setWaterSubmode(submode);
            verifyMapping(submodeStructure,submode.value());
        }
    }

    @Test
    public void mapTransportSubMode_allMetroSubmodeValuesShouldBeMapped() {
        for (MetroSubmodeEnumeration submode : MetroSubmodeEnumeration.values()) {
            TransportSubmodeStructure submodeStructure = new TransportSubmodeStructure();
            submodeStructure.setMetroSubmode(submode);
            verifyMapping(submodeStructure,submode.value());
        }
    }

    @Test
    public void mapTransportSubMode_allCoachSubmodeValuesShouldBeMapped() {
        for (CoachSubmodeEnumeration submode : CoachSubmodeEnumeration.values()) {
            TransportSubmodeStructure submodeStructure = new TransportSubmodeStructure();
            submodeStructure.setCoachSubmode(submode);
            verifyMapping(submodeStructure,submode.value());
        }
    }

    private void verifyMapping(TransportSubmodeStructure submodeStructure, String val) {
        if (val.equals("unknown") || val.equals("undefined")) {
            // We don't want map these.
            return;
        }

        TransmodelTransportSubmode mapped = modeMapper.getTransportSubmode(submodeStructure);
        Assert.assertNotNull("Missing transport submode mapping for: " + val, mapped);
        Assert.assertEquals("Expected mapped submode to have same value as org", val, mapped.getValue());
    }


}
