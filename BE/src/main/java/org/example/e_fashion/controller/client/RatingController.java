package org.example.e_fashion.controller.client;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.RatingResponseDTO;
import org.example.e_fashion.dto.response.RatingSummaryResponseDTO;
import org.example.e_fashion.service.RatingService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;



    @GetMapping("/product/{productId}")
    public Page<RatingResponseDTO> getProductRatings(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ratingService.getProductRatings(productId, page, size);
    }

    @GetMapping("/order/{orderId}")
    public List<RatingResponseDTO> getRatingsByOrder(
            @PathVariable String orderId
    ) {
        return ratingService.getRatingsByOrder(orderId);
    }

    @GetMapping("/product/{productId}/summary")
    public RatingSummaryResponseDTO getProductRatingSummary(@PathVariable String productId) {
        return ratingService.getRatingSummary(productId);
    }
}