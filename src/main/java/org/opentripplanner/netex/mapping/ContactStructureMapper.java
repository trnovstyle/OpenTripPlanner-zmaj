package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Contact;
import org.rutebanken.netex.model.ContactStructure;

public class ContactStructureMapper {


    public Contact mapContactStructure(ContactStructure netexContact) {
        if (netexContact == null) {
            return null;
        }

        Contact otpContact = new Contact();
        if (netexContact.getContactPerson() != null) {
            otpContact.setContactPerson(netexContact.getContactPerson().getValue());
        }
        otpContact.setEmail(netexContact.getEmail());
        if (netexContact.getFurtherDetails() != null) {
            otpContact.setFurtherDetails(netexContact.getFurtherDetails().getValue());
        }
        otpContact.setPhone(netexContact.getPhone());
        otpContact.setUrl(netexContact.getUrl());
        return otpContact;
    }
}
