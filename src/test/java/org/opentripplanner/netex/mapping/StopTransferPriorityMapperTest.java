package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.StopTransferPriority;
import org.rutebanken.netex.model.InterchangeWeightingEnumeration;

import static org.junit.Assert.*;

public class StopTransferPriorityMapperTest {

  @SuppressWarnings("ConstantConditions")
  @Test
  public void mapToDomain() {

    assertNull(TransferPriorityMapper.mapToDomain(null));

    assertEquals(
        StopTransferPriority.DISCOURAGED,
        TransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.NO_INTERCHANGE)
    );
    assertEquals(
        StopTransferPriority.ALLOWED,
        TransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.INTERCHANGE_ALLOWED)
    );
    assertEquals(
        StopTransferPriority.PREFERRED,
        TransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.PREFERRED_INTERCHANGE)
    );
    assertEquals(
        StopTransferPriority.RECOMMENDED,
        TransferPriorityMapper.mapToDomain(InterchangeWeightingEnumeration.RECOMMENDED_INTERCHANGE)
    );
  }
}