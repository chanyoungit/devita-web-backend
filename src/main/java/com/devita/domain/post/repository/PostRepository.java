package com.devita.domain.post.repository;

import com.devita.domain.post.domain.Post;
import com.devita.domain.user.domain.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {



    // 모든 게시물을 페이징하여 조회
    Page<Post> findAll(Pageable pageable);

    // 사용자가 작성한 게시물 조회
    // N+1 고려
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.writer w WHERE w.id = :writerId",
            countQuery = "SELECT COUNT(p) FROM Post p WHERE p.writer.id = :writerId")
    Page<Post> findByWriterIdWithFetchJoin(@Param("writerId") Long writerId, Pageable pageable);
}