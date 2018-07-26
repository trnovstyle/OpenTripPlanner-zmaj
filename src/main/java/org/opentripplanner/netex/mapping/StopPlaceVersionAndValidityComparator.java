package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.StopPlace;

import java.util.Comparator;

public class StopPlaceVersionAndValidityComparator implements Comparator<StopPlace> {
    private ValidityComparator validityComparator = new ValidityComparator();

    @Override
    public int compare(StopPlace s1, StopPlace s2) {
        int compareValue = validityComparator.compare(s1.getValidBetween(), s2.getValidBetween());

        // If both are equally valid, sort by version
        if (compareValue == 0) {
            return Integer.compare(Integer.parseInt(s2.getVersion()), Integer.parseInt(s1.getVersion()));
        } else {
            return compareValue;
        }
    }
}
