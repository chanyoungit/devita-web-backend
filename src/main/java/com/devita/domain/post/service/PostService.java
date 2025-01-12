package com.devita.domain.post.service;

import com.devita.common.exception.AccessDeniedException;
import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.post.domain.Post;
import com.devita.domain.post.dto.PostReqDTO;
import com.devita.domain.post.dto.PostResDTO;
import com.devita.domain.post.dto.PostsResDTO;
import com.devita.domain.post.repository.PostRepository;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 게시물 생성
    public Post addPost(Long userId, PostReqDTO postReqDTO) {
        User writer = getWriter(userId);

        Post post = Post.builder()
                .writer(writer)
                .title(postReqDTO.title())
                .description(postReqDTO.description())
                .build();

        return postRepository.save(post);
    }

    // 게시물 삭제
    public void deletePost(Long userId, Long postId) {
        Post post = validateWriter(userId, postId);

        postRepository.delete(post);
    }

    // 게시물 수정
    public PostResDTO updatePost(Long userId, Long postId, PostReqDTO postReqDTO) {
        Post post = validateWriter(userId, postId);

        post.updatePost(postReqDTO.title(), postReqDTO.description());
        postRepository.save(post);

        return new PostResDTO(postId, post.getWriter().getNickname(), post.getTitle(), post.getDescription(), post.getLikes(), post.getViews());
    }

    // 게시물 페이징 조회
    public List<PostsResDTO> getPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size); // 페이지 번호와 페이지 크기를 기반으로 Pageable 객체 생성
        Page<Post> postPage = postRepository.findAll(pageable);

        return postPage.getContent().stream()
                .map(post -> new PostsResDTO(post.getId(), post.getTitle(), post.getDescription(), post.getLikes(), post.getViews()))
                .toList();
    }

    // 게시물 상세 조회
    public PostResDTO getPost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.POST_NOT_FOUND));

        if (!post.getWriter().getId().equals(userId)) {
            post.increaseView();
            postRepository.save(post);
        }

        return new PostResDTO(postId, post.getWriter().getNickname(), post.getTitle(), post.getDescription(), post.getLikes(), post.getViews());
    }

    // 작성한 게시물 조회
    public List<PostsResDTO> getMyPosts(Long userId, int page, int size) {
        getWriter(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Post> postPage = postRepository.findByWriterIdWithFetchJoin(userId, pageable);

        return postPage.getContent().stream()
                .map(post -> new PostsResDTO(
                        post.getId(),
                        post.getTitle(),
                        post.getDescription(),
                        post.getLikes(),
                        post.getViews()
                ))
                .toList();
    }

    private User getWriter(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    // 작성자 유무 확인
    private Post validateWriter(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> p.getWriter().getId().equals(userId))
                .orElseThrow(() -> new AccessDeniedException(ErrorCode.ACCESS_DENIED));
        return post;
    }
}