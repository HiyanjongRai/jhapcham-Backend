package com.example.jhapcham.notification;

import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final SmsLogRepository smsLogRepository;
    private final SmsPreferenceRepository smsPreferenceRepository;
    private final RestTemplate restTemplate;

    @Value("${sms.provider.enabled:false}")
    private Boolean smsEnabled;

    @Value("${sms.provider.api-key:}")
    private String smsApiKey;

    @Value("${sms.provider.url:}")
    private String smsProviderUrl;

    @Value("${sms.provider.sender-id:Jhapcham}")
    private String senderId;

    /**
     * Send SMS to user
     * Supports: Twilio, AWS SNS, or generic HTTP-based SMS gateway
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendSms(User user, String phoneNumber, SmsType smsType, String message, String referenceId) {
        try {
            // Respect per-user SMS preferences before doing anything
            SmsPreference pref = smsPreferenceRepository.findByUser(user).orElse(null);
            if (pref != null) {
                if (!Boolean.TRUE.equals(pref.getAllSmsEnabled())) {
                    log.info("All SMS disabled for user {}. Skipping {} SMS.", user.getId(), smsType);
                    return;
                }
                boolean allowed = switch (smsType) {
                    case ORDER_CONFIRMATION  -> Boolean.TRUE.equals(pref.getOrderConfirmation());
                    case SHIPMENT_NOTIFICATION -> Boolean.TRUE.equals(pref.getShipmentUpdates());
                    case DELIVERY_OTP        -> Boolean.TRUE.equals(pref.getDeliveryNotifications());
                    case REFUND_ALERT        -> Boolean.TRUE.equals(pref.getRefundAlerts());
                    case DISPUTE_ALERT       -> Boolean.TRUE.equals(pref.getDisputeAlerts());
                    case INVENTORY_ALERT     -> Boolean.TRUE.equals(pref.getInventoryAlerts());
                };
                if (!allowed) {
                    log.info("SMS type {} disabled for user {}. Skipping.", smsType, user.getId());
                    return;
                }
            }

            SmsLog smsLog = SmsLog.builder()
                    .user(user)
                    .phoneNumber(phoneNumber)
                    .smsType(smsType)
                    .message(message)
                    .referenceId(referenceId)
                    .sent(false)
                    .retryCount(0)
                    .build();

            if (!smsEnabled) {
                log.warn("SMS disabled. Message logged but not sent: {} to {}", smsType, phoneNumber);
                smsLogRepository.save(smsLog);
                return;
            }

            // Send SMS via provider
            boolean success = sendViaSmsProvider(phoneNumber, message);

            if (success) {
                smsLog.setSent(true);
                smsLog.setSentAt(LocalDateTime.now());
                log.info("SMS sent successfully: {} to {}", smsType, phoneNumber);
            } else {
                smsLog.setFailureReason("SMS provider returned failure");
                log.error("Failed to send SMS: {} to {}", smsType, phoneNumber);
            }

            smsLogRepository.save(smsLog);
        } catch (Exception e) {
            log.error("Error sending SMS: {}", e.getMessage(), e);
        }
    }

    /**
     * Send order confirmation SMS with OTP
     */
    public void sendOrderConfirmationSms(User user, String phoneNumber, String orderId, String otp) {
        String name = (user.getFullName() != null && !user.getFullName().trim().isEmpty()) ? user.getFullName().split(" ")[0] : "Customer";
        String message = String.format(
            "Hi %s! Your Jhapcham order #%s is confirmed. OTP: %s. Track at jhapcham.com",
            name,
            orderId,
            otp
        );
        sendSms(user, phoneNumber, SmsType.ORDER_CONFIRMATION, message, orderId);
    }

    /**
     * Send shipment update SMS
     */
    @Transactional
    public void sendShipmentSms(User user, String phoneNumber, String trackingNumber) {
        String message = String.format(
            "Your Jhapcham order has been shipped! Tracking: %s. Track live at jhapcham.com",
            trackingNumber
        );
        sendSms(user, phoneNumber, SmsType.SHIPMENT_NOTIFICATION, message, trackingNumber);
    }

    /**
     * Send delivery OTP SMS
     */
    @Transactional
    public void sendDeliveryOtpSms(User user, String phoneNumber, String otp) {
        String message = String.format(
            "Delivery OTP for your Jhapcham order: %s. Valid for 10 minutes.",
            otp
        );
        sendSms(user, phoneNumber, SmsType.DELIVERY_OTP, message, otp);
    }

    /**
     * Send refund notification SMS
     */
    @Transactional
    public void sendRefundSms(User user, String phoneNumber, String amount, String orderId) {
        String message = String.format(
            "Your refund of Rs. %s for order #%s has been processed. Check your bank account.",
            amount,
            orderId
        );
        sendSms(user, phoneNumber, SmsType.REFUND_ALERT, message, orderId);
    }

    /**
     * Send dispute alert SMS
     */
    @Transactional
    public void sendDisputeSms(User user, String phoneNumber, String disputeId) {
        String message = String.format(
            "A dispute has been opened on your Jhapcham order. Dispute ID: %s. Respond within 48 hours.",
            disputeId
        );
        sendSms(user, phoneNumber, SmsType.DISPUTE_ALERT, message, disputeId);
    }

    /**
     * Send inventory alert SMS to seller
     */
    @Transactional
    public void sendInventoryAlertSms(User seller, String phoneNumber, String productName) {
        String message = String.format(
            "Alert! Your product '%s' stock is running low. Reorder now at seller.jhapcham.com",
            productName
        );
        sendSms(seller, phoneNumber, SmsType.INVENTORY_ALERT, message, productName);
    }

    /**
     * Send SMS via SMS Provider (Twilio, AWS SNS, or HTTP Gateway)
     */
    private boolean sendViaSmsProvider(String phoneNumber, String message) {
        try {
            if (!isValidPhoneNumber(phoneNumber)) {
                log.warn("Invalid phone number: {}", phoneNumber);
                return false;
            }

            // Generic HTTP SMS Gateway (modify based on your provider)
            if (smsProviderUrl != null && !smsProviderUrl.isEmpty()) {
                return sendViaHttpGateway(phoneNumber, message);
            }

            // If no provider configured, log and return success (for development)
            log.info("SMS Provider not configured. Message: {} to {}", message, phoneNumber);
            return true;
        } catch (Exception e) {
            log.error("Error in SMS provider: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send SMS via HTTP-based SMS Gateway
     */
    private boolean sendViaHttpGateway(String phoneNumber, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + smsApiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("phoneNumber", phoneNumber);
            body.put("message", message);
            body.put("senderId", senderId);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(smsProviderUrl, request, Map.class);
            
            if (response != null && response.containsKey("status")) {
                String status = response.get("status").toString();
                return "success".equalsIgnoreCase(status) || "sent".equalsIgnoreCase(status);
            }
            return false;
        } catch (Exception e) {
            log.error("Error sending SMS via HTTP gateway: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate phone number format
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        // Basic validation: should contain at least 10 digits
        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        return digitsOnly.length() >= 10;
    }

    /**
     * Retry failed SMS messages
     */
    @Transactional
    public void retryFailedSms() {
        try {
            var failedMessages = smsLogRepository.findBySentFalse();
            for (SmsLog smsLog : failedMessages) {
                if (smsLog.getRetryCount() == null) smsLog.setRetryCount(0);
                
                if (smsLog.getRetryCount() < 3) {  // Max 3 retries
                    smsLog.setRetryCount(smsLog.getRetryCount() + 1);
                    boolean success = sendViaSmsProvider(smsLog.getPhoneNumber(), smsLog.getMessage());
                    
                    if (success) {
                        smsLog.setSent(true);
                        smsLog.setSentAt(LocalDateTime.now());
                        smsLog.setFailureReason(null);
                        log.info("SMS retry successful for message ID: {}", smsLog.getId());
                    }
                    
                    smsLogRepository.save(smsLog);
                }
            }
        } catch (Exception e) {
            log.error("Error retrying failed SMS: {}", e.getMessage());
        }
    }

    /**
     * Get SMS history for user
     */
    public java.util.List<SmsLog> getUserSmsHistory(User user) {
        return smsLogRepository.findByUser(user);
    }
}
