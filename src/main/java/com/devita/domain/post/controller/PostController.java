package com.devita.domain.post.controller;

import com.devita.common.response.ApiResponse;
import com.devita.domain.post.dto.PostReqDTO;
import com.devita.domain.post.dto.PostResDTO;
import com.devita.domain.post.dto.PostsResDTO;
import com.devita.domain.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PostController {

    private final PostService postService;

    // 게시물 생성
    @PostMapping("/post")
    public ApiResponse<Long> addPost(@AuthenticationPrincipal Long userId, @RequestBody PostReqDTO postReqDTO) {
        Long postId = postService.addPost(userId, postReqDTO).getId();

        return ApiResponse.success(postId);
    }

    // 게시물 삭제
    @DeleteMapping("/post/{postId}")
    public ApiResponse<Void> deletePost(@AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        postService.deletePost(userId, postId);

        return ApiResponse.success(null);
    }

    // 게시물 수정
    @PutMapping("/post/{postId}")
    public ApiResponse<PostResDTO> updatePost(@AuthenticationPrincipal Long userId, @PathVariable Long postId, @RequestBody PostReqDTO postReqDTO) {
        PostResDTO postResDTO = postService.updatePost(userId, postId, postReqDTO);

        return ApiResponse.success(postResDTO);
    }

    // 게시물 페이징 조회
    @GetMapping("/posts")
    public ApiResponse<List<PostsResDTO>> getPosts(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        List<PostsResDTO> posts = postService.getPosts(page, size);

        return ApiResponse.success(posts);
    }

    // 게시물 상세 조회
    @GetMapping("/post/{postId}")
    public ApiResponse<PostResDTO> getPost(@AuthenticationPrincipal Long userId, @PathVariable Long postId) {
        PostResDTO postResDTO = postService.getPost(userId, postId);

        return ApiResponse.success(postResDTO);
    }

    // 작성한 게시물 조회
    @GetMapping("/posts/my")
    public ApiResponse<List<PostsResDTO>> getMyPosts(@AuthenticationPrincipal Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        List<PostsResDTO> posts = postService.getMyPosts(userId, page, size);

        return ApiResponse.success(posts);
    }
}
