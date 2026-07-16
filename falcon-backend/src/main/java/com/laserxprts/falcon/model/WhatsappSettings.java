package com.laserxprts.falcon.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "whatsapp_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappSettings {
    @Id
    private String id;
    
    private String accessToken;
    private String phoneNumberId;
}
