package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.ValidBetween;

import java.util.Collection;
import java.util.Comparator;

/**
 * Compares validity according to the following criteria in order:
 * 1. Valid now (or no validity information)
 * 2. Future valid period with earliest start date
 * 3. Past valid period with latest end date
 *
 * Secondary sorting by version
 */

public class ValidityComparator implements Comparator<Collection<ValidBetween>> {
    private ValidityFilter validityFilter = new ValidityFilter();

    @Override
    public int compare(Collection<ValidBetween> v1collection, Collection<ValidBetween> v2collection) {
        ValidBetween v1 = v1collection.size() == 0 ? null : v1collection.stream().findFirst().get();
        ValidBetween v2 = v2collection.size() == 0 ? null : v2collection.stream().findFirst().get();

        Boolean validNow1 = validityFilter.isValidNow(v1);
        Boolean validNow2 = validityFilter.isValidNow(v2);
        if (validNow1 && !validNow2) return -1;
        if (validNow2 && !validNow1) return 1;
        if (validNow1 && validNow2) return 0;

        Boolean validFuture1 = validityFilter.isValidFuture(v1);
        Boolean validFuture2 = validityFilter.isValidFuture(v2);
        if (validFuture1 && !validFuture2) return -1;
        if (validFuture2 && !validFuture1) return 1;
        if (validFuture1 && validFuture2) {
            return v1.getFromDate().compareTo(v2.getFromDate());
        }

        Boolean validPast1 = validityFilter.isValidPast(v1);
        Boolean validPast2 = validityFilter.isValidPast(v2);
        if (validPast1 && !validPast2) return -1;
        if (validPast2 && !validPast1) return 1;
        if (validPast1 && validPast2) {
            return v2.getToDate().compareTo(v1.getToDate());
        }

        return 0;
    }
}