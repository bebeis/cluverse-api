package cluverse.certification.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DataGoKrCertificationResponse(
        Response response,
        Header header,
        Body body
) {

    public DataGoKrCertificationResponse(Response response) {
        this(response, null, null);
    }

    public Header resolvedHeader() {
        return response == null ? header : response.header();
    }

    public Body resolvedBody() {
        return response == null ? body : response.body();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(JsonNode items) {
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
