package org.example.e_fashion.controller.staff;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.CouponResponseDTO;
import org.example.e_fashion.service.CouponService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/coupons")
@RequiredArgsConstructor
public class CouponStaffController {

    private final CouponService couponService;

    @GetMapping
    public List<CouponResponseDTO> getAll() {
        return couponService.getAll();
    }
}
