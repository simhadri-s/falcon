package com.laserxprts.falcon.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileUploadService {

    public record UploadedAsset(String secureUrl, String publicId, String originalFilename) {}

    private final Cloudinary cloudinary;

    public FileUploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    private String processUpload(MultipartFile file, String folder, String prefix) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Cannot upload an empty file.");
        }

        String publicId = prefix + "_" + UUID.randomUUID().toString();

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "falcon/" + folder,
                "public_id", publicId,
                "use_filename", true,
                "unique_filename", true
        ));

        
        
        return uploadResult.get("secure_url").toString();
    }

    public String uploadPdf(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new RuntimeException("Validation Failed: Only PDF files are allowed.");
        }

        try {
            return processUpload(file, "cvs", "cv");
        } catch (IOException e) {
            throw new RuntimeException("Error reading PDF file: " + e.getMessage());
        }
    }

    public String uploadImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Validation Failed: Only image files are allowed.");
        }

        try {
            return processUpload(file, "images", "img");
        } catch (IOException e) {
            throw new RuntimeException("Error reading image file: " + e.getMessage());
        }
    }

    public List<String> uploadMultipleImages(List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                imageUrls.add(uploadImage(file));
            }
        }
        return imageUrls;
    }

    public UploadedAsset uploadReceiptPdf(byte[] fileBytes, String fileName, String publicId) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new RuntimeException("Cannot upload an empty receipt file.");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
                    "resource_type", "raw",
                    "folder", "falcon/receipts",
                    "public_id", publicId,
                    "overwrite", true,
                    "invalidate", true,
                    "filename_override", fileName
            ));

            return new UploadedAsset(
                uploadResult.get("secure_url").toString(),
                uploadResult.get("public_id").toString(),
                fileName
            );
        } catch (IOException e) {
            throw new RuntimeException("Error uploading receipt PDF: " + e.getMessage(), e);
        }
    }

    public byte[] downloadFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new RuntimeException("File URL is empty.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .GET()
                .build();

            HttpResponse<byte[]> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("File download failed with status " + response.statusCode());
            }

            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("Unable to download file: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes files from Cloudinary based on their URL
     */
    public void deleteFiles(List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        for (String url : urls) {
            String publicId = extractPublicIdFromUrl(url);
            if (publicId != null) {
                try {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                } catch (Exception e) {
                    System.err.println("Cloudinary deletion failed for: " + publicId + " | " + e.getMessage());
                }
            }
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            // Logic to find the path after /upload/ and before the file extension
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            String afterUpload = url.substring(uploadIndex + 8);
            // Remove versioning (e.g., v12345678/) if present
            if (afterUpload.matches("^v\\d+/.*")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }

            int extensionIndex = afterUpload.lastIndexOf(".");
            return (extensionIndex != -1) ? afterUpload.substring(0, extensionIndex) : afterUpload;
        } catch (Exception e) {
            return null;
        }
    }
}
