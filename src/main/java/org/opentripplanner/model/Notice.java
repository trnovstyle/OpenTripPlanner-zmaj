package org.opentripplanner.model;

public class Notice extends IdentityBean<AgencyAndId> {

    private static final long serialVersionUID = 1L;

    private AgencyAndId id;

    private String text;

    private String publicCode;

    public AgencyAndId getId() {
        return id;
    }

    public void setId(AgencyAndId id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPublicCode() {
        return publicCode;
    }

    public void setPublicCode(String publicCode) {
        this.publicCode = publicCode;
    }
}