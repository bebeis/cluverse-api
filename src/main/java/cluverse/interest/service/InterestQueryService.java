package cluverse.interest.service;

import cluverse.interest.service.implement.InterestReader;
import cluverse.interest.service.response.InterestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestQueryService {

    private final InterestReader interestReader;

    public List<InterestResponse> getInterests() {
        return interestReader.readInterests();
    }
}
