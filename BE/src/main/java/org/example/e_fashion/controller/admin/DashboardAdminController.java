package org.example.e_fashion.controller.admin;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.DashboardResponseDTO;
import org.example.e_fashion.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardAdminController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponseDTO> getDashboard(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        DashboardResponseDTO response = dashboardService.getDashboard(from, to);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export/revenue")
    public void exportRevenue(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletResponse response
    ) throws IOException {
        dashboardService.exportRevenue(from, to, response);
    }

    @GetMapping("/export/recent-orders")
    public void exportRecentOrders(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletResponse response
    ) throws IOException {
        dashboardService.exportRecentOrders(from, to, response);
    }

    @GetMapping("/export/top-products")
    public void exportTopProducts(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletResponse response
    ) throws IOException {
        dashboardService.exportTopProducts(from, to, response);
    }

    @GetMapping("/export/low-stock")
    public void exportLowStock(
            HttpServletResponse response
    ) throws IOException {
        dashboardService.exportLowStock(response);
    }
}