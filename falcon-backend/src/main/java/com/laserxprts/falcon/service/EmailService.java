package com.laserxprts.falcon.service;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.laserxprts.falcon.model.AddressSnapshot;
import com.laserxprts.falcon.model.CompanySettings;
import com.laserxprts.falcon.model.MailerSettings;
import com.laserxprts.falcon.model.Order;
import com.laserxprts.falcon.model.OrderItem;
import com.laserxprts.falcon.repository.CompanySettingsRepository;
import com.laserxprts.falcon.repository.MailerSettingsRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final CompanySettingsRepository companySettingsRepository;
    private final MailerSettingsRepository mailerSettingsRepository;

    @Value("${spring.mail.username}")
    private String adminMail;

    public EmailService(JavaMailSender mailSender, 
                        CompanySettingsRepository companySettingsRepository,
                        MailerSettingsRepository mailerSettingsRepository) {
        this.mailSender = mailSender;
        this.companySettingsRepository = companySettingsRepository;
        this.mailerSettingsRepository = mailerSettingsRepository;
    }

    private JavaMailSender getDynamicMailSender() {
        return mailerSettingsRepository.findById("MAILER_SETTINGS")
                .map(settings -> {
                    if (settings.getMailUsername() == null || settings.getMailUsername().trim().isEmpty()) {
                        return mailSender;
                    }
                    
                    JavaMailSenderImpl dynamicSender = new JavaMailSenderImpl();
                    dynamicSender.setHost(settings.getMailHost() != null && !settings.getMailHost().trim().isEmpty() 
                            ? settings.getMailHost() : "smtp.gmail.com");
                    dynamicSender.setPort(settings.getMailPort() > 0 ? settings.getMailPort() : 587);
                    dynamicSender.setUsername(settings.getMailUsername());
                    dynamicSender.setPassword(settings.getMailPassword() != null ? settings.getMailPassword() : "");
                    
                    java.util.Properties props = dynamicSender.getJavaMailProperties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.debug", "false");
                    
                    return (JavaMailSender) dynamicSender;
                })
                .orElse(mailSender);
    }

    private String getSenderEmail() {
        String mailerUsername = mailerSettingsRepository.findById("MAILER_SETTINGS")
                .map(MailerSettings::getMailUsername)
                .filter(email -> email != null && !email.trim().isEmpty())
                .orElse(null);
        if (mailerUsername != null) {
            return mailerUsername;
        }

        return companySettingsRepository.findById("COMPANY_SETTINGS")
                .map(CompanySettings::getEmail)
                .orElse(adminMail);
    }

    public String getCompanyName() {
        return companySettingsRepository.findById("COMPANY_SETTINGS")
                .map(CompanySettings::getCompanyName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .orElse("Falcon Store");
    }

    private String buildItemRowsHtml(Order order) {
        StringBuilder itemRows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            itemRows.append("<tr style='border-bottom: 1px solid #eee;'>")
                .append("<td style='padding: 10px 0; color: #424242;'>")
                .append(item.getProductSnapshot().getName())
                .append("</td>")
                .append("<td style='padding: 10px 0; text-align: center; color: #424242;'>")
                .append(item.getQuantity())
                .append("</td>")
                .append("</tr>");
        }
        return itemRows.toString();
    }

    private String buildShippingAddressHtml(Order order) {
        AddressSnapshot address = order.getAddressSnapshot();
        return "<div style=\"background-color: #f8f9fa; padding: 20px; border-radius: 6px; margin-bottom: 30px;\">" +
               "    <h3 style=\"margin: 0 0 10px; color: #1a237e; font-size: 16px;\">Shipping Address</h3>" +
               "    <p style=\"margin: 0; color: #424242; line-height: 1.5;\">" +
               "        <strong>" + address.getFullName() + "</strong><br>" +
               "        " + address.getStreet() + "<br>" +
               "        " + address.getCity() + ", " + address.getPincode() + "<br>" +
               "        " + address.getCountry() + "<br>" +
               "        Phone: " + address.getPhoneNumber() + "" +
               "    </p>" +
               "</div>";
    }

    private String getDisplayOrderId(Order order) {
        return order.getId();
    }

    @Async
    public void sendUserInquiryMail(@NonNull String toEmail, @NonNull String subject, @NonNull String htmlBody) {
        try {
            JavaMailSender dynamicSender = getDynamicMailSender();
            MimeMessage message = dynamicSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(getSenderEmail());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            dynamicSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Error sending mail: " + e.getMessage());
            throw new RuntimeException("Failed to send mail");
        }
    }

    @Async
    public void sendAdminInquiryMail(@NonNull String subject, @NonNull String adminMailBody) {
        try {
            JavaMailSender dynamicSender = getDynamicMailSender();
            MimeMessage message = dynamicSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String sender = getSenderEmail();
            Objects.requireNonNull(sender);
            helper.setFrom(sender);
            helper.setTo(sender);
            helper.setSubject(subject);
            helper.setText(adminMailBody, true);
            dynamicSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send admin mail");
            throw new RuntimeException("unable to send admin inquiry mail");
        }
    }

    @Async
    public void sendJobApplicationMail(@NonNull String email,@NonNull String subject,@NonNull String htmlBody) {
        try {
            JavaMailSender dynamicSender = getDynamicMailSender();
            MimeMessage message = dynamicSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message);
            String sender = getSenderEmail();
            Objects.requireNonNull(sender);
            helper.setFrom(sender);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            dynamicSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send Job Application mail");
            throw new RuntimeException("unable to send Job Application mail");
        }
    }

    @Async
    public void sendOrderConfirmationMail(@NonNull Order order, byte[] receiptContent, String receiptFileName) {
        try {
            String toEmail = order.getUserId();
            String displayOrderId = getDisplayOrderId(order);
            String subject = "Order Confirmation - #" + displayOrderId;
            
            String orderDate = order.getCreatedAt() != null 
                ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) 
                : "";
            String companyName = getCompanyName();
            boolean hasReceiptAttachment = receiptContent != null && receiptContent.length > 0;
            String receiptNote = hasReceiptAttachment
                ? "<div style=\"margin: 0 0 24px; padding: 14px 16px; background-color: #eef4ff; border: 1px solid #d7e3ff; border-radius: 6px; color: #1a237e; font-size: 14px;\">Your PDF receipt is attached to this email for easy download.</div>"
                : "";
            String htmlBody = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05);\">" +
                "    <div style=\"background-color: #1a237e; color: white; padding: 30px; text-align: center;\">" +
                "        <h1 style=\"margin: 0; font-size: 24px; font-weight: 600;\">Order Confirmed!</h1>" +
                "        <p style=\"margin: 10px 0 0; opacity: 0.8;\">Thank you for your purchase from " + companyName + ".</p>" +
                "    </div>" +
                "    <div style=\"padding: 30px;\">" +
                "        <div style=\"display: flex; justify-content: space-between; margin-bottom: 30px; padding-bottom: 20px; border-bottom: 2px solid #f5f5f5;\">" +
                "            <div>" +
                "                <p style=\"margin: 0; color: #757575; font-size: 12px; text-transform: uppercase; letter-spacing: 1px;\">Order ID</p>" +
                "                <p style=\"margin: 5px 0 0; font-weight: 600; color: #212121;\">#" + displayOrderId + "</p>" +
                "            </div>" +
                "            <div style=\"text-align: right;\">" +
                "                <p style=\"margin: 0; color: #757575; font-size: 12px; text-transform: uppercase; letter-spacing: 1px;\">Order Date</p>" +
                "                <p style=\"margin: 5px 0 0; font-weight: 600; color: #212121;\">" + orderDate + "</p>" +
                "            </div>" +
                "        </div>" +
                receiptNote +
                "        <h3 style=\"margin: 0 0 15px; color: #1a237e; font-size: 18px;\">Order Summary</h3>" +
                "        <table style=\"width: 100%; border-collapse: collapse; margin-bottom: 30px;\">" +
                "            <thead>" +
                "                <tr style=\"border-bottom: 1px solid #eee;\">" +
                "                    <th style=\"text-align: left; padding: 10px 0; color: #757575; font-weight: 500;\">Product</th>" +
                "                    <th style=\"text-align: center; padding: 10px 0; color: #757575; font-weight: 500;\">Qty</th>" +
                "                </tr>" +
                "            </thead>" +
                "            <tbody>" + buildItemRowsHtml(order) + "</tbody>" +
                "        </table>" +
                buildShippingAddressHtml(order) +
                "        <div style=\"text-align: center;\">" +
                "            <p style=\"color: #757575; font-size: 14px;\">If you have any questions, please contact our support team.</p>" +
                "            <p style=\"margin-top: 20px; font-weight: 600; color: #1a237e;\">Team " + companyName + "</p>" +
                "        </div>" +
                "    </div>" +
                "    <div style=\"background-color: #f5f5f5; padding: 20px; text-align: center; color: #9e9e9e; font-size: 12px;\">" +
                "        &copy; 2026 " + companyName + ". All rights reserved." +
                "    </div>" +
                "</div>";

            JavaMailSender dynamicSender = getDynamicMailSender();
            MimeMessage message = dynamicSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(getSenderEmail());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (hasReceiptAttachment) {
                String attachmentFileName = receiptFileName != null && !receiptFileName.isBlank()
                    ? receiptFileName
                    : "receipt-" + displayOrderId + ".pdf";
                helper.addAttachment(attachmentFileName, new ByteArrayResource(receiptContent));
            }
            dynamicSender.send(message);
        } catch (Exception e) {
            System.err.println("Error sending order confirmation mail: " + e.getMessage());
        }
    }

    @Async
    public void sendOrderStatusUpdateMail(@NonNull Order order, @NonNull String newStatus) {
        try {
            String toEmail = order.getUserId();
            String displayOrderId = getDisplayOrderId(order);
            String subject = "Order Status Update - #" + displayOrderId;
            
            String statusText = newStatus.replace("_", " ");
            String statusColor = "#1a237e"; // Default
            String messageBody = "Your order status has been updated to <strong>" + statusText + "</strong>.";

            // Customize based on status
            if ("SHIPPED".equalsIgnoreCase(newStatus)) {
                statusColor = "#f57c00";
                messageBody = "Great news! Your order has been shipped and is on its way to you.";
            } else if ("DELIVERED".equalsIgnoreCase(newStatus)) {
                statusColor = "#388e3c";
                messageBody = "Your order has been delivered. We hope you enjoy your purchase!";
            } else if ("CANCELLED".equalsIgnoreCase(newStatus)) {
                statusColor = "#d32f2f";
                messageBody = "Your order has been cancelled.";
            } else if ("OUT_FOR_DELIVERY".equalsIgnoreCase(newStatus)) {
                statusColor = "#0288d1";
                messageBody = "Your order is out for delivery and will reach you shortly.";
            }

            String orderDate = order.getCreatedAt() != null 
                ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) 
                : "";

            String companyName = getCompanyName();
            String htmlBody = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;\">" +
                "    <div style=\"background-color: " + statusColor + "; color: white; padding: 25px; text-align: center;\">" +
                "        <h1 style=\"margin: 0; font-size: 22px;\">Order Update</h1>" +
                "        <p style=\"margin: 5px 0 0; opacity: 0.9;\">Order #" + displayOrderId + "</p>" +
                "    </div>" +
                "    <div style=\"padding: 30px; line-height: 1.6; color: #424242;\">" +
                "        <p style=\"font-size: 16px;\">Hi,</p>" +
                "        <p style=\"font-size: 16px;\">" + messageBody + "</p>" +
                "        " +
                "        <div style=\"margin: 25px 0; padding: 20px; background-color: #f8f9fa; border-radius: 6px; border-left: 4px solid " + statusColor + ";\">" +
                "            <p style=\"margin: 0 0 10px; font-weight: 600; color: #212121;\">Order Details:</p>" +
                "            <p style=\"margin: 0; font-size: 14px;\">Status: <span style=\"color: " + statusColor + "; font-weight: 600;\">" + statusText + "</span></p>" +
                "            <p style=\"margin: 5px 0 0; font-size: 14px;\">Order Date: " + orderDate + "</p>" +
                "        </div>" +
                "        " +
                "        <h3 style=\"margin: 0 0 15px; color: #1a237e; font-size: 18px;\">Order Summary</h3>" +
                "        <table style=\"width: 100%; border-collapse: collapse; margin-bottom: 30px;\">" +
                "            <thead>" +
                "                <tr style=\"border-bottom: 1px solid #eee;\">" +
                "                    <th style=\"text-align: left; padding: 10px 0; color: #757575; font-weight: 500;\">Product</th>" +
                "                    <th style=\"text-align: center; padding: 10px 0; color: #757575; font-weight: 500;\">Qty</th>" +
                "                </tr>" +
                "            </thead>" +
                "            <tbody>" + buildItemRowsHtml(order) + "</tbody>" +
                "        </table>" +
                buildShippingAddressHtml(order) +
                "        " +
                "        <p style=\"font-size: 14px; color: #757575;\">You can track your order status in the app at any time.</p>" +
                "        " +
                "        <div style=\"margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; text-align: center;\">" +
                "            <p style=\"margin: 0; font-weight: 600; color: #1a237e;\">Team " + companyName + "</p>" +
                "        </div>" +
                "    </div>" +
                "    <div style=\"background-color: #f5f5f5; padding: 15px; text-align: center; color: #9e9e9e; font-size: 12px;\">" +
                "        &copy; 2026 " + companyName + ". All rights reserved." +
                "    </div>" +
                "</div>";

            JavaMailSender dynamicSender = getDynamicMailSender();
            MimeMessage message = dynamicSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(getSenderEmail());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            dynamicSender.send(message);
        } catch (Exception e) {
            System.err.println("Error sending order status update mail: " + e.getMessage());
        }
    }

    @Async
    public void sendLowStockAlertMail(@NonNull String productName, int remainingStock) {
        try {
            String adminEmail = getSenderEmail();
            String companyName = getCompanyName();
            String subject = "⚠️ Low Stock Alert: " + productName;

            String statusColor = remainingStock == 0 ? "#d32f2f" : "#f57c00";
            String statusLabel = remainingStock == 0 ? "OUT OF STOCK" : "LOW STOCK";
            String message = remainingStock == 0
                    ? "The product <strong>" + productName + "</strong> is now <strong>out of stock</strong>. Please restock immediately."
                    : "The product <strong>" + productName + "</strong> has only <strong>" + remainingStock + " unit(s)</strong> remaining. Consider restocking soon.";

            String htmlBody = "<div style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05);\">"
                    + "    <div style=\"background-color: " + statusColor + "; color: white; padding: 25px; text-align: center;\">"
                    + "        <h1 style=\"margin: 0; font-size: 22px;\">⚠️ " + statusLabel + "</h1>"
                    + "        <p style=\"margin: 8px 0 0; opacity: 0.9;\">" + companyName + " Inventory Alert</p>"
                    + "    </div>"
                    + "    <div style=\"padding: 30px; color: #424242;\">"
                    + "        <p style=\"font-size: 16px; line-height: 1.6;\">" + message + "</p>"
                    + "        <div style=\"margin: 25px 0; padding: 20px; background-color: #f8f9fa; border-radius: 6px; border-left: 4px solid " + statusColor + ";\">"
                    + "            <p style=\"margin: 0 0 8px; font-weight: 600; color: #212121;\">Stock Details</p>"
                    + "            <p style=\"margin: 0; font-size: 14px;\">Product: <strong>" + productName + "</strong></p>"
                    + "            <p style=\"margin: 6px 0 0; font-size: 14px;\">Remaining Stock: <span style=\"color: " + statusColor + "; font-weight: 700;\">" + remainingStock + "</span></p>"
                    + "        </div>"
                    + "        <p style=\"font-size: 13px; color: #757575;\">This is an automated alert from your inventory management system. Please log in to the admin panel to update the stock.</p>"
                    + "    </div>"
                    + "    <div style=\"background-color: #f5f5f5; padding: 15px; text-align: center; color: #9e9e9e; font-size: 12px;\">"
                    + "        &copy; 2026 " + companyName + ". All rights reserved."
                    + "    </div>"
                    + "</div>";

            JavaMailSender dynamicSender = getDynamicMailSender();
            MimeMessage message2 = dynamicSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message2, true, "UTF-8");
            helper.setFrom(adminEmail);
            helper.setTo(adminEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            dynamicSender.send(message2);
        } catch (Exception e) {
            System.err.println("Failed to send low stock alert mail: " + e.getMessage());
        }
    }

    @Async
    public void sendOtpHtmlAsync(String email, int otp) {
        String companyName = getCompanyName();
        String htmlBody = "<div style=\"font-family: Helvetica, Arial, sans-serif; min-width: 1000px; overflow: auto; line-height: 2\">" +
                "  <div style=\"margin: 50px auto; width: 70%; padding: 20px 0\">" +
                "    <div style=\"border-b: 1px solid #eee\">" +
                "      <a href=\"\" style=\"font-size: 1.4em; color: #1a237e; text-decoration: none; font-weight: 600\">" + companyName + "</a>" +
                "    </div>" +
                "    <p style=\"font-size: 1.1em\">Hi,</p>" +
                "    <p>Thank you for choosing " + companyName + ". Use the following OTP to complete your password reset procedure. OTP is valid for 5 minutes.</p>" +
                "    <h2 style=\"background: #1a237e; margin: 0 auto; width: max-content; padding: 0 10px; color: #fff; border-radius: 4px;\">" + otp + "</h2>" +
                "    <p style=\"font-size: 0.9em;\">Regards,<br />" + companyName + "</p>" +
                "    <hr style=\"border: none; border-top: 1px solid #eee\" />" +
                "  </div>" +
                "</div>";

        try {
            JavaMailSender dynamicSender = getDynamicMailSender();
            MimeMessage message = dynamicSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(getSenderEmail());
            helper.setTo(email);
            helper.setSubject("OTP for resetting password");
            helper.setText(htmlBody, true);
            dynamicSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Error sending OTP mail: " + e.getMessage());
        }
    }
}
