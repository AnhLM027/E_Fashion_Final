package org.example.e_fashion.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.CouponRequestDTO;
import org.example.e_fashion.dto.response.CouponResponseDTO;
import org.example.e_fashion.service.CouponService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class CouponAdminController {

    private final CouponService couponService;

    @PostMapping
    public CouponResponseDTO create(
            @RequestBody CouponRequestDTO request) {
        return couponService.create(request);
    }

    @PutMapping("/{id}")
    public CouponResponseDTO update(
            @PathVariable String id,
            @RequestBody CouponRequestDTO request) {
        return couponService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        couponService.delete(id);
    }
}
