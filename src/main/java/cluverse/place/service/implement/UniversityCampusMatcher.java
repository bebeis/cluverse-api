package cluverse.place.service.implement;

import cluverse.place.domain.PlaceCandidate;
import cluverse.university.domain.UniversityCampus;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class UniversityCampusMatcher {

    private static final double EARTH_RADIUS_METER = 6_371_000.0;

    private UniversityCampusMatcher() {
    }

    public static Optional<UniversityCampus> findNearestInRadius(
            PlaceCandidate candidate,
            List<UniversityCampus> campuses
    ) {
        return campuses.stream()
                .map(campus -> new CampusDistance(campus, distanceMeter(candidate, campus)))
                .filter(value -> value.distanceMeter() <= value.campus().getLocalRadiusMeter())
                .min(Comparator.comparingDouble(CampusDistance::distanceMeter))
                .map(CampusDistance::campus);
    }

    static double distanceMeter(PlaceCandidate candidate, UniversityCampus campus) {
        double latitude1 = Math.toRadians(candidate.latitude().doubleValue());
        double latitude2 = Math.toRadians(campus.getLatitude().doubleValue());
        double latitudeDelta = latitude2 - latitude1;
        double longitudeDelta = Math.toRadians(
                campus.getLongitude().doubleValue() - candidate.longitude().doubleValue());
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(latitude1) * Math.cos(latitude2)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 2 * EARTH_RADIUS_METER * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private record CampusDistance(UniversityCampus campus, double distanceMeter) {
    }
}
