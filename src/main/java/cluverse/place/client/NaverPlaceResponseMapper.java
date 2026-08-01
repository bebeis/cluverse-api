package cluverse.place.client;

import cluverse.place.domain.PlaceProvider;
import cluverse.place.domain.PlaceSourceCandidate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class NaverPlaceResponseMapper {

    private static final int NAVER_COORDINATE_SCALE = 7;
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    public List<PlaceSourceCandidate> map(NaverLocalSearchResponse response) {
        if (response == null || response.items() == null) {
            return List.of();
        }
        return response.items().stream()
                .filter(item -> item.mapx() != null && item.mapy() != null)
                .map(this::map)
                .toList();
    }

    private PlaceSourceCandidate map(NaverLocalSearchResponse.Item item) {
        return new PlaceSourceCandidate(
                PlaceProvider.NAVER,
                normalizeTitle(item.title()),
                normalizeNullable(item.category()),
                normalizeNullable(item.address()),
                normalizeNullable(item.roadAddress()),
                BigDecimal.valueOf(item.mapy(), NAVER_COORDINATE_SCALE),
                BigDecimal.valueOf(item.mapx(), NAVER_COORDINATE_SCALE),
                normalizeNullable(item.link())
        );
    }

    private String normalizeTitle(String value) {
        String unescaped = HtmlUtils.htmlUnescape(value == null ? "" : value);
        return HTML_TAG.matcher(unescaped).replaceAll("").trim();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return HtmlUtils.htmlUnescape(value).trim();
    }
}
