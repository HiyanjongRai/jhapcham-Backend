//package com.example.jhapcham.productLike;
//
//import com.example.jhapcham.product.model.Product;
//import com.example.jhapcham.user.model.User;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {
//    // Find likes by user
//    List<ProductLike> findByUser(User user);
//
//    // Find likes by product
//    List<ProductLike> findByProduct(Product product);
//
//    // Find like by user and product
//    Optional<ProductLike> findByUserAndProduct(User user, Product product);
//
//    // Count likes by product
//    int countByProduct(Product product);
//
//    @Query("""
//        select count(pl)
//        from ProductLike pl
//        where pl.product.sellerId = :sellerId
//    """)
//    long countBySellerId(@Param("sellerId") Long sellerId);
//
//}
