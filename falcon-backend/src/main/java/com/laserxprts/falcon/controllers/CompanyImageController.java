package com.laserxprts.falcon.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import com.laserxprts.falcon.dto.request.CompanyImageRequest;
import com.laserxprts.falcon.model.CompanyImage;
import com.laserxprts.falcon.service.CompanyImageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@PreAuthorize("@permissionService.hasAccess('MANAGE_COMPANYIMAGES')")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/company-images")
public class CompanyImageController {

    private final CompanyImageService companyImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompanyImage> createCompanyImage(
        @Valid @ModelAttribute CompanyImageRequest companyImageRequest
    ) {
        CompanyImage createdImage = companyImageService.createCompanyImage(companyImageRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdImage);
    }

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<List<CompanyImage>> getAllCompanyImages() {
        return ResponseEntity.ok(companyImageService.getAllCompanyImages());
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/{id}")
    public ResponseEntity<CompanyImage> getCompanyImageById(@PathVariable String id) {
        return ResponseEntity.ok(companyImageService.getCompanyImageById(id));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompanyImage> updateCompanyImage(
        @PathVariable String id,
        @Valid @ModelAttribute CompanyImageRequest companyImageRequest
    ) {
        return ResponseEntity.ok(companyImageService.updateCompanyImage(id, companyImageRequest));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanyImage> updateCompanyImageDetails(
        @PathVariable String id,
        @RequestBody CompanyImage companyImage
    ) {
        CompanyImageRequest companyImageRequest = new CompanyImageRequest();
        companyImageRequest.setName(companyImage.getName());
        companyImageRequest.setDescription(companyImage.getDescription());

        return ResponseEntity.ok(companyImageService.updateCompanyImage(id, companyImageRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompanyImage(@PathVariable String id) {
        companyImageService.deleteCompanyImage(id);
        return ResponseEntity.noContent().build();
    }
}
