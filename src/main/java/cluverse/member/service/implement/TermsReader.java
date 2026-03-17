package cluverse.member.service.implement;

import cluverse.member.repository.TermsRepository;
import cluverse.member.service.response.TermsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TermsReader {

    private final TermsRepository termsRepository;

    public List<TermsResponse> readTerms() {
        return termsRepository.findAllByIsActiveTrueOrderByIsRequiredDescIdAsc().stream()
                .map(TermsResponse::from)
                .toList();
    }
}
