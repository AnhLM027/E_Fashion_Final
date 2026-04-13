package org.example.e_fashion.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ApplyCouponRequestDTO;
import org.example.e_fashion.dto.request.CouponRequestDTO;
import org.example.e_fashion.dto.response.ApplyCouponResponseDTO;
import org.example.e_fashion.dto.response.CouponResponseDTO;
import org.example.e_fashion.entity.CouponEntity;
import org.example.e_fashion.entity.UserCouponEntity;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.entity.enums.DiscountType;
import org.example.e_fashion.mapper.CouponMapper;
import org.example.e_fashion.repository.CouponRepository;
import org.example.e_fashion.repository.UserCouponRepository;
import org.example.e_fashion.repository.UserRepository;
import org.example.e_fashion.service.CouponService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponMapper couponMapper;

    // ================= ADMIN =================

    @Override
    public CouponResponseDTO create(CouponRequestDTO request) {

        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Coupon code already exists");
        }

        CouponEntity entity = new CouponEntity();
        couponMapper.toEntity(request, entity);

        couponRepository.save(entity);
        return couponMapper.toResponse(entity);
    }

    @Override
    public CouponResponseDTO update(String id, CouponRequestDTO request) {

        CouponEntity entity = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        couponMapper.toEntity(request, entity);

        return couponMapper.toResponse(entity);
    }

    @Override
    public void delete(String id) {
        couponRepository.deleteById(id);
    }

    @Override
    public List<CouponResponseDTO> getAll() {
        return couponRepository.findAll()
                .stream()
                .map(couponMapper::toResponse)
                .toList();
    }

    // ================= CLIENT =================

    @Override
    public ApplyCouponResponseDTO applyCoupon(
            String userId,
            ApplyCouponRequestDTO request
    ) {

        ApplyCouponResponseDTO response = new ApplyCouponResponseDTO();

        CouponEntity coupon = couponRepository
                .findByCode(request.getCouponCode())
                .orElse(null);

        if (coupon == null) {
            return fail(response, "Coupon not found");
        }

        UserCouponEntity userCoupon = userCouponRepository
                .findByUser_IdAndCoupon_Code(userId, request.getCouponCode())
                .orElse(null);

        if (userCoupon == null) {
            return fail(response, "Coupon not assigned");
        }

        if (!coupon.getIsActive()) {
            return fail(response, "Coupon inactive");
        }

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStartDate() != null &&
                now.isBefore(coupon.getStartDate())) {
            return fail(response, "Coupon not started yet");
        }

        if (coupon.getEndDate() != null &&
                now.isAfter(coupon.getEndDate())) {
            return fail(response, "Coupon expired");
        }

        if (coupon.getUsageLimit() != null &&
                coupon.getUsageLimit() == 0) {
            return fail(response, "Coupon usage limit reached");
        }

        if (userCoupon.getIsUsed()) {
            return fail(response, "Coupon already used");
        }

        if (request.getOrderTotal()
                .compareTo(coupon.getMinOrderValue()) < 0) {
            return fail(response, "Order value too low");
        }

        BigDecimal discount = calculateDiscount(
                request.getOrderTotal(), coupon);

        BigDecimal finalTotal =
                request.getOrderTotal().subtract(discount);

        response.setApplicable(true);
        response.setMessage("Applied successfully");
        response.setDiscountAmount(discount);
        response.setFinalTotal(finalTotal);

        return response;
    }

    @Override
    public void markCouponUsed(String userId, String couponCode) {

        UserCouponEntity userCoupon = userCouponRepository
                .findByUser_IdAndCoupon_Code(userId, couponCode)
                .orElseThrow();

        userCoupon.setIsUsed(true);
    }

    @Override
    public List<CouponResponseDTO> getUserCoupons(String userId) {

        return userCouponRepository.findByUser_Id(userId)
                .stream()
                .map(uc -> {
                    CouponResponseDTO dto =
                            couponMapper.toResponse(uc.getCoupon());
                    dto.setIsUsed(uc.getIsUsed());
                    return dto;
                })
                .toList();
    }

    private BigDecimal calculateDiscount(
            BigDecimal orderTotal,
            CouponEntity coupon
    ) {
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            return orderTotal
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));
        }
        return coupon.getDiscountValue();
    }

    private ApplyCouponResponseDTO fail(
            ApplyCouponResponseDTO response,
            String message
    ) {
        response.setApplicable(false);
        response.setMessage(message);
        response.setDiscountAmount(BigDecimal.ZERO);
        response.setFinalTotal(null);
        return response;
    }

    @Override
    public void claimCoupon(String userId, String couponCode) {

        CouponEntity coupon = couponRepository
                .findByCode(couponCode)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        if (!coupon.getIsActive()) {
            throw new RuntimeException("Coupon inactive");
        }

        // check đã claim chưa
        boolean exists = userCouponRepository
                .findByUser_IdAndCoupon_Code(userId, couponCode)
                .isPresent();

        if (exists) {
            throw new RuntimeException("Coupon already claimed");
        }

        // tạo record user_coupon
        UserCouponEntity userCoupon = new UserCouponEntity();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow();

        userCoupon.setUser(user);
        userCoupon.setCoupon(coupon);
        userCoupon.setIsUsed(false);

        userCouponRepository.save(userCoupon);
    }
}