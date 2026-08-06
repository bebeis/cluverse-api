package cluverse.place.controller;

import cluverse.docs.RestDocsSupport;
import cluverse.place.domain.PlaceCategory;
import cluverse.place.domain.PlaceProvider;
import cluverse.place.service.PlaceQueryService;
import cluverse.place.service.PlaceSearchServiceV2;
import cluverse.place.service.response.PlaceContentResponse;
import cluverse.place.service.response.PlaceContentType;
import cluverse.place.service.response.PlaceContentsResponse;
import cluverse.place.service.response.PlaceDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaceControllerV2DocsTest extends RestDocsSupport {

    private final PlaceSearchServiceV2 placeSearchService = mock(PlaceSearchServiceV2.class);
    private final PlaceQueryService placeQueryService = mock(PlaceQueryService.class);

    @Override
    protected Object initController() {
        return new PlaceControllerV2(placeSearchService, placeQueryService);
    }

    @Test
    void 장소_상세_조회() throws Exception {
        when(placeQueryService.readDetail(1L)).thenReturn(new PlaceDetailResponse(
                1L,
                PlaceProvider.NAVER,
                "클루버스 카페",
                PlaceCategory.CAFE,
                "음식점>카페",
                "서울시 관악구",
                "서울시 관악구 대학로 1",
                new BigDecimal("37.1234567"),
                new BigDecimal("127.1234567"),
                "https://map.naver.com/example",
                3L
        ));

        mockMvc.perform(get("/api/v2/places/{placeId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.placeId").value(1L))
                .andExpect(jsonPath("$.data.name").value("클루버스 카페"))
                .andDo(document("places-v2/get-detail",
                        pathParameters(
                                parameterWithName("placeId").description("장소 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.placeId").type(JsonFieldType.NUMBER).description("장소 ID"),
                                fieldWithPath("data.provider").type(JsonFieldType.STRING).description("장소 제공자"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("장소명"),
                                fieldWithPath("data.category").type(JsonFieldType.STRING).description("서비스 장소 카테고리"),
                                fieldWithPath("data.rawCategory").type(JsonFieldType.STRING).description("외부 장소 카테고리"),
                                fieldWithPath("data.address").type(JsonFieldType.STRING).description("지번 주소"),
                                fieldWithPath("data.roadAddress").type(JsonFieldType.STRING).description("도로명 주소"),
                                fieldWithPath("data.latitude").type(JsonFieldType.NUMBER).description("위도"),
                                fieldWithPath("data.longitude").type(JsonFieldType.NUMBER).description("경도"),
                                fieldWithPath("data.sourceUrl").type(JsonFieldType.STRING).description("원본 장소 URL"),
                                fieldWithPath("data.recommendationCount").type(JsonFieldType.NUMBER).description("추천 수")
                        )
                ));
    }

    @Test
    void 장소_관련_콘텐츠_조회() throws Exception {
        when(placeQueryService.readContents(1L, "cursor", 20)).thenReturn(new PlaceContentsResponse(
                List.of(new PlaceContentResponse(
                        PlaceContentType.POST,
                        10L,
                        10L,
                        "학교 앞 카페 추천",
                        "조용하고 커피가 맛있어요.",
                        2L,
                        "luna",
                        true,
                        LocalDateTime.of(2026, 8, 1, 12, 0)
                )),
                "next-cursor",
                true
        ));

        mockMvc.perform(get("/api/v2/places/{placeId}/contents", 1L)
                        .queryParam("cursor", "cursor")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contents[0].contentId").value(10L))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andDo(document("places-v2/get-contents",
                        pathParameters(
                                parameterWithName("placeId").description("장소 ID")
                        ),
                        queryParameters(
                                parameterWithName("cursor").description("다음 페이지 커서").optional(),
                                parameterWithName("size").description("페이지 크기").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").type(JsonFieldType.NUMBER).description("HTTP 상태 코드"),
                                fieldWithPath("status").type(JsonFieldType.STRING).description("HTTP 상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메시지"),
                                fieldWithPath("data.contents").type(JsonFieldType.ARRAY).description("관련 콘텐츠 목록"),
                                fieldWithPath("data.contents[].contentType").type(JsonFieldType.STRING).description("콘텐츠 유형"),
                                fieldWithPath("data.contents[].contentId").type(JsonFieldType.NUMBER).description("콘텐츠 ID"),
                                fieldWithPath("data.contents[].postId").type(JsonFieldType.NUMBER).description("연결할 부모 게시글 ID"),
                                fieldWithPath("data.contents[].title").type(JsonFieldType.STRING).description("제목").optional(),
                                fieldWithPath("data.contents[].content").type(JsonFieldType.STRING).description("내용"),
                                fieldWithPath("data.contents[].authorId").type(JsonFieldType.NUMBER).description("작성자 ID").optional(),
                                fieldWithPath("data.contents[].authorNickname").type(JsonFieldType.STRING).description("작성자 닉네임").optional(),
                                fieldWithPath("data.contents[].isLocalStudent").type(JsonFieldType.BOOLEAN).description("현지 학생 작성 여부"),
                                fieldWithPath("data.contents[].createdAt").type(JsonFieldType.STRING).description("작성 시각"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).description("다음 페이지 커서").optional(),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }
}
