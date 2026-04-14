package com.openspec.blog.service;

import com.openspec.blog.dto.SiteStats;
import com.openspec.blog.model.Post;
import com.openspec.blog.repository.DataRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final DataRepository repository;

    public PostService(DataRepository repository) {
        this.repository = repository;
    }

    public List<Post> getAllPosts() {
        return repository.getAllPosts().stream()
                .map(this::normalizePost)
                .collect(Collectors.toList());
    }

    public Optional<Post> getPostByIdOrSlug(String idOrSlug) {
        List<Post> posts = getAllPosts();
        try {
            long id = Long.parseLong(idOrSlug);
            return posts.stream().filter(p -> id == p.getId()).findFirst();
        } catch (NumberFormatException e) {
            return posts.stream().filter(p -> idOrSlug.equals(p.getSlug())).findFirst();
        }
    }

    public List<Post> searchPosts(String tag, String search, boolean includeDraft) {
        return getAllPosts().stream()
                .filter(p -> includeDraft || Boolean.TRUE.equals(p.getPublished()))
                .filter(p -> tag == null || (p.getTags() != null && p.getTags().contains(tag)))
                .filter(p -> search == null || matchesSearch(p, search.toLowerCase()))
                .sorted(Comparator.comparing(Post::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private boolean matchesSearch(Post post, String search) {
        if (search == null || search.isEmpty()) return true;
        String lower = search.toLowerCase();
        return containsLower(post.getTitle(), lower)
                || containsLower(post.getExcerpt(), lower)
                || containsLower(post.getContent(), lower);
    }

    private boolean containsLower(String text, String lower) {
        return text != null && text.toLowerCase().contains(lower);
    }

    public List<String> getAllTags() {
        return getAllPosts().stream()
                .filter(p -> p.getTags() != null)
                .flatMap(p -> p.getTags().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<Post> getRelatedPosts(String slug, int limit) {
        Optional<Post> current = getPostByIdOrSlug(slug);
        if (current.isEmpty() || current.get().getTags() == null) {
            return List.of();
        }
        List<String> currentTags = current.get().getTags();
        return getAllPosts().stream()
                .filter(p -> !p.getSlug().equals(slug) && Boolean.TRUE.equals(p.getPublished()))
                .filter(p -> p.getTags() != null && p.getTags().stream().anyMatch(currentTags::contains))
                .sorted(Comparator.comparing(
                        (Post p) -> (int) p.getTags().stream().filter(currentTags::contains).count()
                ).reversed()
                        .thenComparing(Post::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public SiteStats getSiteStats() {
        List<Post> all = getAllPosts().stream()
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .collect(Collectors.toList());
        long totalViews = all.stream().mapToLong(p -> p.getViews() != null ? p.getViews() : 0).sum();
        long totalLikes = all.stream().mapToLong(p -> p.getLikes() != null ? p.getLikes() : 0).sum();
        return new SiteStats(all.size(), totalViews, totalLikes);
    }

    public Post likePost(String idOrSlug) throws IOException {
        List<Post> posts = repository.getAllPosts();
        int index = findPostIndex(posts, idOrSlug);
        if (index < 0) {
            return null;
        }

        Post post = posts.get(index);
        Post updated = new Post();
        updated.setId(post.getId());
        updated.setSlug(post.getSlug());
        updated.setTitle(post.getTitle());
        updated.setExcerpt(post.getExcerpt());
        updated.setContent(post.getContent());
        updated.setContentHtml(post.getContentHtml());
        updated.setDate(post.getDate());
        updated.setTags(post.getTags());
        updated.setPublished(post.getPublished());
        updated.setLikes(normalizeLikes(post.getLikes()) + 1);
        updated.setViews(normalizeViews(post.getViews()));

        posts.set(index, updated);
        repository.saveAllPosts(posts);
        return normalizePost(updated);
    }

    public Post incrementViews(String idOrSlug) throws IOException {
        List<Post> posts = repository.getAllPosts();
        int index = findPostIndex(posts, idOrSlug);
        if (index < 0) {
            return null;
        }

        Post post = posts.get(index);
        Post updated = new Post();
        updated.setId(post.getId());
        updated.setSlug(post.getSlug());
        updated.setTitle(post.getTitle());
        updated.setExcerpt(post.getExcerpt());
        updated.setContent(post.getContent());
        updated.setContentHtml(post.getContentHtml());
        updated.setDate(post.getDate());
        updated.setTags(post.getTags());
        updated.setPublished(post.getPublished());
        updated.setLikes(normalizeLikes(post.getLikes()));
        updated.setViews(normalizeViews(post.getViews()) + 1);

        posts.set(index, updated);
        repository.saveAllPosts(posts);
        return normalizePost(updated);
    }

    public Post createPost(Post newPost) throws IOException {
        List<Post> posts = repository.getAllPosts();

        long newId = posts.stream()
                .mapToLong(p -> p.getId() != null ? p.getId() : 0)
                .max()
                .orElse(0) + 1;

        String slugBase = toSlug(newPost.getTitle());
        String slug = slugBase.isEmpty() ? "post-" + newId : slugBase;
        int suffix = 1;
        while (existsSlug(posts, slug)) {
            slug = slugBase + "-" + suffix;
            suffix++;
        }

        Post post = new Post();
        post.setId(newId);
        post.setSlug(slug);
        post.setTitle(newPost.getTitle());
        post.setExcerpt(newPost.getExcerpt());
        post.setContent(newPost.getContent());
        post.setContentHtml(newPost.getContentHtml());
        post.setDate(LocalDate.now().toString());
        post.setTags(newPost.getTags() != null
                ? newPost.getTags().stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
                : new ArrayList<>());
        post.setPublished(true);
        post.setLikes(0L);

        posts.add(0, post);
        repository.saveAllPosts(posts);
        return normalizePost(post);
    }

    private boolean existsSlug(List<Post> posts, String slug) {
        return posts.stream().anyMatch(p -> slug.equals(p.getSlug()));
    }

    private int findPostIndex(List<Post> posts, String idOrSlug) {
        try {
            long id = Long.parseLong(idOrSlug);
            for (int i = 0; i < posts.size(); i++) {
                if (id == posts.get(i).getId()) return i;
            }
        } catch (NumberFormatException e) {
            for (int i = 0; i < posts.size(); i++) {
                if (idOrSlug.equals(posts.get(i).getSlug())) return i;
            }
        }
        return -1;
    }

    private Post normalizePost(Post post) {
        if (post == null) return null;
        Post normalized = new Post();
        normalized.setId(post.getId());
        normalized.setSlug(post.getSlug());
        normalized.setTitle(post.getTitle());
        normalized.setExcerpt(post.getExcerpt());
        normalized.setContent(post.getContent());
        normalized.setContentHtml(post.getContentHtml());
        normalized.setDate(post.getDate());
        normalized.setTags(post.getTags());
        normalized.setPublished(post.getPublished());
        normalized.setLikes(normalizeLikes(post.getLikes()));
        normalized.setViews(normalizeViews(post.getViews()));
        return normalized;
    }

    private Long normalizeLikes(Number likes) {
        if (likes == null) return 0L;
        long l = likes.longValue();
        return l >= 0 ? l : 0L;
    }

    private Long normalizeViews(Number views) {
        if (views == null) return 0L;
        long v = views.longValue();
        return v >= 0 ? v : 0L;
    }

    public static String toSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("[^\\p{L}\\p{N}\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return normalized;
    }
}
