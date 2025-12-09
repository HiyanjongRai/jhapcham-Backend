package com.example.jhapcham.cart;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("select c from CartItem c join fetch c.product where c.user = :user")
    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProductAndSelectedColorAndSelectedStorage(
            User user,
            Product product,
            String selectedColor,
            String selectedStorage
    );

    void deleteByUserAndProductAndSelectedColorAndSelectedStorage(
            User user,
            Product product,
            String selectedColor,
            String selectedStorage
    );

    void deleteAllByUser(User user);

    List<CartItem> findTop200ByUser_IdOrderByCreatedAtDesc(Long userId);

    void deleteAllByUser_Id(Long userId);

}
