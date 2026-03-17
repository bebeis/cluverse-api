package cluverse.interest.service.implement;

import cluverse.interest.repository.InterestRepository;
import cluverse.interest.service.response.InterestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InterestReader {

    private final InterestRepository interestRepository;

    public List<InterestResponse> readInterests() {
        return interestRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(InterestResponse::from)
                .toList();
    }
}
