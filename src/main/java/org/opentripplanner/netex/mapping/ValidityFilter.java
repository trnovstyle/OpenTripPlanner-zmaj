package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.ValidBetween;

import java.time.LocalDateTime;

public class ValidityFilter {
    // No validity information treated as valid now
    public boolean isValidNow(ValidBetween validBetween) {
        LocalDateTime now = LocalDateTime.now();
        if (validBetween != null) {
            if (validBetween.getFromDate() == null && validBetween.getToDate() == null) {
                return true;
            }

            if (validBetween.getFromDate() != null && validBetween.getFromDate().isAfter(now)) {
                return false;
            }

            if (validBetween.getToDate() != null && validBetween.getToDate().isBefore(now)) {
                return false;
            }

            return true;
        }
        return true;
    }

    public boolean isValidPast(ValidBetween validBetween) {
        LocalDateTime now = LocalDateTime.now();
        if (validBetween != null) {
            if (validBetween.getToDate() == null) {
                return false;
            }

            if (validBetween.getToDate() != null && validBetween.getToDate().isAfter(now)) {
                return false;
            }

            return true;
        }
        return false;
    }

    public boolean isValidFuture(ValidBetween validBetween) {
        LocalDateTime now = LocalDateTime.now();
        if (validBetween != null) {
            if (validBetween.getFromDate() == null) {
                return false;
            }

            if (validBetween.getFromDate() != null && validBetween.getFromDate().isBefore(now)) {
                return false;
            }

            return true;
        }
        return false;
    }
}