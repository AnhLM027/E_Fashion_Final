package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.dto.dashboard.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DashboardResponseDTO {

    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long pendingOrders;
    private Long totalUsers;

    private List<RevenueByDayDTO> revenueByDay;
    private List<RecentOrderDTO> recentOrders;
    private List<TopProductDTO> topProducts;
    private List<LowStockDTO> lowStockItems;
}