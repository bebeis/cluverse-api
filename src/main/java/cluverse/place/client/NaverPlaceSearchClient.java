package cluverse.place.client;

import cluverse.common.exception.ExternalServiceException;
import cluverse.place.domain.PlaceSourceCandidate;
import cluverse.place.exception.PlaceExceptionMessage;
import cluverse.place.properties.LocalMapProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.util.List;

@Component
public class NaverPlaceSearchClient implements PlaceSearchClient {

    private static final int MAX_DISPLAY_SIZE = 5;

    private final RestClient restClient;
    private final NaverPlaceResponseMapper responseMapper;

    public NaverPlaceSearchClient(LocalMapProperties properties, NaverPlaceResponseMapper responseMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.providerBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("X-Naver-Client-Id", properties.naverClientId())
                .defaultHeader("X-Naver-Client-Secret", properties.naverClientSecret())
                .build();
        this.responseMapper = responseMapper;
    }

    @Override
    public List<PlaceSourceCandidate> search(String query) {
        try {
            NaverLocalSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/local.json")
                            .queryParam("query", query)
                            .queryParam("display", MAX_DISPLAY_SIZE)
                            .queryParam("start", 1)
                            .build())
                    .retrieve()
                    .body(NaverLocalSearchResponse.class);
            return responseMapper.map(response);
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new ExternalServiceException(PlaceExceptionMessage.PLACE_SEARCH_UNAVAILABLE.getMessage(), e);
        }
    }
}
