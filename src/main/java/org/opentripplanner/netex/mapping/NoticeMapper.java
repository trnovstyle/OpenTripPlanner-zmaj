package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Notice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoticeMapper {
    private static final Logger LOG = LoggerFactory.getLogger(AgencyMapper.class);

    public Notice mapNotice(org.rutebanken.netex.model.Notice netexNotice){
        Notice otpNotice = new Notice();

        otpNotice.setId(AgencyAndIdFactory.getAgencyAndId(netexNotice.getId()));
        otpNotice.setText(netexNotice.getText().getValue());
        otpNotice.setPublicCode(netexNotice.getPublicCode());

        return otpNotice;
    }
}
