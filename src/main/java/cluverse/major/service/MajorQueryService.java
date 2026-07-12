package cluverse.major.service;

import cluverse.major.service.implement.MajorReader;
import cluverse.major.service.response.MajorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MajorQueryService {

    private final MajorReader majorReader;

    public List<MajorResponse> getMajors(Long parentMajorId) {
        return majorReader.readMajors(parentMajorId);
    }
}
