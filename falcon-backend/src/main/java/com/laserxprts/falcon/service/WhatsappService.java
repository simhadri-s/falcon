package com.laserxprts.falcon.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.laserxprts.falcon.model.Order;
import com.laserxprts.falcon.model.WhatsappSettings;
import com.laserxprts.falcon.repository.AddressRepository;
import com.laserxprts.falcon.repository.WhatsappSettingsRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WhatsappService {

    @Value("${whatsapp.access.token}")
    private String accessToken;

    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;

    private final AddressRepository addressRepository;
    private final WhatsappSettingsRepository whatsappSettingsRepository;
    private final RestTemplate restTemplate;

    public WhatsappService(AddressRepository addressRepository, WhatsappSettingsRepository whatsappSettingsRepository) {
        this.addressRepository = addressRepository;
        this.whatsappSettingsRepository = whatsappSettingsRepository;
        this.restTemplate = new RestTemplate();
    }

    private String getActiveAccessToken() {
        return whatsappSettingsRepository.findById("WHATSAPP_SETTINGS")
                .map(WhatsappSettings::getAccessToken)
                .filter(token -> token != null && !token.isBlank())
                .orElse(accessToken);
    }

    private String getActivePhoneNumberId() {
        return whatsappSettingsRepository.findById("WHATSAPP_SETTINGS")
                .map(WhatsappSettings::getPhoneNumberId)
                .filter(id -> id != null && !id.isBlank())
                .orElse(phoneNumberId);
    }


    private void sendMetaTemplateMessage(String toPhoneNumber, String templateName, List<String> parameters) {
        String currentToken = getActiveAccessToken();
        if (currentToken != null) currentToken = currentToken.trim();
        
        String currentPhoneId = getActivePhoneNumberId();
        if (currentPhoneId != null) currentPhoneId = currentPhoneId.trim();

        if (currentToken == null || currentToken.isBlank() || currentPhoneId == null || currentPhoneId.isBlank()) {
            log.error("WhatsApp credentials not configured. Cannot send template message to {}", toPhoneNumber);
            return;
        }

        try {
            String url = "https://graph.facebook.com/v19.0/" + currentPhoneId + "/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(currentToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("recipient_type", "individual");
            String formattedPhone = toPhoneNumber.replaceAll("[^0-9]", "");
            if (formattedPhone.length() == 10) {
                formattedPhone = "91" + formattedPhone;
            }
            
            requestBody.put("to", formattedPhone);
            requestBody.put("type", "template");

            Map<String, Object> templateObj = new HashMap<>();
            templateObj.put("name", templateName);
            Map<String, String> language = new HashMap<>();
            language.put("code", "en_US"); // Assuming templates are created in English (US)
            templateObj.put("language", language);

            if (parameters != null && !parameters.isEmpty()) {
                List<Map<String, String>> paramList = new java.util.ArrayList<>();
                for (String param : parameters) {
                    Map<String, String> p = new HashMap<>();
                    p.put("type", "text");
                    p.put("text", param != null ? param : "");
                    paramList.add(p);
                }
                
                Map<String, Object> component = new HashMap<>();
                component.put("type", "body");
                component.put("parameters", paramList);

                templateObj.put("components", java.util.Arrays.asList(component));
            }

            requestBody.put("template", templateObj);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("WhatsApp template sent successfully to {}. Response: {}", toPhoneNumber, response.getBody());

        } catch (Exception e) {
            log.error("Failed to send WhatsApp template message to {}: {}", toPhoneNumber, e.getMessage());
        }
    }

    @Async
    public void sendOrderStatusUpdateWhatsapp(Order order, String newStatus) {
        if (order.getAddressSnapshot() == null || order.getAddressSnapshot().getPhoneNumber() == null) {
            return;
        }
        
        String phone = order.getAddressSnapshot().getPhoneNumber();
        String name = order.getAddressSnapshot().getFullName();
        if (name == null || name.isBlank()) name = "Customer";
        
        String orderId = order.getId();
        String statusUpper = newStatus.toUpperCase();
        
        String templateName;
        List<String> parameters;

        if (statusUpper.equals("CANCELLED")) {
            // Template: order_canceled (Name, OrderId)
            templateName = "order_canceled";
            parameters = java.util.Arrays.asList(name, orderId);
        } else if (statusUpper.equals("DELIVERED")) {
            // Template: order_delivered (Name, Date, OrderId)
            templateName = "order_delivered";
            String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            parameters = java.util.Arrays.asList(name, dateStr, orderId);
        } else {
            // Template: order_update (Name, OrderId, Status)
            templateName = "order_update"; // We use standard name convention 'order_update' despite the screenshot typo 'Udpate'
            parameters = java.util.Arrays.asList(name, orderId, statusUpper);
        }

        sendMetaTemplateMessage(phone, templateName, parameters);
    }


}
