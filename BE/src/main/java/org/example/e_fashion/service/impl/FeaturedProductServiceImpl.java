package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.FeaturedProductResponseDTO;
import org.example.e_fashion.entity.enums.FeaturedType;
import org.example.e_fashion.repository.FeaturedProductRepository;
import org.example.e_fashion.service.FeaturedProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeaturedProductServiceImpl implements FeaturedProductService {

    private final FeaturedProductRepository repository;

    public List<FeaturedProductResponseDTO> getFeatured(String type) {

        FeaturedType featuredType = FeaturedType.valueOf(type.toUpperCase());

        return (switch (featuredType) {
                    case NEW -> repository.findNewProducts();
                    case SALE -> repository.findSaleProducts();
                    case BESTSELLERS -> repository.findBestSellerProducts();
                }).reversed().stream().map(p ->
                FeaturedProductResponseDTO.builder()
                        .productId(p.getProductId())
                        .name(p.getName())
                        .slug(p.getSlug())
                        .thumbnail(p.getThumbnail())
                        .minPrice(p.getMinPrice())
                        .maxPrice(p.getMaxPrice())
                        .isOnSale(p.getIsOnSale())
                        .totalSold(p.getTotalSold())
                        .build()
        ).toList();
    }
}