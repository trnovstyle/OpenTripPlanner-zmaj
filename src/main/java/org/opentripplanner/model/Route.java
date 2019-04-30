/*
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentripplanner.model;

import java.util.List;

public final class Route extends IdentityBean<AgencyAndId> {

    private static final long serialVersionUID = 1L;

    private static final int MISSING_VALUE = -999;

    private AgencyAndId id;

    private Agency agency;

    private Operator operator;

    private String shortName;

    private String longName;

    private int type;

    private TransmodelTransportSubmode transportSubmode;

    private String desc;

    private String url;

    private String color;

    private String textColor;

    private List<KeyValue> keyValues;

    private BookingArrangement bookingArrangements;

    private FlexibleRouteTypeEnum flexibleRouteType;

    @Deprecated private int routeBikesAllowed = 0;

    /**
     * 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    private int bikesAllowed = 0;

    private int sortOrder = MISSING_VALUE;

    private String brandingUrl;

    private int eligibilityRestricted = MISSING_VALUE;

    public AgencyAndId getId() {
        return id;
    }

    public void setId(AgencyAndId id) {
        this.id = id;
    }

    /**
     * The 'agency' property represent a GTFS Agency and NeTEx the Authority. Note that Agency does NOT map
     * 1-1 to Authority, it is rather a mix between Authority and Operator.
     */
    public Agency getAgency() {
        return agency;
    }

    public void setAgency(Agency agency) {
        this.agency = agency;
    }

    /**
     * NeTEx Operator, not in use when importing GTFS files.
     */
    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public TransmodelTransportSubmode getTransportSubmode() {
        return transportSubmode;
    }

    public void setTransportSubmode(TransmodelTransportSubmode transportSubmode) {
        this.transportSubmode = transportSubmode;
    }

    @Deprecated
    public int getRouteBikesAllowed() {
        return routeBikesAllowed;
    }

    @Deprecated
    public void setRouteBikesAllowed(int routeBikesAllowed) {
        this.routeBikesAllowed = routeBikesAllowed;
    }

    /**
     * @return 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    public int getBikesAllowed() {
        return bikesAllowed;
    }

    /**
     * @param bikesAllowed 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes
     *          NOT allowed
     */
    public void setBikesAllowed(int bikesAllowed) {
        this.bikesAllowed = bikesAllowed;
    }

    public boolean isSortOrderSet() {
        return sortOrder != MISSING_VALUE;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getBrandingUrl() {
        return brandingUrl;
    }

    public void setBrandingUrl(String brandingUrl) {
        this.brandingUrl = brandingUrl;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public List<KeyValue> getKeyValues() {
        return keyValues;
    }

    public void setKeyValues(List<KeyValue> keyValues) {
        this.keyValues = keyValues;
    }

    public BookingArrangement getBookingArrangements() {
        return bookingArrangements;
    }

    public void setBookingArrangements(BookingArrangement bookingArrangements) {
        this.bookingArrangements = bookingArrangements;
    }

    public FlexibleRouteTypeEnum getFlexibleRouteType() {
        return flexibleRouteType;
    }

    public void setFlexibleRouteType(FlexibleRouteTypeEnum flexibleRouteType) {
        this.flexibleRouteType = flexibleRouteType;
    }

    public boolean hasEligibilityRestricted() {
        return eligibilityRestricted != MISSING_VALUE;
    }

    public int getEligibilityRestricted() {
        return eligibilityRestricted;
    }

    public void setEligibilityRestricted(int eligibilityRestricted) {
        this.eligibilityRestricted = eligibilityRestricted;
    }

    @Override
    public String toString() {
        return "<Route " + id + " " + shortName + ">";
    }

    public enum FlexibleRouteTypeEnum {
        corridorService,
        mainRouteWithFlexibleEnds,
        flexibleAreasOnly,
        hailAndRideSections,
        fixedStopAreaWide,
        freeAreaAreaWide,
        mixedFlexible,
        mixedFlexibleAndFixed,
        fixed,
        other
    }

}
