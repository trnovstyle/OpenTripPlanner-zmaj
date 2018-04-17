package org.opentripplanner.model;

/**
 * Generic marketing classification.
 */
public class Branding  extends IdentityBean<AgencyAndId> {

    private static final long serialVersionUID = 1L;

    private AgencyAndId id;

    private String name;

    private String url;

    /**
     * URL to image file for branding.
     */
    private String image;

    private String description;

    @Override
    public AgencyAndId getId() {
        return id;
    }

    @Override
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String toString() {
        return "<Branding " + this.id + ">";
    }
}
