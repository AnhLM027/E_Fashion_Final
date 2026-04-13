package org.example.e_fashion.service;

import jakarta.servlet.http.HttpServletResponse;
import org.example.e_fashion.dto.response.DashboardResponseDTO;

import java.io.IOException;

public interface DashboardService {

    DashboardResponseDTO getDashboard(String from, String to);

    void exportRevenue(
            String from,
            String to,
            HttpServletResponse response
    ) throws IOException;

    void exportRecentOrders(
            String from,
            String to,
            HttpServletResponse response
    ) throws IOException;

    void exportTopProducts(
            String from,
            String to,
            HttpServletResponse response
    ) throws IOException;

    void exportLowStock(HttpServletResponse response) throws IOException;
}
