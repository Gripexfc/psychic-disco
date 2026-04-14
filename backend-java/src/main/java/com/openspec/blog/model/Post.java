package com.openspec.blog.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Post {
    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String content;
    private String contentHtml;
    private String date;
    private List<String> tags;
    private Boolean published;
    private Long likes;

    public Post() {}

    public Post(Long id, String slug, String title, String excerpt, String content,
                String contentHtml, String date, List<String> tags, Boolean published, Long likes) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.excerpt = excerpt;
        this.content = content;
        this.contentHtml = contentHtml;
        this.date = date;
        this.tags = tags;
        this.published = published;
        this.likes = likes;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentHtml() { return contentHtml; }
    public void setContentHtml(String contentHtml) { this.contentHtml = contentHtml; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }

    public Long getLikes() { return likes; }
    public void setLikes(Long likes) { this.likes = likes; }
}
