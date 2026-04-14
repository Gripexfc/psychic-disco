package com.openspec.blog.dto;

public class SiteStats {
    private long totalPosts;
    private long totalViews;
    private long totalLikes;

    public SiteStats() {}

    public SiteStats(long totalPosts, long totalViews, long totalLikes) {
        this.totalPosts = totalPosts;
        this.totalViews = totalViews;
        this.totalLikes = totalLikes;
    }

    public long getTotalPosts() { return totalPosts; }
    public void setTotalPosts(long totalPosts) { this.totalPosts = totalPosts; }

    public long getTotalViews() { return totalViews; }
    public void setTotalViews(long totalViews) { this.totalViews = totalViews; }

    public long getTotalLikes() { return totalLikes; }
    public void setTotalLikes(long totalLikes) { this.totalLikes = totalLikes; }
}
