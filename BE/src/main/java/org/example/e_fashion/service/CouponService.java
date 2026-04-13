package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.ApplyCouponRequestDTO;
import org.example.e_fashion.dto.request.CouponRequestDTO;
import org.example.e_fashion.dto.response.ApplyCouponResponseDTO;
import org.example.e_fashion.dto.response.CouponResponseDTO;

import java.util.List;

public interface CouponService {

    // ===== ADMIN =====
    CouponResponseDTO create(CouponRequestDTO request);
    CouponResponseDTO update(String id, CouponRequestDTO request);
    void delete(String id);
    List<CouponResponseDTO> getAll();

    // ===== CLIENT =====
    ApplyCouponResponseDTO applyCoupon(String userId,
                                       ApplyCouponRequestDTO request);

    void markCouponUsed(String userId, String couponCode);

    List<CouponResponseDTO> getUserCoupons(String userId);

    void claimCoupon(String userId, String couponCode);
}