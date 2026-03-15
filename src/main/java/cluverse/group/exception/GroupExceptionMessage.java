package cluverse.group.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GroupExceptionMessage {

    GROUP_NOT_FOUND("존재하지 않는 그룹입니다."),
    GROUP_ACCESS_DENIED("그룹 관리 권한이 없습니다."),
    GROUP_MEMBER_NOT_FOUND("그룹 멤버를 찾을 수 없습니다."),
    GROUP_ROLE_NOT_FOUND("그룹 직책을 찾을 수 없습니다."),
    GROUP_ROLE_ALREADY_EXISTS("이미 등록된 그룹 직책입니다."),
    GROUP_MEMBER_ALREADY_EXISTS("이미 그룹에 속한 회원입니다."),
    GROUP_OWNER_TRANSFER_REQUIRED("오너는 그룹을 탈퇴할 수 없습니다. 먼저 오너를 이관해주세요."),
    GROUP_OWNER_TRANSFER_TARGET_INVALID("새 오너는 현재 그룹 멤버여야 합니다."),
    GROUP_MEMBER_LIMIT_EXCEEDED("그룹 최대 인원을 초과했습니다.");

    private final String message;
}
