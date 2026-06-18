package com.example.jhapcham.wishlist.persistence;


import com.example.jhapcham.wishlist.application.*;
import com.example.jhapcham.wishlist.domain.*;
import com.example.jhapcham.wishlist.persistence.*;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUser(User user);

    Optional<Wishlist> findByUserAndProduct(User user, Product product);

    void deleteByUserAndProduct(User user, Product product);

    boolean existsByUserAndProduct(User user, Product product);
}
