package com.openspec.blog.controller;

import com.openspec.blog.dto.ApiResponse;
import com.openspec.blog.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TagController {

    private final PostService postService;

    public TagController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/api/tags")
    public ResponseEntity<ApiResponse<List<String>>> getTags() {
        try {
            List<String> tags = postService.getAllTags();
            return ResponseEntity.ok(ApiResponse.ok(tags));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("TAGS_READ_FAILED", "Failed to load tags.", e.getMessage()));
        }
    }
}
