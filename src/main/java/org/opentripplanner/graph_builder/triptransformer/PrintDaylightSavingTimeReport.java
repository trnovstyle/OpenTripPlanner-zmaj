package org.opentripplanner.graph_builder.triptransformer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;



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
    private List<CsvLine> lines = new ArrayList<>();

    PrintDaylightSavingTimeReport(OtpTransitBuilder transitService) {
        this.transit = new TransitServiceDecorator(transitService);
    }

    void collectData(String tag) {
        ServiceDate dstDate = TimeUtil.DST_2019_OCT;
        lines.addAll(
                mapOfCsvLines(
                    tag,
                    transit.tripsByRoute(dstDate, transit::isBefore_04_30, true),
                    transit.tripsByRoute(TimeUtil.dayBefore(dstDate), transit::departAfter2400, false)
                )
        );
    }

    public void print(String filename) {
        printTripsOperatingInPeriod(filename, lines);
    }

    public void printTrip(ServiceDate date, AgencyAndId routeId, int departureTimeMin) {
        List<CsvLine> lines = mapOfCsvLines(
                null,
                transit.tripsByRoute(date, (t) -> filterTimeShift(t, routeId, departureTimeMin), false),
                ArrayListMultimap.create()
        );
        System.out.println("\n---------------------------------------");
        printTripsOperatingInPeriod(null, lines);
        System.out.println("---------------------------------------\n");
    }

    private boolean filterTimeShift(Trip t, AgencyAndId routeId, int depatureTime) {
        return t.getRoute().getId().equals(routeId) && transit.departureTime(t) == depatureTime;
    }

    private List<CsvLine> mapOfCsvLines(
            String tag,
            Multimap<Route, Trip> tripsByRouteDst,
            Multimap<Route, Trip> tripsByRouteDayBeforeDst
    ) {
        List<CsvLine> lines = new ArrayList<>();
        List<Route> routes = new ArrayList<>(tripsByRouteDst.keySet());
        routes.sort(new RouteComparator());

        for (Route route : routes) {
            final Multimap<String, Trip> tripBySign = mapTripByHeadsign(tripsByRouteDst.get(route));
            final Multimap<String, Trip> tripBySignDayBeforeDst = mapTripByHeadsign(tripsByRouteDayBeforeDst.get(route));

            for (String headsign : tripBySign.keySet()) {
                CsvLine csvLine = new CsvLine(tag, route, headsign);
                lines.add(csvLine);

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

    private void printTripsOperatingInPeriod(String filename, List<CsvLine> lines) {
        try {
            List<Route> routes = lines.stream()
                    .map(CsvLine::route)
                    .distinct()
                    .sorted(new RouteComparator())
                    .collect(Collectors.toList());

            PrintWriter out = filename != null
                    ? new PrintWriter(new File(filename))
                    : new PrintWriter(System.out, true);

            out.println(CsvLine.csvHeader());

            for (Route route : routes) {
                List<CsvLine> linesForRoute = lines
                        .stream()
                        .filter(l -> l.route().equals(route))
                        .collect(Collectors.toList());

                Map<String, List<CsvLine>> headsignsLines = new TreeMap<>();
                for (CsvLine line : linesForRoute) {
                    headsignsLines.putIfAbsent(line.headsign(), new ArrayList<>());
                    headsignsLines.get(line.headsign()).add(line);
                }

                List<String> headsigns = new ArrayList<>(headsignsLines.keySet());
                headsigns.sort(String::compareToIgnoreCase);

                for (String heading : headsigns) {
                    List<CsvLine> csvLines = headsignsLines.get(heading);
                    csvLines.sort(Comparator.comparing(CsvLine::tag));

                    for (CsvLine it : csvLines) {
                        out.println(it.toCsv());
                    }
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
