package com.openspec.blog;

import com.openspec.blog.model.Post;
import com.openspec.blog.repository.DataRepository;
import com.openspec.blog.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private DataRepository repository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(repository);
    }

    @Test
    void getAllPosts_normalizesLikes() {
        Post post1 = new Post();
        post1.setId(1L);
        post1.setLikes(-5L);
        post1.setPublished(true);

        when(repository.getAllPosts()).thenReturn(Arrays.asList(post1));

        List<Post> result = postService.getAllPosts();

        assertEquals(1, result.size());
        assertEquals(0L, result.get(0).getLikes());
    }

    @Test
    void getAllPosts_filtersNullLikes() {
        Post post1 = new Post();
        post1.setId(1L);
        post1.setLikes(null);
        post1.setPublished(true);

        when(repository.getAllPosts()).thenReturn(Arrays.asList(post1));

        List<Post> result = postService.getAllPosts();

        assertEquals(1, result.size());
        assertEquals(0L, result.get(0).getLikes());
    }

    @Test
    void getPostByIdOrSlug_findById() {
        Post post = new Post();
        post.setId(1L);
        post.setSlug("test-post");
        post.setPublished(true);

        when(repository.getAllPosts()).thenReturn(Arrays.asList(post));

        var result = postService.getPostByIdOrSlug("1");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void getPostByIdOrSlug_findBySlug() {
        Post post = new Post();
        post.setId(1L);
        post.setSlug("test-post");
        post.setPublished(true);

        when(repository.getAllPosts()).thenReturn(Arrays.asList(post));

        var result = postService.getPostByIdOrSlug("test-post");

        assertTrue(result.isPresent());
        assertEquals("test-post", result.get().getSlug());
    }

    @Test
    void getPostByIdOrSlug_notFound() {
        when(repository.getAllPosts()).thenReturn(Arrays.asList());

        var result = postService.getPostByIdOrSlug("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchPosts_byTag() {
        Post post1 = new Post();
        post1.setId(1L);
        post1.setTags(Arrays.asList("Vue", "UI"));
        post1.setPublished(true);

        Post post2 = new Post();
        post2.setId(2L);
        post2.setTags(Arrays.asList("Java", "Backend"));
        post2.setPublished(true);

        when(repository.getAllPosts()).thenReturn(Arrays.asList(post1, post2));

        List<Post> result = postService.searchPosts("Vue", null, false);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void searchPosts_bySearchKeyword() {
        Post post1 = new Post();
        post1.setId(1L);
        post1.setTitle("Vue Introduction");
        post1.setExcerpt("Learn Vue");
        post1.setContent("Vue content");
        post1.setPublished(true);

        Post post2 = new Post();
        post2.setId(2L);
        post2.setTitle("Java Tutorial");
        post2.setExcerpt("Learn Java");
        post2.setContent("Java content");
        post2.setPublished(true);

        when(repository.getAllPosts()).thenReturn(Arrays.asList(post1, post2));

        List<Post> result = postService.searchPosts(null, "vue", false);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void searchPosts_excludesUnpublishedByDefault() {
        Post post1 = new Post();
        post1.setId(1L);
        post1.setPublished(true);

        Post post2 = new Post();
        post2.setId(2L);
        post2.setPublished(false);

        when(repository.getAllPosts()).thenReturn(Arrays.asList(post1, post2));

        List<Post> result = postService.searchPosts(null, null, false);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void searchPosts_includesUnpublishedWhenRequested() {
        Post post1 = new Post();
        post1.setId(1L);
        post1.setPublished(true);

        Post post2 = new Post();
        post2.setId(2L);
        post2.setPublished(false);

        when(repository.getAllPosts()).thenReturn(Arrays.asList(post1, post2));

        List<Post> result = postService.searchPosts(null, null, true);

        assertEquals(2, result.size());
    }

    @Test
    void getAllTags_returnsSortedUniqueTags() {
        Post post1 = new Post();
        post1.setTags(Arrays.asList("Vue", "UI"));

        Post post2 = new Post();
        post2.setTags(Arrays.asList("Java", "UI"));

        when(repository.getAllPosts()).thenReturn(Arrays.asList(post1, post2));

        List<String> result = postService.getAllTags();

        assertEquals(3, result.size());
        assertEquals("Java", result.get(0));
        assertEquals("UI", result.get(1));
        assertEquals("Vue", result.get(2));
    }

    @Test
    void toSlug_normalizesString() {
        assertEquals("vue-3-introduction", PostService.toSlug("Vue 3 Introduction"));
        assertEquals("hello-world", PostService.toSlug("Hello   World"));
        assertEquals("test-post", PostService.toSlug("Test--Post"));
        assertEquals("", PostService.toSlug("   "));
    }
}
