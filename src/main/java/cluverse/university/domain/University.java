package cluverse.university.domain;

import cluverse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class University extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "university_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String emailDomain;
    private String badgeImageUrl;
    private String address;

    @Column(nullable = false)
    private boolean isActive = true;

    private University(String name, String emailDomain, String badgeImageUrl, String address, boolean isActive) {
        this.name = normalizeRequired(name);
        this.emailDomain = normalizeOptional(emailDomain);
        this.badgeImageUrl = normalizeOptional(badgeImageUrl);
        this.address = normalizeOptional(address);
        this.isActive = isActive;
    }

    public static University create(
            String name,
            String emailDomain,
            String badgeImageUrl,
            String address,
            boolean isActive
    ) {
        return new University(name, emailDomain, badgeImageUrl, address, isActive);
    }

    public void update(String name, String emailDomain, String badgeImageUrl, String address, boolean isActive) {
        this.name = normalizeRequired(name);
        this.emailDomain = normalizeOptional(emailDomain);
        this.badgeImageUrl = normalizeOptional(badgeImageUrl);
        this.address = normalizeOptional(address);
        this.isActive = isActive;
    }

    private String normalizeRequired(String value) {
        return normalizeOptional(Objects.requireNonNull(value));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
