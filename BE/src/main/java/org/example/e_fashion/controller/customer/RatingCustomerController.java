package org.example.e_fashion.controller.customer;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.RatingRequestDTO;
import org.example.e_fashion.dto.response.RatingResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.service.RatingService;
import org.example.e_fashion.utils.ExtractUserUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/ratings")
@RequiredArgsConstructor
public class RatingCustomerController {

    private final RatingService ratingService;
    private final ExtractUserUtils extractUserUtils;

    @PostMapping
    public RatingResponseDTO createRating(
            HttpServletRequest request,
            @RequestBody RatingRequestDTO ratingRequestDTO
    ) {
        UserEntity user = extractUserUtils.extract(request);
        return ratingService.createRating(user.getId(), ratingRequestDTO);
    }

    @PutMapping("/{ratingId}")
    public RatingResponseDTO updateRating(
            HttpServletRequest request,
            @PathVariable String ratingId,
            @RequestBody RatingRequestDTO ratingRequestDTO
    ) {
        UserEntity user = extractUserUtils.extract(request);
        return ratingService.updateRating(user.getId(), ratingId, ratingRequestDTO);
    }
}
