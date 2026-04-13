package org.example.e_fashion.controller.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ApplyCouponRequestDTO;
import org.example.e_fashion.dto.response.ApplyCouponResponseDTO;
import org.example.e_fashion.dto.response.CouponResponseDTO;
import org.example.e_fashion.service.CouponService;
import org.example.e_fashion.utils.ExtractUserUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final ExtractUserUtils extractUserUtils;

    @PostMapping("/apply")
    public ApplyCouponResponseDTO apply(
            HttpServletRequest request,
            @RequestBody ApplyCouponRequestDTO dto) {

        String userId = extractUserUtils.extract(request).getId();
        return couponService.applyCoupon(userId, dto);
    }

    @PostMapping("/claim/{couponCode}")
    public void claimCoupon(
            HttpServletRequest request,
            @PathVariable String couponCode) {

        String userId = extractUserUtils.extract(request).getId();
        couponService.claimCoupon(userId, couponCode);
    }

    @GetMapping("/my")
    public List<CouponResponseDTO> myCoupons(
            HttpServletRequest request) {

        String userId = extractUserUtils.extract(request).getId();
        return couponService.getUserCoupons(userId);
    }

    @GetMapping
    public List<CouponResponseDTO> getAll() {
        return couponService.getAll();
    }
}