package com.example.jhapcham.Checkout;

import com.example.jhapcham.cart.CartItem;
import com.example.jhapcham.cart.CartItemRepository;
import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.seller.model.SellerProfile;
import com.example.jhapcham.seller.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckoutService {

    private final CartItemRepository cartRepo;
    private final ProductRepository productRepo;
    private final CheckoutSessionRepository checkoutRepo;
    private final SellerProfileRepository sellerProfileRepository;

    public CheckoutSession startCheckout(
            Long userId,
            String fullAddress,
            Double lat,
            Double lng,
            boolean insideValley,
            PaymentMethod paymentMethod
    ) {

        List<CartItem> cart = cartRepo.findTop200ByUser_IdOrderByCreatedAtDesc(userId);

        if (cart.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        List<CheckoutItemSnapshot> snapshots = new ArrayList<>();
        double subtotal = 0.0;
        double deliveryTotal = 0.0;

        for (CartItem ci : cart) {

            Product p = ci.getProduct();

            if (p.getStock() < ci.getQuantity()) {
                throw new IllegalStateException("Out of stock for " + p.getName());
            }

            double unitPrice = p.isOnSale() ? p.getSalePrice() : p.getPrice();
            double itemSubtotal = unitPrice * ci.getQuantity();

            SellerProfile seller = sellerProfileRepository
                    .findByUserId(p.getSellerId())
                    .orElseThrow(() -> new IllegalStateException("Seller profile not found"));

            double deliveryFee = calculateDeliveryFee(seller, insideValley, subtotal);
            double lineTotal = itemSubtotal + deliveryFee;

            CheckoutItemSnapshot snap = CheckoutItemSnapshot.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .unitPrice(unitPrice)
                    .quantity(ci.getQuantity())
                    .deliveryFee(deliveryFee)
                    .lineTotal(lineTotal)
                    .selectedColor(ci.getSelectedColor())
                    .selectedStorage(ci.getSelectedStorage())
                    .build();

            snapshots.add(snap);

            subtotal += itemSubtotal;
            deliveryTotal += deliveryFee;
        }

        double tax = subtotal * 0.13;
        double grandTotal = subtotal + deliveryTotal + tax;

        boolean paid = paymentMethod == PaymentMethod.ONLINE;

        CheckoutSession session = CheckoutSession.builder()
                .userId(userId)
                .subtotal(subtotal)
                .deliveryTotal(deliveryTotal)
                .tax(tax)
                .grandTotal(grandTotal)
                .fullAddress(fullAddress)
                .latitude(lat)
                .longitude(lng)
                .paymentMethod(paymentMethod)
                .isPaid(paid)
                .status(paid ? CheckoutStatus.PAID : CheckoutStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .items(snapshots)
                .build();

        snapshots.forEach(s -> s.setCheckoutSession(session));

        return checkoutRepo.save(session);
    }

    private double calculateDeliveryFee(SellerProfile seller, boolean insideValley, double orderSubtotal) {

        if (Boolean.TRUE.equals(seller.getFreeShippingEnabled())
                && orderSubtotal >= seller.getFreeShippingMinOrder()) {
            return 0.0;
        }

        if (insideValley) {
            return seller.getInsideValleyDeliveryFee();
        }

        return seller.getOutsideValleyDeliveryFee();
    }

    public CheckoutSession markPaid(Long checkoutId) {

        CheckoutSession session = checkoutRepo.findById(checkoutId)
                .orElseThrow(() -> new IllegalStateException("Checkout not found"));

        session.setIsPaid(true);
        session.setStatus(CheckoutStatus.PAID);

        return checkoutRepo.save(session);
    }

    public CheckoutSession getConfirmed(Long checkoutId) {

        CheckoutSession session = checkoutRepo.findById(checkoutId)
                .orElseThrow(() -> new IllegalStateException("Checkout not found"));

        if (!session.getIsPaid()) {
            throw new IllegalStateException("Payment not completed");
        }

        return session;
    }

    public void clearCart(Long userId) {
        cartRepo.deleteAllByUser_Id(userId);
    }

    public CheckoutSession getById(Long checkoutId) {

        return checkoutRepo.findById(checkoutId)
                .orElseThrow(() -> new IllegalStateException("Checkout not found"));


    }

}