package com.alekhya.ecommerce.order;

import com.alekhya.ecommerce.cart.Cart;
import com.alekhya.ecommerce.cart.CartItem;
import com.alekhya.ecommerce.cart.CartRepository;
import com.alekhya.ecommerce.cart.CartStatus;
import com.alekhya.ecommerce.common.BadRequestException;
import com.alekhya.ecommerce.common.ConflictException;
import com.alekhya.ecommerce.common.NotFoundException;
import com.alekhya.ecommerce.user.User;
import com.alekhya.ecommerce.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderService(CartRepository cartRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse checkout(Long userId) {
        User user = getUser(userId);
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("User " + userId + " does not have an active cart."));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("The active cart is empty.");
        }

        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getAvailableQuantity() < item.getQuantity()) {
                throw new ConflictException("Not enough stock for product " + item.getProduct().getId() + ".");
            }
        }

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CustomerOrder order = new CustomerOrder(user, total);
        for (CartItem item : cart.getItems()) {
            item.getProduct().reduceQuantity(item.getQuantity());
            order.addItem(new OrderItem(order, item.getProduct(), item.getQuantity()));
        }

        cart.checkout();
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Long userId) {
        getUser(userId);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " was not found."));
        return OrderResponse.from(order);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " was not found."));
    }
}
