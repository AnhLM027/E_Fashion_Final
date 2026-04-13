package org.example.e_fashion.service;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.example.e_fashion.dto.request.OrderCreateRequestDTO;
import org.example.e_fashion.dto.response.OrderResponseDTO;
import org.example.e_fashion.entity.OrderEntity;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.entity.enums.OrderStatus;
import org.example.e_fashion.entity.enums.PaymentStatus;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    OrderEntity createOrderFromCart(UserEntity user, OrderCreateRequestDTO request);

    List<OrderEntity> getOrdersByUser(String userId);

    OrderEntity getByIdAndUser(String id, String userId);

    List<OrderResponseDTO> getAll();

    List<OrderResponseDTO> getAllByStatus(OrderStatus status);

    OrderResponseDTO getById(String orderId);

    void exportOrders(
            LocalDate from,
            LocalDate to,
            HttpServletResponse response
    ) throws IOException;

    void updatePaymentStatus(String orderId, PaymentStatus paymentStatus);
}