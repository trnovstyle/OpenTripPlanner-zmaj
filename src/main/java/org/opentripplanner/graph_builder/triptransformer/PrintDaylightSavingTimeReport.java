package org.opentripplanner.graph_builder.triptransformer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This service is used to identify, print and modify services/routes that is affected by daylight saving time
 * adjustments that occour in fall and spring.
 * <p>
 * - It can be used to extract a csv file of all routes that are affected by DST.
 * - It can add/remove new trips in the DST window.
 * - It ca
 */
class PrintDaylightSavingTimeReport {
    private TransitServiceDecorator  transit;

    public PrintDaylightSavingTimeReport(OtpTransitBuilder transitService, CalendarServiceData data) {
        this.transit = new TransitServiceDecorator(transitService, data);
    }

    public void print(String filename) {
        ServiceDate dstDate = TimeUtil.DST_2019_OCT;
        Map<Route, Map<String, CsvLine>> lines = mapOfCsvLines(
                transit.tripsByRoute(dstDate, transit::isDstSummerTime, true),
                transit.tripsByRoute(TimeUtil.dayBefore(dstDate), transit::departAfter2400, false)
        );
        printTripsOperatingInPeriod(lines, filename);
    }

    public void printTrip(ServiceDate date, AgencyAndId routeId, int departureTimeMin) {
        Map<Route, Map<String, CsvLine>> lines = mapOfCsvLines(
                transit.tripsByRoute(date, (t) -> filterTimeShift(t, routeId, departureTimeMin), false),
                ArrayListMultimap.create()
        );
        System.out.println("\n---------------------------------------");
        printTripsOperatingInPeriod(lines, null);
        System.out.println("---------------------------------------\n");
    }

    private boolean filterTimeShift(Trip t, AgencyAndId routeId, int depatureTime) {
        return t.getRoute().getId().equals(routeId) && transit.departureTime(t) == depatureTime;
    }

    private Map<Route, Map<String, CsvLine>> mapOfCsvLines(
            Multimap<Route, Trip> tripsByRouteDst,
            Multimap<Route, Trip> tripsByRouteDayBeforeDst
    ) {
        Map<Route, Map<String, CsvLine>> lines = new HashMap<>();
        List<Route> routes = new ArrayList<>(tripsByRouteDst.keySet());
        routes.sort(new RouteComparator());

        for (Route route : routes) {
            final Multimap<String, Trip> tripBySign = mapTripByHeadsign(tripsByRouteDst.get(route));
            final Multimap<String, Trip> tripBySignDayBeforeDst = mapTripByHeadsign(tripsByRouteDayBeforeDst.get(route));

            Map<String, CsvLine> headsignMap = new HashMap<>();
            lines.put(route, headsignMap);

            for (String headsign : tripBySign.keySet()) {
                CsvLine csvLine = new CsvLine(route, headsign);
                headsignMap.put(headsign, csvLine);

                for (Trip trip : tripBySign.get(headsign)) {
                    csvLine.addDeparture(
                            trip,
                            transit.departureTime(trip),
                            transit.isOneDayService(trip)
                    );
                }
                Collection<Trip> tripsDayBeforeDst = tripBySignDayBeforeDst.get(headsign);
                if(tripsDayBeforeDst != null) {
                    for (Trip trip : tripsDayBeforeDst) {
                        csvLine.addDepDayBefore(
                                trip,
                                transit.departureTime(trip)
                        );
                    }
                }
            }
        }
        return lines;
    }

    private void printTripsOperatingInPeriod(Map<Route, Map<String, CsvLine>> lines, String filename) {
        List<Route> routes = new ArrayList<>(lines.keySet());
        routes.sort(new RouteComparator());

        try {
            PrintWriter out = filename != null
                    ? new PrintWriter(new File(filename))
                    : new PrintWriter(System.out, true);

            out.println(CsvLine.csvHeader());

            for (Route route : routes) {
                Map<String, CsvLine> headsignsLines = lines.get(route);
                List<String> headsigns = new ArrayList<>(headsignsLines.keySet());
                headsigns.sort(String::compareToIgnoreCase);

                for (String it : headsigns) {
                    CsvLine csvLine = headsignsLines.get(it);
                    out.println(csvLine.toCsv());
                }
            }
            if(filename != null) {
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Multimap<String, Trip> mapTripByHeadsign(Collection<Trip> trips) {
        final Multimap<String, Trip> tripBySign = ArrayListMultimap.create();
        for (Trip trip : trips) {
            tripBySign.put(trip.getTripHeadsign(), trip);
        }
        return tripBySign;
    }

}
