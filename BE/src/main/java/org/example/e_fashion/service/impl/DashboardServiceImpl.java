package org.example.e_fashion.service.impl;

import jakarta.servlet.http.HttpServletResponse;
import org.example.e_fashion.dto.dashboard.LowStockDTO;
import org.example.e_fashion.dto.dashboard.RecentOrderDTO;
import org.example.e_fashion.dto.dashboard.RevenueByDayDTO;
import org.example.e_fashion.dto.dashboard.TopProductDTO;
import org.example.e_fashion.entity.enums.OrderStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.response.DashboardResponseDTO;
import org.example.e_fashion.repository.OrderRepository;
import org.example.e_fashion.repository.ProductVariantSizeRepository;
import org.example.e_fashion.repository.UserRepository;
import org.example.e_fashion.service.DashboardService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductVariantSizeRepository variantSizeRepository;

    @Override
    public DashboardResponseDTO getDashboard(String from, String to) {

        LocalDateTime fromDate;
        LocalDateTime toDate;

        if (from != null && to != null) {
            fromDate = LocalDate.parse(from).atStartOfDay();
            toDate = LocalDate.parse(to).atTime(23, 59, 59);

            if (fromDate.isAfter(toDate)) {
                throw new IllegalArgumentException("Invalid date range");
            }
        } else {
            // default = last 7 days
            fromDate = LocalDate.now().minusDays(6).atStartOfDay();
            toDate = LocalDate.now().atTime(23, 59, 59);
        }

        DashboardResponseDTO dto = new DashboardResponseDTO();

        // 1️⃣ Revenue
        BigDecimal totalRevenue =
                orderRepository.sumRevenueBetween(fromDate, toDate);

        dto.setTotalRevenue(
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO
        );

        // 2️⃣ Orders
        dto.setTotalOrders(
                orderRepository.countBetween(fromDate, toDate)
        );

        dto.setPendingOrders(
                orderRepository.countPendingBetween(OrderStatus.PENDING, fromDate, toDate)
        );

        // 3️⃣ Users (không cần range)
        dto.setTotalUsers(userRepository.count());

        // 4️⃣ Revenue By Day
        List<Object[]> rawRevenue =
                orderRepository.getRevenueBetween(fromDate, toDate);

        List<RevenueByDayDTO> revenueList = rawRevenue.stream()
                .map(row -> {
                    java.sql.Date sqlDate = (java.sql.Date) row[0];
                    LocalDate localDate = sqlDate.toLocalDate();

                    BigDecimal revenue = (BigDecimal) row[1];

                    return new RevenueByDayDTO(localDate, revenue);
                        })
                .toList();

        dto.setRevenueByDay(revenueList);

        // 5️⃣ Recent Orders
        dto.setRecentOrders(
                orderRepository.getRecentOrdersBetween(
                        fromDate,
                        toDate,
                        PageRequest.of(0, 5)
                )
        );

        // 6️⃣ Top Products
        dto.setTopProducts(
                orderRepository.getTopSellingProductsBetween(
                        fromDate,
                        toDate,
                        PageRequest.of(0, 5)
                )
        );

        // 7️⃣ Low Stock (không phụ thuộc date)
        dto.setLowStockItems(
                variantSizeRepository.findLowStock(5)
        );

        return dto;
    }

    @Override
    public void exportRevenue(
            String from,
            String to,
            HttpServletResponse response
    ) throws IOException {
        DashboardResponseDTO dashboard = getDashboard(from, to);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=revenue.csv"
        );

        PrintWriter writer = response.getWriter();
        writer.write("\uFEFF");

        writer.println("Date,Revenue");

        for (RevenueByDayDTO r : dashboard.getRevenueByDay()) {
            writer.println(
                    r.getDate() + "," +
                            r.getRevenue().toPlainString()
            );
        }

        writer.flush();
        writer.close();
    }

    @Override
    public void exportRecentOrders(
            String from,
            String to,
            HttpServletResponse response
    ) throws IOException {

        DashboardResponseDTO dashboard =
                getDashboard(from, to);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=recent-orders.csv"
        );

        PrintWriter writer = response.getWriter();
        writer.write("\uFEFF");

        writer.println("Id,Receiver Name,Final Price,Status,Created At");

        for (RecentOrderDTO r : dashboard.getRecentOrders()) {
            writer.println(
                    r.getId() + ","
                        + r.getReceiverName() + ","
                            + r.getFinalPrice() + ","
                                + r.getStatus() + ","
                                    + r.getCreatedAt()
            );
        }

        writer.flush();
        writer.close();
    }

    @Override
    public void exportTopProducts(
            String from,
            String to,
            HttpServletResponse response
    ) throws IOException {

        DashboardResponseDTO dashboard =
                getDashboard(from, to);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=top_products.csv"
        );

        PrintWriter writer = response.getWriter();
        writer.write("\uFEFF");

        writer.println("Product ID,Product Name,Total Sold");

        for (TopProductDTO p : dashboard.getTopProducts()) {
            writer.println(
                    p.getProductId() + "," +
                            "\"" + p.getProductName() + "\"," +
                            p.getTotalSold()
            );
        }

        writer.flush();
        writer.close();
    }

    @Override
    public void exportLowStock(HttpServletResponse response) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=low_stock.csv"
        );

        PrintWriter writer = response.getWriter();
        writer.write("\uFEFF");

        writer.println("Product,Color,Size,Stock");

        List<LowStockDTO> list =
                variantSizeRepository.findLowStock(1000);

        for (LowStockDTO item : list) {
            writer.println(
                    "\"" + item.getProductName() + "\"," +
                            item.getColorName() + "," +
                            item.getSizeName() + "," +
                            item.getStock()
            );
        }

        writer.flush();
        writer.close();
    }
}