package com.openspec.blog.service;

import com.openspec.blog.model.SiteInfo;
import com.openspec.blog.repository.DataRepository;
import org.springframework.stereotype.Service;

@Service
public class SiteService {

    private final DataRepository repository;

    public SiteService(DataRepository repository) {
        this.repository = repository;
    }

    public SiteInfo getSiteInfo() {
        return repository.getSiteInfo();
    }
}
