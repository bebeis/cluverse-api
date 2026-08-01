package cluverse.university.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UniversityCampus extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "university_campus_id")
    private Long id;

    @Column(nullable = false)
    private Long universityId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    private int localRadiusMeter;

    @Column(nullable = false)
    private boolean isActive;

    private UniversityCampus(Long universityId, String name, BigDecimal latitude, BigDecimal longitude,
                             int localRadiusMeter, boolean isActive) {
        this.universityId = universityId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.localRadiusMeter = localRadiusMeter;
        this.isActive = isActive;
    }

    public static UniversityCampus create(Long universityId, String name, BigDecimal latitude,
                                          BigDecimal longitude, int localRadiusMeter) {
        return new UniversityCampus(universityId, name, latitude, longitude, localRadiusMeter, true);
    }
}
