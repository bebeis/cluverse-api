package cluverse.comment.repository;

import cluverse.comment.domain.Comment;

public interface PostCommentActivityRepositoryCustom {

    int upsertLatest(Comment comment);
}
