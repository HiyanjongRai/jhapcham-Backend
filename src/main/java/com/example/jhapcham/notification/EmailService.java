package com.example.jhapcham.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @org.springframework.scheduling.annotation.Async
    public void sendDeliveryOtpEmail(String toEmail, String customerName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("🚚 Order Out for Delivery - OTP: " + otp);
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #000; margin-bottom: 16px;'>Hello %s!</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Great news! Your order is <b>Out for Delivery</b>.</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Please provide the following OTP to our delivery agent when they arrive to confirm your receipt:</p>" +
                "<div style='background: #f4f4f5; border-radius: 8px; padding: 16px; text-align: center; margin: 24px 0;'>" +
                "<span style='font-size: 32px; font-weight: 800; letter-spacing: 4px; color: #000;'>%s</span>" +
                "</div>" +
                "<p style='font-size: 14px; color: #71717a;'>This OTP is valid for 24 hours. For your security, please do not share this code with anyone else except our authorized delivery agent.</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Thank you for shopping with <b>Jhapcham</b>!</p>" +
                "</div>",
                customerName != null ? customerName : "Customer",
                otp
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Delivery OTP email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send Delivery OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendPasswordResetEmail(String toEmail, String username, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("🔒 Password Reset Verification - Jhapcham");
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #000; margin-bottom: 16px;'>Password Reset Request</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>We received a request to reset your Jhapcham account password. Use the verification code below to proceed:</p>" +
                "<div style='background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 8px; padding: 16px; text-align: center; margin: 24px 0;'>" +
                "<span style='font-size: 32px; font-weight: 800; letter-spacing: 4px; color: #0ea5e9;'>%s</span>" +
                "</div>" +
                "<p style='font-size: 14px; color: #71717a;'>This code will expire in <b>15 minutes</b>. If you did not request this, you can safely ignore this email.</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Jhapcham E-Commerce Support</p>" +
                "</div>",
                username != null ? username : "Valued Member",
                otp
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Password reset OTP email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send Password reset OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendRefundRequestEmail(String toEmail, String customerName, Long orderId, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("💰 Refund Request Submitted - Order #" + orderId);
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #000; margin-bottom: 16px;'>Hello %s!</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>We've received your refund request for <b>Order #%d</b>.</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'><b>Reason:</b> %s</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Our team will review your request and get back to you within 24-48 hours.</p>" +
                "<div style='background: #f0f9ff; border-left: 4px solid #0ea5e9; padding: 16px; margin: 20px 0;'>" +
                "<p style='margin: 0; font-size: 14px; color: #0ea5e9;'><b>📋 What happens next?</b></p>" +
                "<p style='margin: 8px 0 0 0; font-size: 14px; color: #09090b;'>You can track your refund status in your account dashboard under 'Refunds & Returns'.</p>" +
                "</div>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Thank you for shopping with <b>Jhapcham</b>!</p>" +
                "</div>",
                customerName != null ? customerName : "Customer",
                orderId,
                reason
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Refund request email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send refund request email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendRefundApprovedEmail(String toEmail, String customerName, java.math.BigDecimal amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("✅ Refund Approved - Rs. " + amount);
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #22c55e; margin-bottom: 16px;'>Great News! Your Refund is Approved ✅</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Your refund request has been <b>approved</b>!</p>" +
                "<div style='background: #f0fdf4; border: 2px solid #22c55e; border-radius: 8px; padding: 16px; text-align: center; margin: 20px 0;'>" +
                "<p style='margin: 0; font-size: 14px; color: #16a34a;'>Refund Amount</p>" +
                "<p style='margin: 8px 0 0 0; font-size: 28px; font-weight: 800; color: #22c55e;'>Rs. %s</p>" +
                "</div>" +
                "<p style='font-size: 16px; line-height: 1.5;'>The refund will be processed to your original payment method within 5-7 business days.</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Thank you for your patience!</p>" +
                "</div>",
                customerName != null ? customerName : "Customer",
                amount
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Refund approved email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send refund approved email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendRefundRejectedEmail(String toEmail, String customerName, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("❌ Refund Request - Not Approved");
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #ef4444; margin-bottom: 16px;'>Refund Request Decision</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Unfortunately, your refund request could not be approved.</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'><b>Reason:</b> %s</p>" +
                "<p style='font-size: 14px; color: #71717a;'>If you have any questions or would like to appeal this decision, please contact our support team.</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Jhapcham Customer Support</p>" +
                "</div>",
                customerName != null ? customerName : "Customer",
                reason
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Refund rejected email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send refund rejected email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendRefundCompletedEmail(String toEmail, String customerName, java.math.BigDecimal amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("💳 Refund Completed - Rs. " + amount);
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #22c55e; margin-bottom: 16px;'>Your Refund Has Been Processed! 💳</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Your refund has been successfully processed.</p>" +
                "<div style='background: #f0fdf4; border: 2px solid #22c55e; border-radius: 8px; padding: 16px; text-align: center; margin: 20px 0;'>" +
                "<p style='margin: 0; font-size: 14px; color: #16a34a;'>Amount Credited</p>" +
                "<p style='margin: 8px 0 0 0; font-size: 28px; font-weight: 800; color: #22c55e;'>Rs. %s</p>" +
                "</div>" +
                "<p style='font-size: 16px; line-height: 1.5;'>The amount has been credited to your original payment method. It may take 1-2 business days to appear in your account.</p>" +
                "<p style='font-size: 14px; color: #71717a;'>We hope to serve you better next time. Your feedback helps us improve!</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Thank you for choosing Jhapcham!</p>" +
                "</div>",
                customerName != null ? customerName : "Customer",
                amount
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Refund completed email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send refund completed email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendLoyaltyRedemptionEmail(String toEmail, String customerName, Long pointsRedeemed, Long discountAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("🎁 Loyalty Points Redeemed - Rs. " + discountAmount);
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #000; margin-bottom: 16px;'>Loyalty Points Redeemed! 🎁</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>You've successfully redeemed your loyalty points!</p>" +
                "<div style='background: #fef3c7; border: 2px solid #f59e0b; border-radius: 8px; padding: 16px; text-align: center; margin: 20px 0;'>" +
                "<p style='margin: 0; font-size: 14px; color: #d97706;'>Points Redeemed</p>" +
                "<p style='margin: 8px 0 0 0; font-size: 24px; font-weight: 800; color: #f59e0b;'>%d Points</p>" +
                "<p style='margin: 16px 0 0 0; font-size: 14px; color: #d97706;'>Discount Worth: <b>Rs. %d</b></p>" +
                "</div>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Your discount has been applied to your account. Use it on your next purchase!</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Keep earning more points with every purchase! 🌟</p>" +
                "</div>",
                customerName != null ? customerName : "Customer",
                pointsRedeemed,
                discountAmount
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Loyalty redemption email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send loyalty redemption email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendTierUpgradeEmail(String toEmail, String customerName, String tier, String benefits) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("⭐ Congratulations! You've Reached " + tier + " Tier!");
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #f59e0b; margin-bottom: 16px;'>🎉 Tier Upgraded! You're Now %s</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Congratulations! You've unlocked the <b>%s</b> membership tier!</p>" +
                "<div style='background: #fef3c7; border: 2px solid #f59e0b; border-radius: 8px; padding: 20px; margin: 20px 0;'>" +
                "<p style='margin: 0 0 16px 0; font-size: 16px; font-weight: 800; color: #f59e0b;'>Your New Tier Benefits:</p>" +
                "<p style='margin: 0; font-size: 15px; line-height: 1.6; color: #09090b;'>✨ %s</p>" +
                "</div>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Start enjoying these exclusive benefits on your next purchase!</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Thank you for being a valued member! 💝</p>" +
                "</div>",
                tier,
                customerName != null ? customerName : "Customer",
                tier,
                benefits
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Tier upgrade email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send tier upgrade email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendDisputeInitiatedEmail(String toEmail, String customerName, Long orderId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("⚖️ Dispute Initiated - Order #" + orderId);
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #ef4444; margin-bottom: 16px;'>⚖️ Dispute Initiated</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>A dispute has been initiated for <b>Order #%d</b>.</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Our admin team will review the case and contact you with further instructions.</p>" +
                "<p style='font-size: 14px; color: #71717a;'>Please keep all evidence and communication records ready for review.</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Jhapcham Dispute Resolution Team</p>" +
                "</div>",
                customerName != null ? customerName : "Customer",
                orderId
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Dispute initiated email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send dispute initiated email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendDisputeResolvedEmail(String toEmail, String customerName, String resolution) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("✅ Dispute Resolved");
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #22c55e; margin-bottom: 16px;'>✅ Dispute Resolved</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>The dispute has been reviewed and resolved.</p>" +
                "<div style='background: #f0fdf4; border-left: 4px solid #22c55e; padding: 16px; margin: 20px 0;'>" +
                "<p style='margin: 0; font-size: 14px; color: #16a34a;'><b>Resolution:</b></p>" +
                "<p style='margin: 8px 0 0 0; font-size: 15px; color: #09090b;'>%s</p>" +
                "</div>" +
                "<p style='font-size: 14px; color: #71717a;'>If you have any questions, please contact our support team.</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Jhapcham Dispute Resolution Team</p>" +
                "</div>",
                customerName != null ? customerName : "Customer",
                resolution
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Dispute resolved email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send dispute resolved email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendOrderConfirmationToCustomer(String toEmail, String customerName, Long orderId, java.math.BigDecimal total) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("🛍️ Order Confirmed! - Jhapcham #" + orderId);
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #08c; margin-bottom: 16px;'>Thank you for your order! 🛍️</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>We've received your order <b>#%d</b> and it's being prepared for shipment.</p>" +
                "<div style='background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin: 24px 0;'>" +
                "<p style='margin: 0; font-size: 14px; color: #64748b;'>Order Total</p>" +
                "<p style='margin: 8px 0 0 0; font-size: 24px; font-weight: 800; color: #000;'>Rs. %s</p>" +
                "</div>" +
                "<p style='font-size: 15px; color: #09090b;'>You will receive another email with your delivery details once your order is on its way.</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Thank you for shopping with <b>Jhapcham</b>!</p>" +
                "</div>",
                customerName != null ? customerName : "Customer",
                orderId,
                total
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Order confirmation email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendNewOrderAlertToSeller(String toEmail, String sellerName, Long orderId, java.math.BigDecimal amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("🚀 New Order Received! #" + orderId);
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #22c55e; margin-bottom: 16px;'>You have a new order! 🚀</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Congratulations! A new order <b>#%d</b> has been placed in your store.</p>" +
                "<div style='background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 20px; margin: 24px 0;'>" +
                "<p style='margin: 0; font-size: 14px; color: #166534;'>Order Value</p>" +
                "<p style='margin: 8px 0 0 0; font-size: 24px; font-weight: 800; color: #166534;'>Rs. %s</p>" +
                "</div>" +
                "<p style='font-size: 15px; color: #09090b;'>Please log in to your seller dashboard to process this order and prepare it for dispatch.</p>" +
                "<a href='https://jhapcham.com/seller/orders' style='display: inline-block; background: #000; color: white; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: bold; margin-top: 16px;'>Process Order</a>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Keep up the great work! - <b>Jhapcham Merchant Support</b></p>" +
                "</div>",
                sellerName != null ? sellerName : "Seller",
                orderId,
                amount
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Seller new order alert sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send seller new order alert to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendOrderStatusUpdateEmail(String toEmail, String customerName, Long orderId, String status) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("📦 Order Status Updated - Jhapcham #" + orderId);
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #08c; margin-bottom: 16px;'>Your order status has changed! 📦</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>The status of your order <b>#%d</b> has been updated to:</p>" +
                "<div style='background: #f0faff; border: 1px solid #bae6fd; border-radius: 8px; padding: 20px; margin: 24px 0; text-align: center;'>" +
                "<span style='font-size: 20px; font-weight: 800; text-transform: uppercase; color: #0369a1;'>%s</span>" +
                "</div>" +
                "<p style='font-size: 15px; color: #09090b;'>You can track your order in real-time from your account dashboard.</p>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Thank you for your trust in <b>Jhapcham</b>!</p>" +
                "</div>",
                customerName != null ? customerName : "Customer",
                orderId,
                status.replace("_", " ")
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Order status update email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send order status update email to {}: {}", toEmail, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void sendInventoryAlertEmail(String toEmail, String sellerName, String productName, 
                                        String alertType, Integer currentStock) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("⚠️ Inventory Alert - " + productName);
            
            String htmlContent = String.format(
                "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e4e4e7; border-radius: 12px; padding: 24px; color: #09090b;'>" +
                "<h2 style='color: #f59e0b; margin-bottom: 16px;'>⚠️ Inventory Alert</h2>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Hello %s,</p>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Your product <b>%s</b> has triggered an inventory alert.</p>" +
                "<div style='background: #fef3c7; border: 2px solid #f59e0b; border-radius: 8px; padding: 16px; text-align: center; margin: 20px 0;'>" +
                "<p style='margin: 0; font-size: 14px; color: #d97706;'>Alert Type</p>" +
                "<p style='margin: 8px 0 0 0; font-size: 18px; font-weight: 800; color: #f59e0b;'>%s</p>" +
                "<p style='margin: 12px 0 0 0; font-size: 14px; color: #d97706;'>Current Stock: <b>%d units</b></p>" +
                "</div>" +
                "<p style='font-size: 16px; line-height: 1.5;'>Please take necessary action to manage your inventory.</p>" +
                "<a href='https://jhapcham.com/dashboard/inventory' style='display: inline-block; background: #f59e0b; color: white; padding: 10px 20px; border-radius: 6px; text-decoration: none; font-weight: bold; margin: 16px 0;'>View Inventory</a>" +
                "<hr style='border: 0; border-top: 1px solid #e4e4e7; margin: 24px 0;' />" +
                "<p style='font-size: 14px; color: #71717a; text-align: center;'>Jhapcham Seller Support</p>" +
                "</div>",
                sellerName != null ? sellerName : "Seller",
                productName,
                alertType,
                currentStock
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Inventory alert email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send inventory alert email to {}: {}", toEmail, e.getMessage());
        }
    }
}
