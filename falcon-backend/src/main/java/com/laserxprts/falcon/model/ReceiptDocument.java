package com.laserxprts.falcon.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptDocument {
    private String fileName;
    private LocalDateTime generatedAt;

    @JsonIgnore
    private String url;

    @JsonIgnore
    private String publicId;
}
