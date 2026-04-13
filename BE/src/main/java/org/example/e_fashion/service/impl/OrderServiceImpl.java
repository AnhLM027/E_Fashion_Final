package org.example.e_fashion.service.impl;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.OrderCreateRequestDTO;
import org.example.e_fashion.dto.response.OrderResponseDTO;
import org.example.e_fashion.entity.*;
import org.example.e_fashion.entity.enums.DiscountType;
import org.example.e_fashion.entity.enums.OrderStatus;
import org.example.e_fashion.entity.enums.PaymentStatus;
import org.example.e_fashion.mapper.OrderItemMapper;
import org.example.e_fashion.mapper.OrderMapper;
import org.example.e_fashion.repository.CartRepository;
import org.example.e_fashion.repository.CouponRepository;
import org.example.e_fashion.repository.OrderRepository;
import org.example.e_fashion.service.OrderService;
import org.example.e_fashion.service.OrderStatusHistoryService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderStatusHistoryService orderStatusHistoryService;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CouponRepository couponRepository;

    @Override
    public OrderEntity createOrderFromCart(UserEntity user, OrderCreateRequestDTO request) {

        CartEntity cart = cartRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart has no items");
        }

        OrderEntity order = orderMapper.toEntity(request);
        order.setUser(user);

        BigDecimal total = BigDecimal.ZERO;

        for (CartItemEntity cartItem : cart.getItems()) {

            ProductVariantSizeEntity variantSize = cartItem.getProductVariantSize();

            BigDecimal salePrice = variantSize.getSalePrice();
            BigDecimal price = (salePrice != null && salePrice.signum() > 0)
                    ? salePrice
                    : variantSize.getOriginalPrice();

            int quantity = cartItem.getQuantity();

            int availableStock =
                    variantSize.getStock() - variantSize.getReservedStock();

            if (availableStock < quantity) {
                throw new RuntimeException(
                        "Not enough stock for SKU: " + variantSize.getSku()
                );
            }

            variantSize.setReservedStock(
                    variantSize.getReservedStock() + quantity
            );

            OrderItemEntity orderItem =
                    orderItemMapper.fromCartItem(cartItem, order, price);

            order.getItems().add(orderItem);

            BigDecimal itemTotal =
                    price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            total = total.add(itemTotal);
        }

        BigDecimal discount = BigDecimal.ZERO;
        String appliedCouponCode = null;

        if (request.getCouponCode() != null &&
                !request.getCouponCode().isBlank()) {

            CouponEntity coupon = couponRepository
                    .findByCode(request.getCouponCode())
                    .orElseThrow(() ->
                            new RuntimeException("Coupon not found"));

            // 1️⃣ check active
            if (!Boolean.TRUE.equals(coupon.getIsActive())) {
                throw new RuntimeException("Coupon inactive");
            }

            // 2️⃣ check thời gian
            LocalDateTime now = LocalDateTime.now();

            if (coupon.getStartDate() != null &&
                    now.isBefore(coupon.getStartDate())) {
                throw new RuntimeException("Coupon not started yet");
            }

            if (coupon.getEndDate() != null &&
                    now.isAfter(coupon.getEndDate())) {
                throw new RuntimeException("Coupon expired");
            }

            // 3️⃣ check min order
            if (total.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new RuntimeException("Order value too low");
            }

            // 4️⃣ check usage limit
            if (coupon.getUsageLimit() != null &&
                    coupon.getUsageLimit() == 0) {
                throw new RuntimeException("Coupon usage limit reached");
            }

            // 5️⃣ tính discount
            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {

                discount = total
                        .multiply(coupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            } else {
                discount = coupon.getDiscountValue();
            }

            // không cho giảm quá tổng tiền
            if (discount.compareTo(total) > 0) {
                discount = total;
            }

            appliedCouponCode = coupon.getCode();

            // 🔥 TRỪ usageLimit
            if (coupon.getUsageLimit() != null &&
                    coupon.getUsageLimit() > 0) {

                coupon.setUsageLimit(
                        coupon.getUsageLimit() - 1
                );

                if (coupon.getUsageLimit() == 0) {
                    coupon.setIsActive(false);
                }
            }
        }

        BigDecimal shippingFee =
                request.getShippingFee() != null
                        ? request.getShippingFee()
                        : BigDecimal.ZERO;

        order.setShippingFee(shippingFee);
        order.setTotalPrice(total);

        order.setCouponCode(appliedCouponCode);
        order.setDiscountAmount(discount);

        BigDecimal finalPrice =
                total.subtract(discount).add(shippingFee);

        order.setFinalPrice(finalPrice);

        cart.getItems().clear();

        return orderRepository.save(order);
    }

    @Override
    public List<OrderEntity> getOrdersByUser(String userId) {
        return orderRepository.findByUser_Id(userId);
    }

    @Override
    public OrderEntity getByIdAndUser(String id, String userId) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Override
    public List<OrderResponseDTO> getAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public List<OrderResponseDTO> getAllByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponseDTO getById(String orderId) {
        return orderRepository.findById(orderId)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @Override
    public void exportOrders(
            LocalDate from,
            LocalDate to,
            HttpServletResponse response
    ) throws IOException {

        LocalDateTime fromDateTime = from != null
                ? from.atStartOfDay()
                : null;

        LocalDateTime toDateTime = to != null
                ? to.atTime(23, 59, 59)
                : null;

        System.out.println(fromDateTime + " " + toDateTime);

        List<OrderEntity> orders =
                orderRepository.findForExport(fromDateTime, toDateTime);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=orders.csv"
        );

        PrintWriter writer = response.getWriter();

        // UTF-8 BOM để Excel không lỗi tiếng Việt
        writer.write("\uFEFF");

        // Header
        writer.println(
                "Order ID,Receiver Name,Phone,Status,Payment Status,Total Price,Discount,Final Price,Created At"
        );

        for (OrderEntity order : orders) {

            writer.println(
                    csv(order.getId()) + "," +
                            csv(order.getReceiverName()) + "," +
                            csv(order.getReceiverPhone()) + "," +
                            csv(order.getStatus().name()) + "," +
                            csv(order.getPaymentStatus().name()) + "," +
                            csv(format(order.getTotalPrice())) + "," +
                            csv(format(order.getDiscountAmount())) + "," +
                            csv(format(order.getFinalPrice())) + "," +
                            csv(order.getCreatedAt().toString())
            );
        }

        writer.flush();
        writer.close();
    }

    @Override
    public void updatePaymentStatus(String orderId, PaymentStatus paymentStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setPaymentStatus(paymentStatus);
        orderRepository.save(order);
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String format(BigDecimal value) {
        return value != null ? value.toPlainString() : "0";
    }
}