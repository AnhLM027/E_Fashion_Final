package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.RatingRequestDTO;
import org.example.e_fashion.dto.response.RatingResponseDTO;
import org.example.e_fashion.dto.response.RatingSummaryResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RatingService {

    RatingResponseDTO createRating(String userId, RatingRequestDTO dto);

    RatingResponseDTO updateRating(String userId, String ratingId, RatingRequestDTO dto);

    List<RatingResponseDTO> getProductRatings(String productId);

    List<RatingResponseDTO> getAllRatings();

    Page<RatingResponseDTO> getProductRatings(String productId, int page, int size);

    List<RatingResponseDTO> getRatingsByOrder(String orderId);

    RatingSummaryResponseDTO getRatingSummary(String productId);

    void deleteRating(String ratingId);
}