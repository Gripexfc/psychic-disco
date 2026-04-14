package com.openspec.blog.model;

public class SiteInfo {
    private String title;
    private String subtitle;
    private String bio;
    private String authorName;

    public SiteInfo() {}

    public SiteInfo(String title, String subtitle, String bio, String authorName) {
        this.title = title;
        this.subtitle = subtitle;
        this.bio = bio;
        this.authorName = authorName;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
}
