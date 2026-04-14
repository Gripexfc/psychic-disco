package com.openspec.blog.controller;

import com.openspec.blog.dto.ApiResponse;
import com.openspec.blog.model.SiteInfo;
import com.openspec.blog.service.SiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping("/api/site")
    public ResponseEntity<ApiResponse<SiteInfo>> getSite() {
        try {
            SiteInfo site = siteService.getSiteInfo();
            return ResponseEntity.ok(ApiResponse.ok(site));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("SITE_READ_FAILED", "Failed to read site information.", e.getMessage()));
        }
    }
}
