package com.laserxprts.falcon.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "mailer_settings")
public class MailerSettings {
    
    @Id
    private String id;              // e.g. "MAILER_SETTINGS"
    private String mailHost;        // e.g. "smtp.gmail.com"
    private int mailPort;           // e.g. 587
    private String mailUsername;    // e.g. "youvideontube@gmail.com"
    private String mailPassword;    // e.g. SMTP/App Password

}
