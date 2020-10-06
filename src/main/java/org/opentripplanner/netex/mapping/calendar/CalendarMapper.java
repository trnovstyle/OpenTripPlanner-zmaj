/*
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.opentripplanner.netex.mapping.calendar;

import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.PropertyOfDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


// TODO TGR - Add Unit tests
public class CalendarMapper {
    private static final Logger LOG = LoggerFactory.getLogger(CalendarMapper.class);

    public static Collection<ServiceCalendarDate> mapToCalendarDates(AgencyAndId serviceId, NetexDao netexDao) {

        String[] dayTypeIds = serviceId.getId().split("\\+");

        Collection<ServiceCalendarDate> allServiceCalendarDates = new HashSet<>();

        for(String dayTypeId : dayTypeIds) {
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
            allServiceCalendarDates.addAll(
                    serviceCalendarDatesForDayType
                            .stream()
                            .map(d -> mapServiceCalendarDate(d, serviceId, ServiceCalendarDate.EXCEPTION_TYPE_ADD))
                            .collect(Collectors.toList())
            );
        }

        if (allServiceCalendarDates.isEmpty()) {
            LOG.warn("ServiceCode " + serviceId + " does not contain any serviceDates");
            // Add one date exception when list is empty to ensure serviceId is not lost
            allServiceCalendarDates.add(mapServiceCalendarDate(LocalDate.now().atStartOfDay(), serviceId, 2));
        }

        return allServiceCalendarDates;
    }

    private static ServiceCalendarDate mapServiceCalendarDate(LocalDateTime date, AgencyAndId serviceId, Integer exceptionType) {
        ServiceCalendarDate serviceCalendarDate = new ServiceCalendarDate();
        serviceCalendarDate.setServiceId(serviceId);
        serviceCalendarDate.setDate(new ServiceDate(date.toLocalDate()));
        serviceCalendarDate.setExceptionType(exceptionType);
        return serviceCalendarDate;
    }
}