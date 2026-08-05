package cluverse.certification.service.implement;

import cluverse.certification.client.CertificationScheduleClient;
import cluverse.certification.domain.CertificationDeadline;
import cluverse.certification.domain.CertificationSchedule;
import cluverse.certification.properties.CertificationProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class CertificationScheduleReader {

    private static final int MAX_CACHED_YEARS = 2;

    private final CertificationScheduleClient scheduleClient;
    private final Cache<Integer, List<CertificationSchedule>> scheduleCache;

    public CertificationScheduleReader(
            CertificationScheduleClient scheduleClient,
            CertificationProperties properties
    ) {
        this.scheduleClient = scheduleClient;
        this.scheduleCache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHED_YEARS)
                .expireAfterWrite(properties.cacheTtl())
                .build();
    }

    public List<CertificationDeadline> readUpcomingDeadlines(LocalDate date, int size) {
        return Stream.of(date.getYear(), date.getYear() + 1)
                .flatMap(year -> readAnnualSchedules(year).stream())
                .flatMap(schedule -> schedule.registrationDeadlinesOnOrAfter(date).stream())
                .sorted(Comparator.comparing(CertificationDeadline::registrationEndDate)
                        .thenComparing(CertificationDeadline::qualificationType)
                        .thenComparing(CertificationDeadline::description)
                        .thenComparing(CertificationDeadline::phase))
                .limit(size)
                .toList();
    }

    private List<CertificationSchedule> readAnnualSchedules(int year) {
        return scheduleCache.get(year, scheduleClient::readSchedules);
    }

    public void evictAll() {
        scheduleCache.invalidateAll();
        scheduleCache.cleanUp();
    }
}
