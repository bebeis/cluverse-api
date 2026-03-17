package cluverse.major.service.implement;

import cluverse.major.repository.MajorRepository;
import cluverse.major.service.response.MajorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MajorReader {

    private final MajorRepository majorRepository;

    public List<MajorResponse> readMajors(Long parentMajorId) {
        if (parentMajorId == null) {
            return majorRepository.findAllByIsActiveTrueAndParentIdIsNullOrderByDisplayOrderAscNameAsc().stream()
                    .map(MajorResponse::from)
                    .toList();
        }
        return majorRepository.findAllByIsActiveTrueAndParentIdOrderByDisplayOrderAscNameAsc(parentMajorId).stream()
                .map(MajorResponse::from)
                .toList();
    }
}
