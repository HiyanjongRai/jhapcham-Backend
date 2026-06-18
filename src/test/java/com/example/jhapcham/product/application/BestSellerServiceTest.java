package com.example.jhapcham.product.application;

import com.example.jhapcham.product.dto.BestSellerDTO;
import com.example.jhapcham.product.persistence.BestSellerRepository;
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
class BestSellerServiceTest {

    @Mock
    private BestSellerRepository bestSellerRepository;

    @InjectMocks
    private BestSellerService bestSellerService;

    private List<Object[]> mockBestSellerData;

    @BeforeEach
    void setUp() {
        mockBestSellerData = new ArrayList<>();
        Object[] row = new Object[]{
                1L,                          // id
                1L,                          // seller_profile_id
                1L,                          // seller_user_id
                "Product Name",              // name
                "product-slug",              // slug
                "Electronics",               // category
                "Brand",                     // brand
                new BigDecimal("100.00"),    // price
                new BigDecimal("80.00"),     // sale_price
                true,                        // on_sale
                50,                          // stock_quantity
                "image1.jpg|image2.jpg",     // image_paths
                100L,                        // total_sales
                4.5,                         // average_rating
                50,                          // total_reviews
                "Seller Name",               // seller_full_name
                "Store Name",                // store_name
                "logo.jpg"                   // logo_image_path
        };
        mockBestSellerData.add(row);
    }

    @Test
    void testGetBestSellers_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Object[]> mockPage = new PageImpl<>(mockBestSellerData, pageable, 1);
        
        when(bestSellerRepository.findBestSellers(any(Pageable.class)))
                .thenReturn(mockPage);

        Page<BestSellerDTO> result = bestSellerService.getBestSellers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Product Name", result.getContent().get(0).getName());
        assertEquals(100L, result.getContent().get(0).getTotalSales());
    }

    @Test
    void testGetTopBestSellers_Success() {
        when(bestSellerRepository.findTopBestSellers(10))
                .thenReturn(mockBestSellerData);

        List<BestSellerDTO> result = bestSellerService.getTopBestSellers(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Product Name", result.get(0).getName());
    }

    @Test
    void testGetBestSellers_EmptyResult() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Object[]> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        
        when(bestSellerRepository.findBestSellers(any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<BestSellerDTO> result = bestSellerService.getBestSellers(pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testImagePathsParsing() {
        when(bestSellerRepository.findTopBestSellers(1))
                .thenReturn(mockBestSellerData);

        List<BestSellerDTO> result = bestSellerService.getTopBestSellers(1);

        assertNotNull(result);
        assertEquals(2, result.get(0).getImagePaths().size());
        assertEquals("image1.jpg", result.get(0).getImagePaths().get(0));
        assertEquals("image2.jpg", result.get(0).getImagePaths().get(1));
    }
}
