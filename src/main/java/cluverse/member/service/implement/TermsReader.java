package cluverse.member.service.implement;

import cluverse.member.domain.Terms;
import cluverse.member.repository.TermsRepository;
import cluverse.member.service.response.TermsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsReader {

    private final TermsRepository termsRepository;

    public List<TermsResponse> readTerms() {
        return termsRepository.findAllByIsActiveTrueOrderByIsRequiredDescIdAsc().stream()
                .map(TermsResponse::from)
                .toList();
    }

    public List<Long> readRequiredTermsIds() {
        return termsRepository.findAllByIsActiveTrueAndIsRequiredTrue().stream()
                .map(Terms::getId)
                .toList();
    }
}
