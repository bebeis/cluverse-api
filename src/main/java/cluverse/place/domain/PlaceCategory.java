package cluverse.place.domain;

import java.util.Locale;
import java.util.Set;

public enum PlaceCategory {
    FOOD,
    CAFE,
    OTHER;

    private static final Set<String> FOOD_KEYWORDS = Set.of(
            "음식점", "한식", "중식", "일식", "양식", "분식", "치킨", "피자", "햄버거",
            "고기", "국수", "해물", "뷔페", "술집", "요리"
    );
    private static final Set<String> CAFE_KEYWORDS = Set.of("카페", "디저트", "베이커리", "커피");

    public static PlaceCategory from(String rawCategory) {
        String normalized = rawCategory == null ? "" : rawCategory.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, CAFE_KEYWORDS)) {
            return CAFE;
        }
        if (containsAny(normalized, FOOD_KEYWORDS)) {
            return FOOD;
        }
        return OTHER;
    }

    public boolean isLocalMapEligible() {
        return this == FOOD || this == CAFE;
    }

    private static boolean containsAny(String source, Set<String> keywords) {
        return keywords.stream().anyMatch(source::contains);
    }
}
