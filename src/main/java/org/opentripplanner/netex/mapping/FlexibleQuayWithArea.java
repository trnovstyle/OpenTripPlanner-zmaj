package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Area;
import org.opentripplanner.model.IdentityBean;
import org.opentripplanner.model.Stop;

import java.util.Objects;

public class FlexibleQuayWithArea extends IdentityBean<AgencyAndId> {
    final Stop stop;
    final Area area;

    FlexibleQuayWithArea(Stop stop, Area area) {
        this.stop = stop;
        this.area = area;
    }

    @Override
    public AgencyAndId getId() {
        return stop.getId();
    }

    @Override
    public void setId(AgencyAndId id) {
        stop.setId(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlexibleQuayWithArea that = (FlexibleQuayWithArea) o;
        return Objects.equals(stop, that.stop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stop);
    }
}
