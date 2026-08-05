package cluverse.popularity.service.implement;

public record PopularityScore(int likeWeight, int commentWeight) {

    public long calculate(long likeCount, long commentCount) {
        return likeCount * likeWeight + commentCount * commentWeight;
    }
}
