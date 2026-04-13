package org.example.e_fashion.controller.staff;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ChangeOrderStatusRequestDTO;
import org.example.e_fashion.dto.response.OrderResponseDTO;
import org.example.e_fashion.dto.response.OrderStatusHistoryResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.entity.enums.OrderStatus;
import org.example.e_fashion.entity.enums.PaymentStatus;
import org.example.e_fashion.mapper.OrderStatusHistoryMapper;
import org.example.e_fashion.service.OrderService;
import org.example.e_fashion.service.OrderStatusHistoryService;
import org.example.e_fashion.utils.ExtractUserUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/staff/orders")
@RequiredArgsConstructor
public class OrderStaffController {

    private final OrderService orderService;
    private final OrderStatusHistoryService historyService;
    private final OrderStatusHistoryMapper mapper;
    private final ExtractUserUtils extractUserUtils;

    @GetMapping
    public List<OrderResponseDTO> getAllOrders(
            @RequestParam(required = false) String status
    ) {
        if (status != null) {
            return orderService.getAllByStatus(OrderStatus.valueOf(status));
        }
        return orderService.getAll();
    }

    @GetMapping("/{orderId}")
    public OrderResponseDTO getOrderDetail(
            @PathVariable String orderId
    ) {
        return orderService.getById(orderId);
    }

    @GetMapping("/{orderId}/status-history")
    public List<OrderStatusHistoryResponseDTO> history(
            @PathVariable String orderId
    ) {
        return historyService.getHistoryByOrder(orderId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PutMapping("/{orderId}/status")
    public void changeStatus(
            HttpServletRequest request,
            @PathVariable String orderId,
            @RequestBody ChangeOrderStatusRequestDTO requestDTO
    ) {
        UserEntity user = extractUserUtils.extract(request);
        historyService.changeOrderStatus(orderId, requestDTO, user.getId());
    }

    @PutMapping("/{orderId}/payment-status")
    public void updatePaymentStatus(
            @PathVariable String orderId,
            @RequestParam PaymentStatus status
    ) {
        orderService.updatePaymentStatus(orderId, status);
    }

    @GetMapping("/export")
    public void exportOrders(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            HttpServletResponse response
    ) throws IOException {

        orderService.exportOrders(from, to, response);
    }
}

