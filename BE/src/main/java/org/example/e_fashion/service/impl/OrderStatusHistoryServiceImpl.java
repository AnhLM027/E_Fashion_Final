package org.example.e_fashion.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ChangeOrderStatusRequestDTO;
import org.example.e_fashion.entity.*;
import org.example.e_fashion.entity.enums.OrderStatus;
import org.example.e_fashion.repository.OrderRepository;
import org.example.e_fashion.repository.OrderStatusHistoryRepository;
import org.example.e_fashion.repository.UserRepository;
import org.example.e_fashion.service.OrderStatusHistoryService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderStatusHistoryServiceImpl implements OrderStatusHistoryService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository historyRepository;

    @Override
    public void changeOrderStatus(
            String orderId,
            ChangeOrderStatusRequestDTO request,
            String adminId
    ) {

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = OrderStatus.valueOf(request.getNewStatus());

        validateStatusFlow(oldStatus, newStatus);

        if (newStatus == OrderStatus.CANCELLED) {

            for (OrderItemEntity item : order.getItems()) {

                ProductVariantSizeEntity variantSize =
                        item.getProductVariantSize();

                int quantity = item.getQuantity();

                // Trả lại reservedStock
                variantSize.setReservedStock(
                        variantSize.getReservedStock() - quantity
                );
            }
        }

        if (newStatus == OrderStatus.DELIVERED) {

            for (OrderItemEntity item : order.getItems()) {

                ProductVariantSizeEntity variantSize =
                        item.getProductVariantSize();

                int quantity = item.getQuantity();

                if (variantSize.getReservedStock() < quantity) {
                    throw new RuntimeException("Reserved stock inconsistency");
                }

                // Trừ stock thật
                variantSize.setStock(
                        variantSize.getStock() - quantity
                );

                // Giảm reserved
                variantSize.setReservedStock(
                        variantSize.getReservedStock() - quantity
                );
            }
        }

        if (newStatus == OrderStatus.RETURNED) {

            for (OrderItemEntity item : order.getItems()) {

                ProductVariantSizeEntity variantSize =
                        item.getProductVariantSize();

                int quantity = item.getQuantity();

                // Hoàn lại stock
                variantSize.setStock(
                        variantSize.getStock() + quantity
                );
            }
        }

        order.setStatus(newStatus);

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrder(order);
        history.setPreviousStatus(oldStatus.name());
        history.setNewStatus(newStatus.name());
        history.setNote(request.getNote());

        if (request.getChangedBy() != null) {
            UserEntity user = userRepository
                    .findById(adminId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            history.setCreatedBy(user);
        }

        historyRepository.save(history);
    }

    @Override
    public List<OrderStatusHistoryEntity> getHistoryByOrder(String orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return historyRepository
                .findByOrder_IdOrderByCreatedAtAsc(orderId);
    }

    @Override
    public List<OrderStatusHistoryEntity> getHistoryByOrderAndUser(String orderId, String userId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền xem order này");
        }
        return historyRepository
                .findByOrder_IdOrderByCreatedAtAsc(orderId);
    }

    private void validateStatusFlow(
            OrderStatus oldStatus,
            OrderStatus newStatus
    ) {
        if (oldStatus == OrderStatus.CANCELLED
                || oldStatus == OrderStatus.RETURNED) {
            throw new RuntimeException("Order already closed");
        }

        if (oldStatus == OrderStatus.DELIVERED
                && newStatus != OrderStatus.RETURNED) {
            throw new RuntimeException("Delivered order can only be returned");
        }
    }
}