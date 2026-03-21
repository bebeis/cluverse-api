package cluverse.member.service;

import cluverse.member.service.implement.TermsReader;
import cluverse.member.service.response.TermsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsQueryService {

    private final TermsReader termsReader;

    public List<TermsResponse> getTerms() {
        return termsReader.readTerms();
    }
}
