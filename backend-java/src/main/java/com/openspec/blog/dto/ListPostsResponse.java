package com.openspec.blog.dto;

import com.openspec.blog.model.Post;
import java.util.List;

public class ListPostsResponse {
    private List<Post> items;

    public ListPostsResponse() {}

    public ListPostsResponse(List<Post> items) {
        this.items = items;
    }

    public List<Post> getItems() { return items; }
    public void setItems(List<Post> items) { this.items = items; }
}
