package org.example.e_fashion.controller.customer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.OrderCreateRequestDTO;
import org.example.e_fashion.dto.response.OrderResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.mapper.OrderMapper;
import org.example.e_fashion.service.OrderService;
import org.example.e_fashion.utils.ExtractUserUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class OrderCustomerController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final ExtractUserUtils extractUserUtils;

    @PostMapping
    public OrderResponseDTO create(
            HttpServletRequest request,
            @Valid @RequestBody OrderCreateRequestDTO requestDTO
    ) {
        UserEntity user = extractUserUtils.extract(request);

        return orderMapper.toResponse(
                orderService.createOrderFromCart(user, requestDTO)
        );
    }

    @GetMapping("/my")
    public List<OrderResponseDTO> myOrders(HttpServletRequest request) {
        UserEntity user = extractUserUtils.extract(request);

        return orderService.getOrdersByUser(user.getId())
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @GetMapping("/{orderId}")
    public OrderResponseDTO getById(
            HttpServletRequest request,
            @PathVariable String orderId
    ) {
        UserEntity user = extractUserUtils.extract(request);

        return orderMapper.toResponse(
                orderService.getByIdAndUser(orderId, user.getId())
        );
    }
}
