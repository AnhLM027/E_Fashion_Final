package org.example.e_fashion.controller.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ChangeOrderStatusRequestDTO;
import org.example.e_fashion.dto.response.OrderStatusHistoryResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.mapper.OrderStatusHistoryMapper;
import org.example.e_fashion.service.OrderStatusHistoryService;
import org.example.e_fashion.utils.ExtractUserUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderStatusController {

    private final OrderStatusHistoryService historyService;
    private final OrderStatusHistoryMapper mapper;
    private final ExtractUserUtils extractUserUtils;

    @GetMapping("/{orderId}/status-history")
    public List<OrderStatusHistoryResponseDTO> history(
            HttpServletRequest request,
            @PathVariable String orderId
    ) {
        UserEntity user = extractUserUtils.extract(request);

        return historyService.getHistoryByOrderAndUser(orderId, user.getId())
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}