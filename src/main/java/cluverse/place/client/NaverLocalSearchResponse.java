package cluverse.place.client;

import java.util.List;

public record NaverLocalSearchResponse(List<Item> items) {

    public record Item(
            String title,
            String link,
            String category,
            String address,
            String roadAddress,
            Long mapx,
            Long mapy
    ) {
    }
}
