package com.devita.domain.post.service;

import com.devita.common.exception.AccessDeniedException;
import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.common.exception.SecurityTokenException;
import com.devita.domain.post.domain.Post;
import com.devita.domain.post.dto.PostReqDTO;
import com.devita.domain.post.dto.PostResDTO;
import com.devita.domain.post.dto.PostsResDTO;
import com.devita.domain.post.repository.PostRepository;
import com.devita.domain.user.domain.AuthProvider;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    private User testUser;
    private Post testPost;
    private PostReqDTO postReqDTO;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@test.com";
    private static final String USER_NICKNAME = "testUser";
    private static final Long POST_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(USER_EMAIL)
                .nickname(USER_NICKNAME)
                .provider(AuthProvider.KAKAO)
                .build();
        testUser.setId(USER_ID);

        testPost = Post.builder()
                .writer(testUser)
                .title("Test Title")
                .description("Test Description")
                .build();
        testPost.setId(POST_ID);

        postReqDTO = PostReqDTO.builder()
                .title("Test ReqDTO Title")
                .description("Test ReqDTO Description")
                .build();
    }

    @Test
    @DisplayName("게시글 추가 성공")
    void addPost_Success() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post savedPost = invocation.getArgument(0);
            savedPost.setId(2L);
            return savedPost;
        });

        // when
        Post result = postService.addPost(USER_ID, postReqDTO);

        // then
        assertNotNull(result);
        assertEquals(postReqDTO.title(), result.getTitle());
        assertEquals(postReqDTO.description(), result.getDescription());
        assertEquals(testUser, result.getWriter());
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));

        // when
        postService.deletePost(USER_ID, POST_ID);

        // then
        verify(postRepository).delete(testPost);
    }

    @Test
    @DisplayName("게시글 업데이트 성공")
    void updatePost_Success() {
        // given
        PostReqDTO updateDTO = PostReqDTO.builder()
                .title("Updated Title")
                .description("Updated Description")
                .build();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        // when
        PostResDTO result = postService.updatePost(USER_ID, POST_ID, updateDTO);

        // then
        assertEquals(updateDTO.title(), result.title());
        assertEquals(updateDTO.description(), result.description());
    }

    @Test
    @DisplayName("게시글 단건 조회 성공")
    void getPost_Success() {
        // given
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));

        // when
        PostResDTO result = postService.getPost(USER_ID, POST_ID);

        // then
        assertEquals(testPost.getTitle(), result.title());
        assertEquals(testPost.getDescription(), result.description());
        assertEquals(testPost.getWriter().getNickname(), result.writer());
    }

    @Test
    @DisplayName("게시글 목록 조회 성공")
    void getPosts_Success() {
        // given
        List<Post> posts = List.of(testPost);
        Page<Post> postPage = new PageImpl<>(posts);

        when(postRepository.findAll(any(Pageable.class))).thenReturn(postPage);

        // when
        List<PostsResDTO> result = postService.getPosts(0, 10);

        // then
        assertFalse(result.isEmpty());
        assertEquals(testPost.getTitle(), result.get(0).title());
    }

    @Test
    @DisplayName("사용자의 게시글 목록 조회 성공")
    void getMyPosts_Success() {
        // given
        List<Post> posts = List.of(testPost);
        Page<Post> postPage = new PageImpl<>(posts);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(postRepository.findByWriterIdWithFetchJoin(eq(USER_ID), any(Pageable.class)))
                .thenReturn(postPage);

        // when
        List<PostsResDTO> result = postService.getMyPosts(USER_ID, 0, 10);

        // then
        assertFalse(result.isEmpty());
        assertEquals(testPost.getTitle(), result.get(0).title());
    }

    @Test
    @DisplayName("게시글 추가 시 유저가 없을 경우 예외 발생")
    void addPost_UserNotFound() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> postService.addPost(USER_ID, postReqDTO));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("게시글 업데이트 시 권한이 없을 경우 예외 발생")
    void updatePost_AccessDenied() {
        // given
        User otherUser = User.builder()
                .email("other@test.com")
                .nickname("otherNickname")
                .provider(AuthProvider.KAKAO)
                .build();
        otherUser.setId(2L);

        Post otherPost = Post.builder()
                .writer(otherUser)
                .title("Other Title")
                .description("Other Description")
                .build();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(otherPost));

        // when
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> postService.updatePost(USER_ID, POST_ID, postReqDTO));

        // then
        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("게시글 조회 시 조회수 증가")
    void getPost_IncreaseView() {
        // given
        User otherUser = User.builder()
                .email("other@test.com")
                .nickname("otherNickname")
                .provider(AuthProvider.KAKAO)
                .build();
        otherUser.setId(2L);

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));

        // when
        postService.getPost(2L, POST_ID);

        // then
        verify(postRepository).save(testPost);
        assertEquals(1, testPost.getViews());
    }
}