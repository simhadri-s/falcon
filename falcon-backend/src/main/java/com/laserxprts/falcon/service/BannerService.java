package com.laserxprts.falcon.service;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.laserxprts.falcon.model.Banner;
import com.laserxprts.falcon.repository.BannerRepository;

@Service
public class BannerService {
    private final BannerRepository bannerRepository;
    private final FileUploadService fileUploadService;

    public BannerService(BannerRepository bannerRepository, FileUploadService fileUploadService) {
        this.bannerRepository = bannerRepository;
        this.fileUploadService = fileUploadService;
    }

    public Banner create (@NonNull Banner banner, MultipartFile image) {
        if ( image == null) {
            throw new RuntimeException("Image cannot be null");
        }
        if (banner.getTitle() == null || banner.getTitle().isBlank()) {
            throw new RuntimeException("Title cannot be null");
        }
        String imageUrl = fileUploadService.uploadImage(image);
        banner.setImageUrl(imageUrl);

        return bannerRepository.save(banner);
    }

    public List<Banner> getAll() {
        return bannerRepository.findAll();
    }
    public List<Banner> getActive() {
        return bannerRepository.findByActiveTrue();
    }
  
    public Banner updateDefaultBanner(@NonNull String id, boolean defaultBanner) {
        Banner banner = bannerRepository.findByDefaultBannerTrue();
        if (banner != null ) {
            banner.setDefaultBanner(false);
            bannerRepository.save(banner);
        }
        Banner updatedBanner = bannerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Banner doesn't exists"));
        updatedBanner.setDefaultBanner(defaultBanner);
        return bannerRepository.save(updatedBanner);
    }
    public Banner updateStatus(@NonNull String id, boolean active) {
        Banner banner = bannerRepository.findById(id)
            .orElseThrow(() -> new  RuntimeException("Banner not found"));
            banner.setActive(active); 
            return bannerRepository.save(banner);
    }
    public void delete(@NonNull String id) {
        Banner banner = bannerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cannot find the banner"));
        fileUploadService.deleteFiles(List.of(banner.getImageUrl()));
        bannerRepository.deleteById(id);
    }
}