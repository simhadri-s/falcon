package com.laserxprts.falcon.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.laserxprts.falcon.dto.request.CompanyImageRequest;
import com.laserxprts.falcon.exception.ApiException;
import com.laserxprts.falcon.exception.ResourceNotFoundException;
import com.laserxprts.falcon.model.CompanyImage;
import com.laserxprts.falcon.model.CompanySettings;
import com.laserxprts.falcon.repository.CompanyImageRepository;
import com.laserxprts.falcon.repository.CompanySettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyImageService {

    private static final String COMPANY_IMAGE_NOT_FOUND = "Company image not found";
    private static final String COMPANY_SETTINGS_ID = "COMPANY_SETTINGS";

    private final CompanyImageRepository companyImageRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final FileUploadService fileUploadService;

    public synchronized CompanyImage createCompanyImage(CompanyImageRequest companyImageRequest) {
        List<CompanyImage> existingImages = normalizeCompanyImages();
        if (!existingImages.isEmpty()) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "A company image already exists. Update or delete the existing entry first."
            );
        }

        CompanyImage companyImage = new CompanyImage();
        companyImage.setName(normalizeRequiredName(companyImageRequest.getName()));
        companyImage.setDescription(normalizeDescription(companyImageRequest.getDescription()));
        
        companyImage.setLogoUrl(uploadRequiredLogo(companyImageRequest.getLogo()));
        
        if (hasImage(companyImageRequest.getIcon())) {
            companyImage.setIconUrl(fileUploadService.uploadImage(companyImageRequest.getIcon()));
        }
        if (hasImage(companyImageRequest.getFavicon())) {
            companyImage.setFaviconUrl(fileUploadService.uploadImage(companyImageRequest.getFavicon()));
        }
        if (hasImage(companyImageRequest.getLandingPageImage())) {
            companyImage.setLandingPageImageUrl(fileUploadService.uploadImage(companyImageRequest.getLandingPageImage()));
        }

        CompanyImage savedImage = companyImageRepository.save(companyImage);
        syncCompanySettingsLogo(savedImage.getLogoUrl());
        return savedImage;
    }

    public List<CompanyImage> getAllCompanyImages() {
        return normalizeCompanyImages();
    }

    public CompanyImage getCompanyImageById(String id) {
        validateId(id);

        List<CompanyImage> companyImages = companyImageRepository.findAll();
        if (companyImages.isEmpty()) {
            throw new ResourceNotFoundException(COMPANY_IMAGE_NOT_FOUND);
        }

        CompanyImage companyImage = companyImages.stream()
            .filter(image -> id.equals(image.getId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(COMPANY_IMAGE_NOT_FOUND));

        if (companyImages.size() > 1) {
            keepOnly(companyImages, companyImage);
        }

        return companyImage;
    }

    public synchronized CompanyImage updateCompanyImage(String id, CompanyImageRequest companyImageRequest) {
        validateId(id);

        List<CompanyImage> companyImages = companyImageRepository.findAll();
        if (companyImages.isEmpty()) {
            throw new ResourceNotFoundException(COMPANY_IMAGE_NOT_FOUND);
        }

        CompanyImage companyImage = companyImages.stream()
            .filter(image -> id.equals(image.getId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(COMPANY_IMAGE_NOT_FOUND));

        if (companyImages.size() > 1) {
            keepOnly(companyImages, companyImage);
        }

        companyImage.setName(normalizeRequiredName(companyImageRequest.getName()));
        companyImage.setDescription(normalizeDescription(companyImageRequest.getDescription()));

        List<String> previousUrls = new ArrayList<>();

        if (hasImage(companyImageRequest.getLogo())) {
            previousUrls.add(companyImage.getLogoUrl());
            companyImage.setLogoUrl(fileUploadService.uploadImage(companyImageRequest.getLogo()));
        }
        if (hasImage(companyImageRequest.getIcon())) {
            previousUrls.add(companyImage.getIconUrl());
            companyImage.setIconUrl(fileUploadService.uploadImage(companyImageRequest.getIcon()));
        }
        if (hasImage(companyImageRequest.getFavicon())) {
            previousUrls.add(companyImage.getFaviconUrl());
            companyImage.setFaviconUrl(fileUploadService.uploadImage(companyImageRequest.getFavicon()));
        }
        if (hasImage(companyImageRequest.getLandingPageImage())) {
            previousUrls.add(companyImage.getLandingPageImageUrl());
            companyImage.setLandingPageImageUrl(fileUploadService.uploadImage(companyImageRequest.getLandingPageImage()));
        }

        CompanyImage savedImage = companyImageRepository.save(companyImage);
        syncCompanySettingsLogo(savedImage.getLogoUrl());
        deleteImagesIfUnused(previousUrls, List.of(savedImage));
        return savedImage;
    }

    public synchronized void deleteCompanyImage(String id) {
        validateId(id);

        List<CompanyImage> companyImages = companyImageRepository.findAll();
        if (companyImages.isEmpty()) {
            throw new ResourceNotFoundException(COMPANY_IMAGE_NOT_FOUND);
        }

        CompanyImage companyImage = companyImages.stream()
            .filter(image -> id.equals(image.getId()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(COMPANY_IMAGE_NOT_FOUND));

        List<CompanyImage> remainingImages = new ArrayList<>();
        for (CompanyImage existingImage : companyImages) {
            if (!Objects.equals(existingImage.getId(), companyImage.getId())) {
                remainingImages.add(existingImage);
            }
        }

        if (remainingImages.size() > 1) {
            CompanyImage keeper = remainingImages.get(0);
            keepOnly(remainingImages, keeper);
            remainingImages = List.of(keeper);
        }

        companyImageRepository.deleteById(companyImage.getId());
        
        List<String> imagesToDelete = new ArrayList<>();
        if (hasText(companyImage.getLogoUrl())) imagesToDelete.add(companyImage.getLogoUrl());
        if (hasText(companyImage.getIconUrl())) imagesToDelete.add(companyImage.getIconUrl());
        if (hasText(companyImage.getFaviconUrl())) imagesToDelete.add(companyImage.getFaviconUrl());
        if (hasText(companyImage.getLandingPageImageUrl())) imagesToDelete.add(companyImage.getLandingPageImageUrl());
        
        deleteImagesIfUnused(imagesToDelete, remainingImages);
        syncCompanySettingsLogo(remainingImages.isEmpty() ? null : remainingImages.get(0).getLogoUrl());
    }

    private List<CompanyImage> normalizeCompanyImages() {
        List<CompanyImage> companyImages = companyImageRepository.findAll();
        if (companyImages.size() <= 1) {
            return companyImages;
        }

        CompanyImage keeper = companyImages.get(0);
        keepOnly(companyImages, keeper);
        return List.of(keeper);
    }

    private void keepOnly(List<CompanyImage> companyImages, CompanyImage keeper) {
        List<CompanyImage> duplicates = new ArrayList<>();
        for (CompanyImage companyImage : companyImages) {
            if (!Objects.equals(companyImage.getId(), keeper.getId())) {
                duplicates.add(companyImage);
            }
        }

        if (duplicates.isEmpty()) {
            return;
        }

        companyImageRepository.deleteAll(duplicates);

        List<String> duplicateUrls = new ArrayList<>();
        for (CompanyImage duplicate : duplicates) {
            addIfUnused(duplicateUrls, duplicate.getLogoUrl(), keeper);
            addIfUnused(duplicateUrls, duplicate.getIconUrl(), keeper);
            addIfUnused(duplicateUrls, duplicate.getFaviconUrl(), keeper);
            addIfUnused(duplicateUrls, duplicate.getLandingPageImageUrl(), keeper);
        }

        if (!duplicateUrls.isEmpty()) {
            fileUploadService.deleteFiles(duplicateUrls);
        }
    }

    private void addIfUnused(List<String> urls, String url, CompanyImage keeper) {
        if (hasText(url)
            && !Objects.equals(url, keeper.getLogoUrl())
            && !Objects.equals(url, keeper.getIconUrl())
            && !Objects.equals(url, keeper.getFaviconUrl())
            && !Objects.equals(url, keeper.getLandingPageImageUrl())
            && !urls.contains(url)) {
            urls.add(url);
        }
    }

    private String uploadRequiredLogo(MultipartFile logo) {
        if (!hasImage(logo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please select an image");
        }
        return fileUploadService.uploadImage(logo);
    }

    private void deleteImagesIfUnused(List<String> urls, List<CompanyImage> retainedImages) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

        List<String> toDelete = new ArrayList<>();
        for (String url : urls) {
            if (!hasText(url)) continue;
            
            boolean isUsed = false;
            for (CompanyImage retainedImage : retainedImages) {
                if (Objects.equals(url, retainedImage.getLogoUrl())
                    || Objects.equals(url, retainedImage.getIconUrl())
                    || Objects.equals(url, retainedImage.getFaviconUrl())
                    || Objects.equals(url, retainedImage.getLandingPageImageUrl())) {
                    isUsed = true;
                    break;
                }
            }
            
            if (!isUsed) {
                toDelete.add(url);
            }
        }

        if (!toDelete.isEmpty()) {
            fileUploadService.deleteFiles(toDelete);
        }
    }

    private void validateId(String id) {
        if (!hasText(id)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid company image ID");
        }
    }

    private String normalizeRequiredName(String name) {
        if (!hasText(name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name cannot be blank");
        }
        return name.trim();
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private boolean hasImage(MultipartFile image) {
        return image != null && !image.isEmpty();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void syncCompanySettingsLogo(String logoUrl) {
        CompanySettings settings = companySettingsRepository.findById(COMPANY_SETTINGS_ID)
            .orElseGet(() -> {
                CompanySettings companySettings = new CompanySettings();
                companySettings.setId(COMPANY_SETTINGS_ID);
                return companySettings;
            });
        settings.setLogoUrl(logoUrl);
        companySettingsRepository.save(settings);
    }
}
