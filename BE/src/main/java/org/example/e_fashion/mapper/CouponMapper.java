package org.example.e_fashion.mapper;

import org.example.e_fashion.dto.request.CouponRequestDTO;
import org.example.e_fashion.dto.response.CouponResponseDTO;
import org.example.e_fashion.entity.CouponEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CouponMapper {

    public CouponResponseDTO toResponse(CouponEntity entity) {
        if (entity == null) return null;

        CouponResponseDTO dto = new CouponResponseDTO();

        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setDiscountValue(entity.getDiscountValue());
        dto.setDiscountType(entity.getDiscountType());
        dto.setMinOrderValue(entity.getMinOrderValue());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setUsageLimit(entity.getUsageLimit());
        dto.setIsActive(entity.getIsActive());

        return dto;
    }

    public void toEntity(CouponRequestDTO request, CouponEntity entity) {
        entity.setCode(request.getCode());
        entity.setDiscountValue(request.getDiscountValue());
        entity.setDiscountType(request.getDiscountType());
        entity.setMinOrderValue(request.getMinOrderValue());
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setUsageLimit(request.getUsageLimit());
        entity.setIsActive(request.getIsActive());
    }
}