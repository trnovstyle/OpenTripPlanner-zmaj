/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Area extends IdentityBean<AgencyAndId> {
    private static final long serialVersionUID = 1L;

    private AgencyAndId id;

    private String wkt;

    public Area() {

    }

    public Area(Area a) {
        this.id = a.id;
        this.wkt = a.wkt;
    }


    public String getAreaId() {
        return id.getId();
    }

    public AgencyAndId getId() {
        return id;
    }

    public void setId(AgencyAndId areaId) {
        this.id = areaId;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }

    public void setWkt(List<Double> coordinates) {
        StringBuilder wktPolygon = new StringBuilder();
        wktPolygon.append("POLYGON((");
        for (int i = 0; i < coordinates.size(); i += 2) {
            wktPolygon.append(coordinates.get(i));
            wktPolygon.append(" ");
            wktPolygon.append(coordinates.get(i + 1));
            if (i < coordinates.size() - 2) {
                wktPolygon.append(", ");
            }
        }
        wktPolygon.append("))");
        this.wkt = wktPolygon.toString();
    }

    public Coordinate[] getCoordinates() {
        String[] coordstrings = wkt.split("\\(")[2].split("\\)")[0].split(" ");

        Coordinate[] coordinates = new Coordinate[coordstrings.length / 2];

        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = new Coordinate(
                    Double.parseDouble(coordstrings[i * 2].replace(",", "")),
                    Double.parseDouble(coordstrings[i * 2 + 1].replace(",", "")));
        }

        return coordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Area area = (Area) o;
        return id.equals(area.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
