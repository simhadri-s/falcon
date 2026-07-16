package com.laserxprts.falcon.service;

import com.laserxprts.falcon.repository.IndustryRepository;
import com.laserxprts.falcon.dto.request.IndustryRequest;
import com.laserxprts.falcon.dto.response.IndustryResponse;
import com.laserxprts.falcon.model.Industry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IndustryService {

  
    private final IndustryRepository industryRepository;
    private final FileUploadService fileUploadService;

    public IndustryService(IndustryRepository industryRepository, FileUploadService fileUploadService) {
        this.industryRepository = industryRepository;
        this.fileUploadService = fileUploadService;
    }

    public List<IndustryResponse> getAllIndustries() {
        return industryRepository.findAll().stream()
            .map(IndustryResponse::from)
            .collect(Collectors.toList());
    }

    public IndustryResponse createIndustry(IndustryRequest industryRequest) {
        Industry industry = new Industry();
        industry.setName(industryRequest.getName());
        industry.setDescription(industryRequest.getDescription());
        if (industry.getName() == null || industry.getName().isBlank()) {
            throw new RuntimeException("Name cannot be null");
        }
        String slug = generateSlug(industry.getName());
        if (industryRepository.existsBySlug(slug)) {
            throw new RuntimeException("Industry or slug  already exitst");
        }
        if ( industryRequest.getIcon() != null ) {
            String iconUrl = fileUploadService.uploadImage(industryRequest.getIcon());
            industry.setIconUrl(iconUrl);
        }
        industry.setSlug(slug);
        return IndustryResponse.from(industryRepository.save(industry));
    }

    public IndustryResponse updateIndustry(String id, IndustryRequest industryRequest) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("ID can not be null");
        }
        Industry existing = industryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Industry not found"));

        if (industryRequest.getName() != null && !industryRequest.getName().isBlank()) {
            existing.setName(industryRequest.getName());
            existing.setSlug(generateSlug(industryRequest.getName()));
        }

        if (industryRequest.getDescription()!= null && !industryRequest.getDescription().isBlank()) {
            existing.setDescription(industryRequest.getDescription());
        }
        
        if (industryRequest.getIcon() != null) {
            if (existing.getIconUrl() != null && !existing.getIconUrl().isBlank()) {
                fileUploadService.deleteFiles(List.of(existing.getIconUrl()));
            } 
            String iconUrl = fileUploadService.uploadImage(industryRequest.getIcon());
            existing.setIconUrl(iconUrl);
        }

        return IndustryResponse.from(industryRepository.save(existing));
    }

    public void deleteIndustry(String id) {
        if (id == null || id.isBlank()) {
            throw new RuntimeException("Id can not be null");
        }
        industryRepository.deleteById(id);
    }

    public IndustryResponse getBySlug(String slug) {
        return IndustryResponse.from(
            industryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Not found"))
        );
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .trim()
            .replaceAll("\\s+", "-");
    }
}
