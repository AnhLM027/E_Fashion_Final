package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.RatingRequestDTO;
import org.example.e_fashion.dto.response.RatingResponseDTO;
import org.example.e_fashion.dto.response.RatingSummaryResponseDTO;
import org.example.e_fashion.entity.*;
import org.example.e_fashion.mapper.RatingMapper;
import org.example.e_fashion.repository.*;
import org.example.e_fashion.service.RatingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final RatingMapper ratingMapper;

    @Override
    public RatingResponseDTO createRating(String userId, RatingRequestDTO dto) {
        System.out.println(dto);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        OrderItemEntity orderItem = orderItemRepository
                .findDeliveredOrderItemByUser(userId, dto.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("You must purchase this product before rating"));

        if (ratingRepository.existsByOrderItem_Id(dto.getOrderItemId())) {
            throw new RuntimeException("This product has already been reviewed");
        }

        ProductEntity product = orderItem
                .getProductVariantSize()
                .getProductVariant()
                .getProduct();

        RatingEntity rating = new RatingEntity();
        rating.setUser(user);
        rating.setProduct(product);
        rating.setOrderItem(orderItem);
        rating.setRating(dto.getRating());
        rating.setReviewText(dto.getReviewText());

        RatingEntity saved = ratingRepository.save(rating);

        return ratingMapper.toResponse(saved);
    }

    @Override
    public RatingResponseDTO updateRating(String userId, String ratingId, RatingRequestDTO dto) {

        RatingEntity rating = ratingRepository.findByIdAndUserId(ratingId, userId)
                .orElseThrow(() -> new RuntimeException("Rating not found or permission denied"));

        rating.setRating(dto.getRating());
        rating.setReviewText(dto.getReviewText());

        RatingEntity updated = ratingRepository.save(rating);

        return ratingMapper.toResponse(updated);
    }

    @Override
    public List<RatingResponseDTO> getProductRatings(String productId) {

        List<RatingEntity> ratings = ratingRepository.findByProduct_Id(productId);

        return ratings.stream()
                .map(ratingMapper::toResponse)
                .toList();
    }

    @Override
    public List<RatingResponseDTO> getAllRatings() {

        return ratingRepository.findAll()
                .stream()
                .map(ratingMapper::toResponse)
                .toList();
    }

    @Override
    public Page<RatingResponseDTO> getProductRatings(String productId, int page, int size) {

        return ratingRepository
                .findByProduct_IdOrderByCreatedAtDesc(
                        productId,
                        PageRequest.of(page, size)
                )
                .map(ratingMapper::toResponse);
    }

    @Override
    public List<RatingResponseDTO> getRatingsByOrder(String orderId) {

        return ratingRepository
                .findByOrderItem_Order_Id(orderId)
                .stream()
                .map(ratingMapper::toResponse)
                .toList();
    }

    @Override
    public RatingSummaryResponseDTO getRatingSummary(String productId) {

        Double avg = ratingRepository.getAverageRatingByProduct(productId);
        long total = ratingRepository.countByProduct_Id(productId);

        RatingSummaryResponseDTO dto = new RatingSummaryResponseDTO();

        dto.setAverageRating(avg == null ? 0 : Math.round(avg * 10.0) / 10.0);
        dto.setTotalRatings(total);

        return dto;
    }

    @Override
    public void deleteRating(String ratingId) {

        RatingEntity rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        ratingRepository.delete(rating);
    }
}