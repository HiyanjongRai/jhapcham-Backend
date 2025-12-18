package com.example.jhapcham.wishlist;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUser(User user);

    Optional<Wishlist> findByUserAndProduct(User user, Product product);

    void deleteByUserAndProduct(User user, Product product);

    boolean existsByUserAndProduct(User user, Product product);
}
