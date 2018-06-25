package org.opentripplanner.netex.mapping;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.model.BookingArrangement;
import org.opentripplanner.model.Contact;
import org.rutebanken.netex.model.BookingAccessEnumeration;
import org.rutebanken.netex.model.BookingMethodEnumeration;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PurchaseMomentEnumeration;
import org.rutebanken.netex.model.PurchaseWhenEnumeration;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;

public class BookingArrangementMapperTest {

    BookingArrangementMapper bookingArrangementMapper = new BookingArrangementMapper();

    @Test
    public void testAllValues() {
        ContactStructure bookingContact = new ContactStructure().withContactPerson(new MultilingualString().withValue("Contact Person"))
                                                  .withEmail("contactEmail").withFax("contactFax").withFurtherDetails(new MultilingualString().withValue("contactFurtherDetails"))
                                                  .withPhone("contactPhone").withUrl("contactUrl");

        MultilingualString bookingNote = new MultilingualString().withValue("Booking Note for test");

        Duration minBookingPeriod = Duration.ofHours(2);
        LocalTime latestBookingTime = LocalTime.of(16, 30);

        BookingArrangement mapped = bookingArrangementMapper.mapBookingArrangement(bookingContact, bookingNote, BookingAccessEnumeration.PUBLIC, PurchaseWhenEnumeration.ADVANCE_ONLY, Arrays.asList(PurchaseMomentEnumeration.AFTER_BOARDING, PurchaseMomentEnumeration.ON_BOARDING),
                Arrays.asList(BookingMethodEnumeration.CALL_DRIVER, BookingMethodEnumeration.ONLINE), minBookingPeriod, latestBookingTime);

        Assert.assertNotNull(mapped);

        Contact mappedContact = mapped.getBookingContact();
        Assert.assertNotNull(mappedContact);

        Assert.assertEquals(bookingContact.getContactPerson().getValue(), mappedContact.getContactPerson());
        Assert.assertEquals(bookingContact.getEmail(), mappedContact.getEmail());
        Assert.assertEquals(bookingContact.getFurtherDetails().getValue(), mappedContact.getFurtherDetails());
        Assert.assertEquals(bookingContact.getPhone(), mappedContact.getPhone());
        Assert.assertEquals(bookingContact.getUrl(), mappedContact.getUrl());

        Assert.assertEquals(BookingArrangement.BookingAccessEnum.publicAccess, mapped.getBookingAccess());
        Assert.assertEquals(BookingArrangement.PurchaseWhenEnum.advanceOnly, mapped.getBookWhen());
        Assert.assertEquals(Arrays.asList(BookingArrangement.PurchaseMomentEnum.afterBoarding, BookingArrangement.PurchaseMomentEnum.onBoarding), mapped.getBuyWhen());
        Assert.assertEquals(Arrays.asList(BookingArrangement.BookingMethodEnum.callDriver, BookingArrangement.BookingMethodEnum.online), mapped.getBookingMethods());
        Assert.assertEquals(minBookingPeriod, mapped.getMinimumBookingPeriod());
        Assert.assertEquals(latestBookingTime, mapped.getLatestBookingTime());
    }


    @Test
    public void testNoValues() {
        BookingArrangement mapped = bookingArrangementMapper.mapBookingArrangement(null, null, null, null, new ArrayList<>(), new ArrayList<>(), null, null);
        Assert.assertNotNull(mapped);
    }

    @Test
    public void testAllBookingMethodsHaveMapping() {
        Arrays.stream(BookingMethodEnumeration.values()).forEach(netex -> Assert.assertNotNull(bookingArrangementMapper.mapBookingMethod(netex)));
    }


    @Test
    public void testAllPurchaseMomentsHaveMapping() {
        Arrays.stream(PurchaseMomentEnumeration.values()).forEach(netex -> Assert.assertNotNull(bookingArrangementMapper.mapPurchaseMoment(netex)));
    }


    @Test
    public void testAllPurchaseWhenHaveMapping() {
        Arrays.stream(PurchaseWhenEnumeration.values()).forEach(netex -> Assert.assertNotNull(bookingArrangementMapper.mapPurchaseWhen(netex)));
    }


    @Test
    public void testAllBookingAccessHaveMapping() {
        Arrays.stream(BookingAccessEnumeration.values()).forEach(netex -> Assert.assertNotNull(bookingArrangementMapper.mapBookingAccess(netex)));
    }
}
