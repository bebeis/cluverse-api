package cluverse.certification.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DataGoKrCertificationResponse(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String qualgbNm,
            String description,
            String docRegStartDt,
            String docRegEndDt,
            String docExamStartDt,
            String docExamEndDt,
            String pracRegStartDt,
            String pracRegEndDt,
            String pracExamStartDt,
            String pracExamEndDt
    ) {
    }
}
