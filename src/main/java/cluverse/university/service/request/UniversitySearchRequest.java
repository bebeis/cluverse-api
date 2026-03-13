package cluverse.university.service.request;

import jakarta.validation.constraints.Size;

public record UniversitySearchRequest(
        @Size(max = 100, message = "학교 검색어는 100자 이하여야 합니다.")
        String keyword
) {
}
