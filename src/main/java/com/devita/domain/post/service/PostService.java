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
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.devita.common.exception.ErrorCode.ACCESS_DENIED;
import static com.devita.common.exception.ErrorCode.POST_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String LIKE_KEY_PREFIX = "post:like:";
    private static final String LIKE_COUNT_KEY_PREFIX = "post:like_count:";

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
                .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

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
                .orElseThrow(() -> new AccessDeniedException(ACCESS_DENIED));
        return post;
    }

    // 좋아요 증가 (Redis)
    @Transactional
    public Long increaseLikeRedis(Long userId, Long postId) {
        String likeKey = LIKE_KEY_PREFIX + postId;
        String countKey = LIKE_COUNT_KEY_PREFIX + postId;
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        SetOperations<String, String> setOps = redisTemplate.opsForSet();

        valueOps.increment(countKey);

//        // Redis에 해당 게시물의 좋아요 데이터가 있는지 확인
//        String currentLikeCount = valueOps.get(countKey);
//
//        if (currentLikeCount != null) {
//            boolean isAdded = setOps.add(likeKey, userId.toString()) == 1;
//
//            if (isAdded) {
//                valueOps.increment(countKey);
//            }
//        } else {
//            syncLikeCountToRedis(postId);
//            boolean isAdded = setOps.add(likeKey, userId.toString()) == 1;
//
//            if (isAdded) {
//                valueOps.increment(countKey);
//            }
//        }
//
//        redisTemplate.expire(countKey, 24, TimeUnit.HOURS);

        return Long.parseLong(valueOps.get(countKey));
    }

    // Redis 캐시 처리
    private void syncLikeCountToRedis(Long postId) {
        String countKey = LIKE_COUNT_KEY_PREFIX + postId;
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

        redisTemplate.opsForValue().set(countKey, String.valueOf(post.getLikes()));
    }

    // 좋아요 증가 (Optimistic-Lock)
    @Transactional
    public Long increaseLikePessimistic(Long userId, Long postId) {
        Post post = postRepository.findByIdWithPessimisticLock(postId)
                .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

        post.updateLikes(post.getLikes() + 1);
        postRepository.save(post);

        return post.getLikes();
    }

    @Transactional
    public Long increaseLikeOptimistic(Long userId, Long postId) {
        int maxRetry = 100; // 최대 시도 횟수
        int attempt = 0;    // 현재 시도 횟수

        while (attempt < maxRetry) {
            try {
                // 트랜잭션 내에서 좋아요 증가 처리
                return updateLikeCount(postId);
            } catch (RuntimeException e) {
                attempt++;
                if (attempt >= maxRetry) {
                    throw new AccessDeniedException(ACCESS_DENIED);
                }
            }
        }

        // 실패할 경우 기본값 반환 (일반적으로 여기 도달하지 않음)
        throw new IllegalStateException("Unexpected state in like update process.");
    }

    @Transactional
    public Long updateLikeCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

        post.updateLikes(post.getLikes() + 1); // 좋아요 증가
        return post.getLikes();
    }
}