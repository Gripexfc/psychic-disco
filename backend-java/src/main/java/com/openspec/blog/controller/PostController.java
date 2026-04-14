package com.openspec.blog.controller;

import com.openspec.blog.dto.ApiResponse;
import com.openspec.blog.dto.CreatePostRequest;
import com.openspec.blog.dto.ListPostsResponse;
import com.openspec.blog.dto.SiteStats;
import com.openspec.blog.model.Post;
import com.openspec.blog.service.AuthService;
import com.openspec.blog.service.PostService;
import com.openspec.blog.util.HtmlSanitizer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class PostController {

    private final PostService postService;
    private final AuthService authService;

    public PostController(PostService postService, AuthService authService) {
        this.postService = postService;
        this.authService = authService;
    }

    @GetMapping("/api/posts")
    public ResponseEntity<ApiResponse<ListPostsResponse>> getPosts(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "6") Integer pageSize,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDraft
    ) {
        try {
            if (page == null || page < 1) page = 1;
            if (pageSize == null || pageSize < 1) pageSize = 6;

            List<Post> filtered = postService.searchPosts(tag, search, Boolean.TRUE.equals(includeDraft));
            long total = filtered.size();

            int start = (page - 1) * pageSize;
            List<Post> items = start < filtered.size()
                    ? filtered.subList(start, Math.min(start + pageSize, filtered.size()))
                    : List.of();

            ApiResponse.ApiMeta meta = new ApiResponse.ApiMeta(
                    new ApiResponse.Pagination(page, pageSize, total)
            );
            return ResponseEntity.ok(ApiResponse.ok(new ListPostsResponse(items), meta));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("POSTS_READ_FAILED", "Failed to load posts.", e.getMessage()));
        }
    }

    @GetMapping("/api/posts/{idOrSlug}")
    public ResponseEntity<ApiResponse<Post>> getPost(@PathVariable String idOrSlug) {
        try {
            Optional<Post> post = postService.getPostByIdOrSlug(idOrSlug);
            if (post.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("POST_NOT_FOUND", "Post \"" + idOrSlug + "\" was not found."));
            }
            return ResponseEntity.ok(ApiResponse.ok(post.get()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("POST_READ_FAILED", "Failed to load post detail.", e.getMessage()));
        }
    }

    @PostMapping("/api/posts/{idOrSlug}/like")
    public ResponseEntity<ApiResponse<Post>> likePost(@PathVariable String idOrSlug) {
        try {
            Post updated = postService.likePost(idOrSlug);
            if (updated == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("POST_NOT_FOUND", "Post \"" + idOrSlug + "\" was not found."));
            }
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("POST_LIKE_FAILED", "Failed to like post.", e.getMessage()));
        }
    }

    @PostMapping("/api/posts/{idOrSlug}/view")
    public ResponseEntity<ApiResponse<Post>> incrementView(@PathVariable String idOrSlug) {
        try {
            Post updated = postService.incrementViews(idOrSlug);
            if (updated == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("POST_NOT_FOUND", "Post \"" + idOrSlug + "\" was not found."));
            }
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("POST_VIEW_FAILED", "Failed to record view.", e.getMessage()));
        }
    }

    @GetMapping("/api/posts/{slug}/related")
    public ResponseEntity<ApiResponse<List<Post>>> getRelatedPosts(
            @PathVariable String slug,
            @RequestParam(defaultValue = "3") int limit
    ) {
        try {
            List<Post> related = postService.getRelatedPosts(slug, limit);
            return ResponseEntity.ok(ApiResponse.ok(related));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("RELATED_POSTS_FAILED", "Failed to load related posts.", e.getMessage()));
        }
    }

    @GetMapping("/api/stats")
    public ResponseEntity<ApiResponse<SiteStats>> getSiteStats() {
        try {
            SiteStats stats = postService.getSiteStats();
            return ResponseEntity.ok(ApiResponse.ok(stats));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("STATS_FAILED", "Failed to load site stats.", e.getMessage()));
        }
    }

    @PostMapping("/api/posts")
    public ResponseEntity<ApiResponse<Post>> createPost(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody CreatePostRequest request
    ) {
        String token = extractBearerToken(authHeader);
        if (!authService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("UNAUTHORIZED", "请先登录后再执行该操作。"));
        }

        String normalizedHtml = HtmlSanitizer.sanitize(request.getContentHtml());
        String normalizedText = request.getContent() != null ? request.getContent().trim() : "";

        if (request.getTitle() == null || request.getTitle().isEmpty()
                || request.getExcerpt() == null || request.getExcerpt().isEmpty()
                || (normalizedText.isEmpty() && normalizedHtml.isEmpty())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("INVALID_POST_PAYLOAD", "title, excerpt and one of content/contentHtml are required."));
        }

        try {
            Post newPost = new Post();
            newPost.setTitle(request.getTitle());
            newPost.setExcerpt(request.getExcerpt());
            newPost.setContent(normalizedText.isEmpty() ? null : normalizedText);
            newPost.setContentHtml(normalizedHtml.isEmpty() ? null : normalizedHtml);
            newPost.setTags(request.getTags());

            Post created = postService.createPost(newPost);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("POST_CREATE_FAILED", "Failed to publish post.", e.getMessage()));
        }
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "";
        }
        return authHeader.substring(7).trim();
    }
}
