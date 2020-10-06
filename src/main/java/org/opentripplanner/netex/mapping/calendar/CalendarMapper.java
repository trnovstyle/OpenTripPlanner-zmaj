package org.opentripplanner.netex.mapping.calendar;

import org.glassfish.jersey.internal.util.Producer;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.TripServiceAlteration;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.loader.NetexDao;
import org.opentripplanner.netex.mapping.TripServiceAlterationMapper;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.PropertyOfDay;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


// TODO TGR - Add Unit tests
public class CalendarMapper {
    public static Map<String, Set<ServiceDate>> mapDayTypesToLocalDates(NetexDao netexDao) {
        Map<String, Set<ServiceDate>> result = new HashMap<>();
        for (String dayTypeId : netexDao.dayTypeById.keys()) {
            DayType dayType = netexDao.dayTypeById.lookup(dayTypeId);
            Collection<LocalDateTime> serviceCalendarDatesForDayType = new HashSet<>();
            Collection<LocalDateTime> serviceCalendarDatesRemoveForDayType = new HashSet<>();

            for (DayTypeAssignment dayTypeAssignment : netexDao.dayTypeAssignmentByDayTypeId.lookup(dayTypeId)) {
                boolean available = dayTypeAssignment.isIsAvailable() == null || dayTypeAssignment.isIsAvailable();

                // Add or remove single days
                if (dayTypeAssignment.getDate() != null) {
                    LocalDateTime date = dayTypeAssignment.getDate();
                    if (available) {
                        serviceCalendarDatesForDayType.add(date);
                    } else {
                        serviceCalendarDatesRemoveForDayType.add(date);
                    }
                }
                // Add or remove periods
                else if (dayTypeAssignment.getOperatingPeriodRef() != null &&
                        netexDao.operatingPeriodById.containsKey(dayTypeAssignment.getOperatingPeriodRef().getRef())) {

                    OperatingPeriod operatingPeriod = netexDao.operatingPeriodById.lookup(dayTypeAssignment.getOperatingPeriodRef().getRef());
                    LocalDateTime fromDate = operatingPeriod.getFromDate();
                    LocalDateTime toDate = operatingPeriod.getToDate();

                    EnumSet<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);

                    if (dayType.getProperties() != null) {
                        List<PropertyOfDay> propertyOfDays = dayType.getProperties().getPropertyOfDay();
                        for (PropertyOfDay property : propertyOfDays) {
                            daysOfWeek.addAll(DayOfWeekMapper.mapDayOfWeek(property.getDaysOfWeek()));
                        }
                    }

                    for (LocalDateTime date = fromDate; date.isBefore(toDate.plusDays(1)); date = date.plusDays(1)) {
                        // Every day
                        if (daysOfWeek.size() == 7) {
                            serviceCalendarDatesForDayType.add(date);
                        } else {
                            if(daysOfWeek.contains(date.getDayOfWeek())) {
                                if (available) {
                                    serviceCalendarDatesForDayType.add(date);
                                } else {
                                    serviceCalendarDatesRemoveForDayType.add(date);
                                }
                            }
                        }
                    }
                }
            }
            serviceCalendarDatesForDayType.removeAll(serviceCalendarDatesRemoveForDayType);
            Set<ServiceDate> existing = result.get(dayTypeId);
            Set<ServiceDate> newDates = serviceCalendarDatesForDayType
                            .stream()
                            .map(d -> new ServiceDate(d.toLocalDate()))
                            .collect(Collectors.toSet());

            if(existing == null) {
                result.put(dayTypeId, newDates);
            }
            else {
                existing.addAll(newDates);
            }
        }
        return result;
    }

    public static Map<Collection<ServiceDate>, AgencyAndId> mapDatesToServiceId(
            Collection<Set<ServiceDate>> serviceDates,
            Producer<AgencyAndId> serviceIdGenerator
    ) {
        Map<Collection<ServiceDate>, AgencyAndId> serviceIds = new HashMap<>();
        for (Collection<ServiceDate> dates : serviceDates) {
            serviceIds.put(dates, serviceIdGenerator.call());
        }
        return serviceIds;
    }

    static Map<String, Set<ServiceDate>> createDatedServiceJourneyCalendar(NetexDao netexDao) {
        Map<String, TripServiceAlteration> alternations = new HashMap<>();
        Map<String, Set<ServiceDate>> map = new HashMap<>();

        for (DatedServiceJourney dsj : netexDao.datedServiceJourneyById.values()) {
            var sjId = dsj.getJourneyRef().get(0).getValue().getRef();
            var date = netexDao.operatingDaysById.lookup(dsj.getOperatingDayRef().getRef()).getCalendarDate();
            var serviceDate = new ServiceDate(date.toLocalDate());
            map.computeIfAbsent(sjId, k -> new HashSet<>()).add(serviceDate);
        }
        return map;
        }

    static Map<String, TripServiceAlteration> tripServiceAlterationsBySJId(NetexDao netexDao) {
        Map<String, TripServiceAlteration> alternations = new HashMap<>();

        for (DatedServiceJourney dsj : netexDao.datedServiceJourneyById.values()) {
            var sjId = dsj.getJourneyRef().get(0).getValue().getRef();
            var alt = mapAlterationWithDefaultPlanned(dsj.getServiceAlteration());

            if(alternations.containsKey(sjId)) {
                if(alternations.get(sjId) != alt) {
                    throw new IllegalStateException(
                            "ERROR! Service alternation miss-match for SJ=" + sjId
                                    + "(" + alt + "), expected: " + alternations.get(sjId)
                    );
        }
            }
            else {
                alternations.put(sjId, alt == null ? TripServiceAlteration.planned : alt);
            }
        }
        return alternations;
    }


    public static Collection<ServiceCalendarDate> mapToCalendarDates(
            String feedId, Collection<DatedServiceJourney> datedSJs, NetexDao netexDao
    ) {
        var dates = new HashSet<ServiceCalendarDate>();

        for (DatedServiceJourney dsj : datedSJs) {
            // Generate a service id
            var serviceId = new AgencyAndId(feedId, "DSJ-" + dsj.getJourneyRef().get(0).getValue().getRef());
            dates.add(
                    createServiceCalendarDate(
                            netexDao.operatingDaysById.lookup(dsj.getOperatingDayRef().getRef()).getCalendarDate(),
                            serviceId,
                            ServiceCalendarDate.EXCEPTION_TYPE_ADD
                    )
            );
        }
        return dates;
    }

    private static ServiceCalendarDate createServiceCalendarDate(LocalDateTime date, AgencyAndId serviceId, Integer exceptionType) {
        return new ServiceCalendarDate(
                serviceId,
                new ServiceDate(date.toLocalDate()),
                exceptionType
        );
    }

    public static TripServiceAlteration mapAlterationWithDefaultPlanned(ServiceAlterationEnumeration netexValue) {
        if (netexValue == null) {
            return TripServiceAlteration.planned;
        }
        return TripServiceAlterationMapper.mapAlteration(netexValue);
    }
}