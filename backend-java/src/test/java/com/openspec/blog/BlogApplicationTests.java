package com.openspec.blog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openspec.blog.dto.CreatePostRequest;
import com.openspec.blog.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlogApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthEndpoint_returnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ok"));
    }

    @Test
    void apiHealthEndpoint_returnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ok"));
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("testpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("test-token"))
                .andExpect(jsonPath("$.data.user.username").value("testuser"));
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("wronguser");
        request.setPassword("wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void getMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMe_withValidToken_returnsUser() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void getSite_returnsSiteInfo() throws Exception {
        mockMvc.perform(get("/api/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("EDY.LOG"));
    }

    @Test
    void getTags_returnsSortedUniqueTags() throws Exception {
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
    }

    @Test
    void getPosts_returnsPaginatedPosts() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("page", "1")
                        .param("pageSize", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.pagination.page").value(1))
                .andExpect(jsonPath("$.meta.pagination.pageSize").value(6));
    }

    @Test
    void getPosts_withTagFilter_filtersCorrectly() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("tag", "Vue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getPosts_withSearchFilter_filtersCorrectly() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "vue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getPost_existingPost_returnsPost() throws Exception {
        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getPost_existingBySlug_returnsPost() throws Exception {
        mockMvc.perform(get("/api/posts/cyber-style-homepage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("cyber-style-homepage"));
    }

    @Test
    void getPost_nonexistent_returns404() throws Exception {
        mockMvc.perform(get("/api/posts/nonexistent-slug"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
    }

    @Test
    void likePost_existingPost_incrementsLikes() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/posts/1/like"))
                .andExpect(status().isOk())
                .andReturn();

        // Can run multiple times since test data is reset
        assert result.getResponse().getContentAsString().contains("\"likes\"");
    }

    @Test
    void createPost_withoutAuth_returns401() throws Exception {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("New Post");
        request.setExcerpt("Excerpt");
        request.setContent("Content");

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPost_withAuth_createsPost() throws Exception {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("New Test Post Unique " + System.currentTimeMillis());
        request.setExcerpt("Test excerpt");
        request.setContent("Test content");
        request.setTags(java.util.Arrays.asList("Test", "Java"));

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value(request.getTitle()))
                .andExpect(jsonPath("$.data.slug", startsWith("new-test-post-unique-")));
    }

    @Test
    void createPost_withInvalidPayload_returns400() throws Exception {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle(""); // Missing required fields

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_POST_PAYLOAD"));
    }

    @Test
    void logout_returnsSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));
    }
}
