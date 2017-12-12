package org.opentripplanner.model;

public class NoticeAssignment extends IdentityBean<AgencyAndId> {

    private static final long serialVersionUID = 1L;

    private AgencyAndId id;

    private AgencyAndId noticeId;

    private AgencyAndId elementId;

    public AgencyAndId getId() {
        return id;
    }

    public void setId(AgencyAndId id) {
        this.id = id;
    }

    public AgencyAndId getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(AgencyAndId noticeId) {
        this.noticeId = noticeId;
    }

    public AgencyAndId getElementId() {
        return elementId;
    }

    public void setElementId(AgencyAndId elementId) {
        this.elementId = elementId;
    }
}