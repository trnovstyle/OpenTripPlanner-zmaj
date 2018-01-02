package org.opentripplanner.model;


public class Operator extends IdentityBean<AgencyAndId> {

    private static final long serialVersionUID = 1L;

    private AgencyAndId id;

    private String name;

    private String url;

    private String phone;


    @Override
    public AgencyAndId getId() {
        return id;
    }

    public void setId(AgencyAndId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String toString() {
        return "<Agency " + this.id + ">";
    }
}
