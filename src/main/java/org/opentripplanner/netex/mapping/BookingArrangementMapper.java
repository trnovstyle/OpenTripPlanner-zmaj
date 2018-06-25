package org.opentripplanner.netex.mapping;

import org.apache.commons.collections.CollectionUtils;
import org.opentripplanner.model.BookingArrangement;
import org.rutebanken.netex.model.BookingAccessEnumeration;
import org.rutebanken.netex.model.BookingMethodEnumeration;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PurchaseMomentEnumeration;
import org.rutebanken.netex.model.PurchaseWhenEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class BookingArrangementMapper {

    private static final Logger LOG = LoggerFactory.getLogger(BookingArrangementMapper.class);

    private ContactStructureMapper contactStructureMapper = new ContactStructureMapper();

    public BookingArrangement mapBookingArrangement(ContactStructure bookingContact, MultilingualString bookingNote, BookingAccessEnumeration bookingAccess, PurchaseWhenEnumeration bookWhen,
                                                          List<PurchaseMomentEnumeration> buyWhen, List<BookingMethodEnumeration> bookingMethods, Duration minimumBookingPeriod,
                                                          LocalTime latestBookingTime) {
        BookingArrangement otpBookingArrangement = new BookingArrangement();

        otpBookingArrangement.setBookingContact(contactStructureMapper.mapContactStructure(bookingContact));
        if (bookingNote != null) {
            otpBookingArrangement.setBookingNote(bookingNote.getValue());
        }

        otpBookingArrangement.setBookWhen(mapPurchaseWhen(bookWhen));

        otpBookingArrangement.setBookingAccess(mapBookingAccess(bookingAccess));
        if (!CollectionUtils.isEmpty(buyWhen)) {
            otpBookingArrangement.setBuyWhen(buyWhen.stream().map(bw -> mapPurchaseMoment(bw)).collect(Collectors.toList()));
        }
        if (!CollectionUtils.isEmpty(bookingMethods)) {
            otpBookingArrangement.setBookingMethods(bookingMethods.stream().map(bm -> mapBookingMethod(bm)).collect(Collectors.toList()));
        }
        otpBookingArrangement.setLatestBookingTime(latestBookingTime);
        otpBookingArrangement.setMinimumBookingPeriod(minimumBookingPeriod);
        return otpBookingArrangement;
    }


    public BookingArrangement.BookingAccessEnum mapBookingAccess(BookingAccessEnumeration netexValue) {
        BookingArrangement.BookingAccessEnum otpValue = null;
        if (netexValue != null) {
            if (BookingAccessEnumeration.PUBLIC.equals(netexValue)) {
                otpValue = BookingArrangement.BookingAccessEnum.publicAccess;
            } else {
                try {
                    otpValue = BookingArrangement.BookingAccessEnum.valueOf(netexValue.value());
                } catch (IllegalArgumentException iae) {
                    LOG.warn("Unable to map unknown NeTEx BookingAccess value: " + netexValue);
                }
            }
        }
        return otpValue;
    }

    public BookingArrangement.PurchaseWhenEnum mapPurchaseWhen(PurchaseWhenEnumeration netexValue) {
        BookingArrangement.PurchaseWhenEnum otpValue = null;
        if (netexValue != null) {
            try {
                otpValue = BookingArrangement.PurchaseWhenEnum.valueOf(netexValue.value());
            } catch (IllegalArgumentException iae) {
                LOG.warn("Unable to map unknown NeTEx PurchaseWhenEnum value: " + netexValue);
            }

        }
        return otpValue;
    }

    public BookingArrangement.PurchaseMomentEnum mapPurchaseMoment(PurchaseMomentEnumeration netexValue) {
        BookingArrangement.PurchaseMomentEnum otpValue = null;
        if (netexValue != null) {
            try {
                otpValue = BookingArrangement.PurchaseMomentEnum.valueOf(netexValue.value());
            } catch (IllegalArgumentException iae) {
                LOG.warn("Unable to map unknown NeTEx PurchaseMomentEnum value: " + netexValue);
            }

        }
        return otpValue;
    }

    public BookingArrangement.BookingMethodEnum mapBookingMethod(BookingMethodEnumeration netexValue) {
        BookingArrangement.BookingMethodEnum otpValue = null;
        if (netexValue != null) {
            try {
                otpValue = BookingArrangement.BookingMethodEnum.valueOf(netexValue.value());
            } catch (IllegalArgumentException iae) {
                LOG.warn("Unable to map unknown NeTEx BookingMethodEnum value: " + netexValue);
            }

        }
        return otpValue;
    }
}
