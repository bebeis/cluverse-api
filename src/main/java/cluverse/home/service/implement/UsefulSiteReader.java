package cluverse.home.service.implement;

import cluverse.home.domain.UsefulSite;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UsefulSiteReader {

    private static final List<UsefulSite> USEFUL_SITES = List.of(
            new UsefulSite("에브리타임", "학교 생활과 시간표", "https://everytime.kr"),
            new UsefulSite("ChatGPT", "학습과 아이디어 정리", "https://chatgpt.com"),
            new UsefulSite("Gemini", "자료 탐색과 학습 보조", "https://gemini.google.com"),
            new UsefulSite("Q-Net", "국가자격 시험 정보", "https://www.q-net.or.kr"),
            new UsefulSite("K-MOOC", "대학 공개 강좌", "https://www.kmooc.kr"),
            new UsefulSite("캠퍼스픽", "대외활동과 공모전", "https://www.campuspick.com")
    );

    public List<UsefulSite> readAll() {
        return USEFUL_SITES;
    }
}
