package com.openspec.blog.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openspec.blog.model.Post;
import com.openspec.blog.model.SiteInfo;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class DataRepository {

    @Value("${data.posts-path:data/posts.json}")
    private String postsPath;

    @Value("${data.site-path:data/site.json}")
    private String sitePath;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Post> postsCache;
    private SiteInfo siteCache;

    @PostConstruct
    public void init() throws IOException {
        ensureDataDir();
        loadData();
    }

    private void ensureDataDir() throws IOException {
        Path dataDir = Paths.get("data");
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
    }

    private void loadData() throws IOException {
        Path postsFile = Paths.get(postsPath);
        Path siteFile = Paths.get(sitePath);

        if (Files.exists(postsFile)) {
            String json = Files.readString(postsFile);
            postsCache = objectMapper.readValue(json, new TypeReference<List<Post>>() {});
        } else {
            postsCache = new ArrayList<>();
        }

        if (Files.exists(siteFile)) {
            String json = Files.readString(siteFile);
            siteCache = objectMapper.readValue(json, SiteInfo.class);
        } else {
            siteCache = new SiteInfo("My Blog", "", "");
        }
    }

    public List<Post> getAllPosts() {
        return new ArrayList<>(postsCache);
    }

    public void saveAllPosts(List<Post> posts) throws IOException {
        postsCache = new ArrayList<>(posts);
        String json = objectMapper.writeValueAsString(posts);
        Files.writeString(Paths.get(postsPath), json);
    }

    public SiteInfo getSiteInfo() {
        return siteCache;
    }
}
