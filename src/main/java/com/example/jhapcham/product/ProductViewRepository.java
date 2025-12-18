package com.example.jhapcham.product;

import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

    List<ProductView> findByUser(User user);

    long countByUserAndProduct(User user, Product product);

    List<ProductView> findTop50ByUserOrderByViewedAtDesc(User user);

    // total views for one product
    long countByProduct(Product product);

    // total views for every product
    @Query("select pv.product.id as productId, count(pv) as viewCount " +
            "from ProductView pv group by pv.product.id")
    List<ProductViewCountProjection> countViewsByProduct();


    void deleteByProduct(Product product);


}