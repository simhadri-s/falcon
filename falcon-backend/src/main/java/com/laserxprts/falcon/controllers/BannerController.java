package com.laserxprts.falcon.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laserxprts.falcon.dto.request.BannerRequest;
import com.laserxprts.falcon.model.Banner;
import com.laserxprts.falcon.service.BannerService;

@PreAuthorize("@permissionService.hasAccess('MANAGE_BANNERS')")
@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService){
        this.bannerService = bannerService;
    }

    @PostMapping
    public ResponseEntity<Banner> create(@NonNull @ModelAttribute BannerRequest bannerRequest) {
        Banner banner = new Banner();
        banner.setTitle(bannerRequest.getTitle());
        banner.setDescription(bannerRequest.getDescription());
        banner.setActive(bannerRequest.isActive());
        return new ResponseEntity<>(bannerService.create(banner, bannerRequest.getImage()), HttpStatus.CREATED);
    }

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<List<Banner>> getAll() {
        return ResponseEntity.ok(bannerService.getAll());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Banner> updateStatus(
    @NonNull @PathVariable String id,
    @RequestBody Map<String, Boolean> payload) {
        boolean active = payload.get("active");
        return ResponseEntity.ok(bannerService.updateStatus(id, active));
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/active")
    public ResponseEntity<List<Banner>> getActive() {
        return ResponseEntity.ok(bannerService.getActive());
    }

    @PutMapping("/{id}/default-banner")
    public ResponseEntity<Banner> updateDefaultBanner(
        @NonNull @PathVariable String id,
        @RequestBody Map<String, Boolean> payload
    ) {
        return ResponseEntity.ok(bannerService.updateDefaultBanner(id, payload.get("defaultBanner")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@NonNull @PathVariable String id ) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}