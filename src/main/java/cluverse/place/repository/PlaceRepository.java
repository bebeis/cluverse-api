package cluverse.place.repository;

import cluverse.place.domain.Place;
import cluverse.place.domain.PlaceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByProviderAndSourceFingerprint(PlaceProvider provider, String sourceFingerprint);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO place (
                provider, source_fingerprint, name, category, raw_category,
                address, road_address, latitude, longitude, source_url,
                synchronized_at, created_at, updated_at
            ) VALUES (
                :#{#provider.name()}, :sourceFingerprint, :name, :category, :rawCategory,
                :address, :roadAddress, :latitude, :longitude, :sourceUrl,
                :synchronizedAt, :synchronizedAt, :synchronizedAt
            )
            ON DUPLICATE KEY UPDATE
                place_id = LAST_INSERT_ID(place_id),
                name = VALUES(name),
                category = VALUES(category),
                raw_category = VALUES(raw_category),
                address = VALUES(address),
                road_address = VALUES(road_address),
                latitude = VALUES(latitude),
                longitude = VALUES(longitude),
                source_url = VALUES(source_url),
                synchronized_at = VALUES(synchronized_at),
                updated_at = VALUES(updated_at)
            """, nativeQuery = true)
    int upsert(
            @Param("provider") PlaceProvider provider,
            @Param("sourceFingerprint") String sourceFingerprint,
            @Param("name") String name,
            @Param("category") String category,
            @Param("rawCategory") String rawCategory,
            @Param("address") String address,
            @Param("roadAddress") String roadAddress,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("sourceUrl") String sourceUrl,
            @Param("synchronizedAt") LocalDateTime synchronizedAt
    );

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    Long lastInsertedId();
}
