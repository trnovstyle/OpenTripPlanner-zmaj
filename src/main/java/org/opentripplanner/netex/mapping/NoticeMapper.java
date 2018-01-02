package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Notice;

public class NoticeMapper {

    public Notice mapNotice(org.rutebanken.netex.model.Notice netexNotice){
        Notice otpNotice = new Notice();

        otpNotice.setId(AgencyAndIdFactory.createAgencyAndId(netexNotice.getId()));
        otpNotice.setText(netexNotice.getText().getValue());
        otpNotice.setPublicCode(netexNotice.getPublicCode());

        return otpNotice;
    }
}
