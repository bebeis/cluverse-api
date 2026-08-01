package cluverse.place.service.implement;

import cluverse.member.domain.Member;
import cluverse.member.service.implement.MemberReader;
import cluverse.place.domain.ResolvedPlaceAttachment;
import cluverse.place.domain.SelectedPlace;
import cluverse.university.domain.UniversityCampus;
import cluverse.university.service.implement.UniversityCampusReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalPlaceAttachmentResolver {

    private final MemberReader memberReader;
    private final UniversityCampusReader universityCampusReader;

    public List<ResolvedPlaceAttachment> resolve(Long memberId, List<SelectedPlace> places) {
        Member member = memberReader.readOrThrow(memberId);
        Long universityId = member.getUniversityId();
        List<UniversityCampus> campuses = canRecommend(member)
                ? universityCampusReader.readActiveByUniversityId(universityId)
                : List.of();
        return places.stream()
                .map(place -> resolve(place, universityId, campuses, member.isVerified()))
                .toList();
    }

    private ResolvedPlaceAttachment resolve(SelectedPlace selected, Long universityId,
                                             List<UniversityCampus> campuses, boolean verified) {
        Long campusId = null;
        if (verified && selected.recommended() && selected.candidate().category().isLocalMapEligible()) {
            campusId = UniversityCampusMatcher.findNearestInRadius(selected.candidate(), campuses)
                    .map(UniversityCampus::getId)
                    .orElse(null);
        }
        return new ResolvedPlaceAttachment(
                selected.candidate(), universityId, campusId, selected.recommended());
    }

    private boolean canRecommend(Member member) {
        return member.isVerified() && member.hasUniversity();
    }
}
