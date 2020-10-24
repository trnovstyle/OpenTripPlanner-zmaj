package org.opentripplanner.netex.mapping.calendar;

import javax.validation.constraints.NotNull;
import java.util.Objects;

class DayTypeAndServiceJourneyId {
    private final String dayTypeId;
    private final String sjId;

    public DayTypeAndServiceJourneyId(@NotNull String dayTypeId, @NotNull String sjId) {
        this.dayTypeId = dayTypeId;
        this.sjId = sjId;
    }

    public String dayTypeId() {
        return dayTypeId;
    }

    public String serviceJourneyId() {
        return sjId;
    }

    @Override
    public String toString() {
        return "[dt=" + dayTypeId + ", sj=" + sjId + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DayTypeAndServiceJourneyId that = (DayTypeAndServiceJourneyId) o;
        return dayTypeId.equals(that.dayTypeId) &&
                sjId.equals(that.sjId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayTypeId, sjId);
    }
}
