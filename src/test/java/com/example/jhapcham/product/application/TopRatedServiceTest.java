package com.example.jhapcham.product.application;

import com.example.jhapcham.product.dto.TopRatedDTO;
import com.example.jhapcham.product.persistence.TopRatedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopRatedServiceTest {

    @Mock
    private TopRatedRepository topRatedRepository;

    @InjectMocks
    private TopRatedService topRatedService;

    private List<Object[]> mockTopRatedData;

    @BeforeEach
    void setUp() {
        mockTopRatedData = new ArrayList<>();
        Object[] row = new Object[]{
                1L,                          // id
                1L,                          // seller_profile_id
                1L,                          // seller_user_id
                "Premium Product",           // name
                "premium-product",           // slug
                "Electronics",               // category
                "Brand",                     // brand
                new BigDecimal("200.00"),    // price
                new BigDecimal("150.00"),    // sale_price
                true,                        // on_sale
                30,                          // stock_quantity
                "image1.jpg",                // image_paths
                4.8,                         // average_rating
                200,                         // total_reviews
                5000L,                       // total_views
                "Seller Name",               // seller_full_name
                "Store Name",                // store_name
                "logo.jpg"                   // logo_image_path
        };
        mockTopRatedData.add(row);
    }

    @Test
    void testGetTopRated_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Object[]> mockPage = new PageImpl<>(mockTopRatedData, pageable, 1);
        
        when(topRatedRepository.findTopRated(any(Pageable.class)))
                .thenReturn(mockPage);

        Page<TopRatedDTO> result = topRatedService.getTopRated(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Premium Product", result.getContent().get(0).getName());
        assertEquals(4.8, result.getContent().get(0).getAverageRating());
        assertEquals(200, result.getContent().get(0).getTotalReviews());
    }

    @Test
    void testGetTopRatedProducts_Success() {
        when(topRatedRepository.findTopRatedProducts(10))
                .thenReturn(mockTopRatedData);

        List<TopRatedDTO> result = topRatedService.getTopRatedProducts(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Premium Product", result.get(0).getName());
        assertEquals(5000L, result.get(0).getTotalViews());
    }

    @Test
    void testGetTopRated_RatingValidation() {
        when(topRatedRepository.findTopRatedProducts(5))
                .thenReturn(mockTopRatedData);

        List<TopRatedDTO> result = topRatedService.getTopRatedProducts(5);

        assertNotNull(result);
        assertTrue(result.get(0).getAverageRating() > 0);
        assertTrue(result.get(0).getTotalReviews() > 0);
    }

    @Test
    void testGetTopRated_EmptyResult() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Object[]> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        
        when(topRatedRepository.findTopRated(any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<TopRatedDTO> result = topRatedService.getTopRated(pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }
}
