package org.example.e_fashion.controller.staff;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.RatingResponseDTO;
import org.example.e_fashion.service.RatingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/ratings")
@RequiredArgsConstructor
public class RatingStaffController {

    private final RatingService ratingService;

    @GetMapping
    public List<RatingResponseDTO> getAllRatings() {
        return ratingService.getAllRatings();
    }

    @DeleteMapping("/{ratingId}")
    public void deleteRating(@PathVariable String ratingId) {
        ratingService.deleteRating(ratingId);
    }
}
