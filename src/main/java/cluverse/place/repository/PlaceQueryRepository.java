package cluverse.place.repository;

import cluverse.place.domain.PlaceCategory;
import cluverse.place.repository.dto.LocalMapMarkerQueryResult;
import cluverse.place.repository.dto.PlaceContentQueryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PlaceQueryRepository {

    private static final String ELIGIBLE_RECOMMENDATIONS = """
            SELECT pp.place_id, pp.created_at
            FROM post_place pp
            JOIN post po ON po.post_id = pp.post_id
            WHERE pp.author_university_id = :universityId
              AND (:campusId IS NULL OR pp.university_campus_id = :campusId)
              AND pp.university_campus_id IS NOT NULL
              AND pp.recommended = TRUE
              AND po.status = 'ACTIVE'
              AND po.is_external_visible = TRUE
            UNION ALL
            SELECT cp.place_id, cp.created_at
            FROM comment_place cp
            JOIN comment co ON co.comment_id = cp.comment_id
            JOIN post po ON po.post_id = co.post_id
            WHERE cp.author_university_id = :universityId
              AND (:campusId IS NULL OR cp.university_campus_id = :campusId)
              AND cp.university_campus_id IS NOT NULL
              AND cp.recommended = TRUE
              AND co.status = 'ACTIVE'
              AND po.status = 'ACTIVE'
              AND po.is_external_visible = TRUE
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<LocalMapMarkerQueryResult> findMarkers(
            Long universityId,
            Long campusId,
            PlaceCategory category
    ) {
        String sql = """
                SELECT p.place_id, p.name, p.category, p.address, p.road_address,
                       p.latitude, p.longitude, COUNT(*) AS recommendation_count,
                       MAX(r.created_at) AS last_recommended_at
                FROM (
                """ + ELIGIBLE_RECOMMENDATIONS + """
                ) r
                JOIN place p ON p.place_id = r.place_id
                WHERE (:category IS NULL OR p.category = :category)
                GROUP BY p.place_id, p.name, p.category, p.address, p.road_address,
                         p.latitude, p.longitude
                ORDER BY recommendation_count DESC, last_recommended_at DESC, p.place_id DESC
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("universityId", universityId)
                .addValue("campusId", campusId)
                .addValue("category", category == null ? null : category.name());
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new LocalMapMarkerQueryResult(
                resultSet.getLong("place_id"),
                resultSet.getString("name"),
                resultSet.getString("category"),
                resultSet.getString("address"),
                resultSet.getString("road_address"),
                resultSet.getBigDecimal("latitude"),
                resultSet.getBigDecimal("longitude"),
                resultSet.getLong("recommendation_count"),
                resultSet.getTimestamp("last_recommended_at").toLocalDateTime()
        ));
    }

    public long countRecommendations(Long placeId) {
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT pp.post_place_id AS recommendation_id
                    FROM post_place pp
                    JOIN post po ON po.post_id = pp.post_id
                    WHERE pp.place_id = :placeId
                      AND pp.university_campus_id IS NOT NULL
                      AND pp.recommended = TRUE
                      AND po.status = 'ACTIVE'
                      AND po.is_external_visible = TRUE
                    UNION ALL
                    SELECT cp.comment_place_id AS recommendation_id
                    FROM comment_place cp
                    JOIN comment co ON co.comment_id = cp.comment_id
                    JOIN post po ON po.post_id = co.post_id
                    WHERE cp.place_id = :placeId
                      AND cp.university_campus_id IS NOT NULL
                      AND cp.recommended = TRUE
                      AND co.status = 'ACTIVE'
                      AND po.status = 'ACTIVE'
                      AND po.is_external_visible = TRUE
                ) r
                """;
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("placeId", placeId), Long.class);
        return count == null ? 0 : count;
    }

    public List<PlaceContentQueryResult> findContents(
            Long placeId,
            LocalDateTime cursorCreatedAt,
            String cursorContentType,
            Long cursorContentId,
            int limit
    ) {
        String sql = """
                SELECT content_type, content_id, title, content, author_id, author_nickname, created_at
                FROM (
                    SELECT 'POST' AS content_type, po.post_id AS content_id, po.title, po.content,
                           CASE WHEN po.is_anonymous THEN NULL ELSE po.member_id END AS author_id,
                           CASE WHEN po.is_anonymous THEN NULL ELSE m.nickname END AS author_nickname,
                           po.created_at
                    FROM post_place pp
                    JOIN post po ON po.post_id = pp.post_id
                    JOIN member m ON m.member_id = po.member_id
                    WHERE pp.place_id = :placeId
                      AND po.status = 'ACTIVE'
                      AND po.is_external_visible = TRUE
                    UNION ALL
                    SELECT 'COMMENT' AS content_type, co.comment_id AS content_id, NULL AS title, co.content,
                           CASE WHEN co.is_anonymous THEN NULL ELSE co.member_id END AS author_id,
                           CASE WHEN co.is_anonymous THEN NULL ELSE m.nickname END AS author_nickname,
                           co.created_at
                    FROM comment_place cp
                    JOIN comment co ON co.comment_id = cp.comment_id
                    JOIN post po ON po.post_id = co.post_id
                    JOIN member m ON m.member_id = co.member_id
                    WHERE cp.place_id = :placeId
                      AND co.status = 'ACTIVE'
                      AND po.status = 'ACTIVE'
                      AND po.is_external_visible = TRUE
                ) contents
                WHERE (:cursorCreatedAt IS NULL
                    OR created_at < :cursorCreatedAt
                    OR (created_at = :cursorCreatedAt AND content_type < :cursorContentType)
                    OR (created_at = :cursorCreatedAt AND content_type = :cursorContentType
                        AND content_id < :cursorContentId))
                ORDER BY created_at DESC, content_type DESC, content_id DESC
                LIMIT :limit
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("placeId", placeId)
                .addValue("cursorCreatedAt",
                        cursorCreatedAt == null ? null : Timestamp.valueOf(cursorCreatedAt))
                .addValue("cursorContentType", cursorContentType)
                .addValue("cursorContentId", cursorContentId)
                .addValue("limit", limit);
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new PlaceContentQueryResult(
                resultSet.getString("content_type"),
                resultSet.getLong("content_id"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getObject("author_id", Long.class),
                resultSet.getString("author_nickname"),
                resultSet.getTimestamp("created_at").toLocalDateTime()
        ));
    }
}
