package org.opentripplanner.model;


import org.apache.commons.collections.CollectionUtils;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

public class BookingArrangement implements Serializable {

    private static final long serialVersionUID = 1L;

    private BookingAccessEnum bookingAccess;

    private PurchaseWhenEnum bookWhen;

    private LocalTime latestBookingTime;

    private Duration minimumBookingPeriod;

    private List<PurchaseMomentEnum> buyWhen;

    private List<BookingMethodEnum> bookingMethods;

    private String bookingNote;

    private Contact bookingContact;

    public BookingAccessEnum getBookingAccess() {
        return bookingAccess;
    }

    public void setBookingAccess(BookingAccessEnum bookingAccess) {
        this.bookingAccess = bookingAccess;
    }

    public PurchaseWhenEnum getBookWhen() {
        return bookWhen;
    }

    public void setBookWhen(PurchaseWhenEnum bookWhen) {
        this.bookWhen = bookWhen;
    }

    public LocalTime getLatestBookingTime() {
        return latestBookingTime;
    }

    public void setLatestBookingTime(LocalTime latestBookingTime) {
        this.latestBookingTime = latestBookingTime;
    }

    public Duration getMinimumBookingPeriod() {
        return minimumBookingPeriod;
    }

    public void setMinimumBookingPeriod(Duration minimumBookingPeriod) {
        this.minimumBookingPeriod = minimumBookingPeriod;
    }

    public List<PurchaseMomentEnum> getBuyWhen() {
        return buyWhen;
    }

    public void setBuyWhen(List<PurchaseMomentEnum> buyWhen) {
        this.buyWhen = buyWhen;
    }

    public String getBookingNote() {
        return bookingNote;
    }

    public void setBookingNote(String bookingNote) {
        this.bookingNote = bookingNote;
    }

    public Contact getBookingContact() {
        return bookingContact;
    }

    public void setBookingContact(Contact bookingContact) {
        this.bookingContact = bookingContact;
    }

    public List<BookingMethodEnum> getBookingMethods() {
        return bookingMethods;
    }

    public void setBookingMethods(List<BookingMethodEnum> bookingMethods) {
        this.bookingMethods = bookingMethods;
    }

    public BookingArrangement copy(){
        BookingArrangement copy=new BookingArrangement();
        copy.setMinimumBookingPeriod(this.minimumBookingPeriod);
        copy.setLatestBookingTime(this.latestBookingTime);
        copy.setBookingAccess(this.bookingAccess);
        copy.setBookingContact(this.bookingContact);
        copy.setBookingNote(this.bookingNote);
        copy.setBookWhen(this.bookWhen);
        copy.setBuyWhen(this.buyWhen);
        copy.setBookingMethods(this.bookingMethods);
        return copy;
    }


    /**
     * Copy values from overriding BookingArrangement where values are not null.
     */
    public void addOverrides(BookingArrangement override) {
        if (override != null) {
            if (override.getBookingAccess() != null) {
                this.setBookingAccess(override.getBookingAccess());
            }
            if (override.getBookWhen() != null) {
                this.setBookWhen(override.getBookWhen());
            }
            if (override.getBookingContact() != null) {
                this.setBookingContact(override.getBookingContact());
            }
            if (override.getBookingNote() != null) {
                this.setBookingNote(override.getBookingNote());
            }
            if (!CollectionUtils.isEmpty(override.getBuyWhen())) {
                this.setBuyWhen(override.getBuyWhen());
            }
            if (!CollectionUtils.isEmpty(override.getBookingMethods())) {
                this.setBookingMethods(override.getBookingMethods());
            }
            if (override.getMinimumBookingPeriod() != null) {
                this.setMinimumBookingPeriod(override.getMinimumBookingPeriod());
            }
            if (override.getLatestBookingTime() != null) {
                this.setLatestBookingTime(override.getLatestBookingTime());
            }
        }
    }

    public enum BookingAccessEnum {

        publicAccess,
        authorisedPublic,
        staff,
        other,
    }


    public enum PurchaseWhenEnum {
        timeOfTravelOnly,
        dayOfTravelOnly,
        untilPreviousDay,
        advanceOnly,
        advanceAndDayOfTravel,
        other
    }

    public enum PurchaseMomentEnum {
        onReservation,
        beforeBoarding,
        onBoarding,
        afterBoarding,
        onCheckOut,
        other

    }

    public enum BookingMethodEnum {
        callDriver,
        callOffice,
        online,
        other,
        phoneAtStop,
        text,
        none;
    }
}
