package com.alekhya.ecommerce.cart;

import com.alekhya.ecommerce.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private CartStatus status;

    private OffsetDateTime createdAt;

    @Column(name = "last_activity_at")
    private OffsetDateTime lastActivityAt;

    @Column(name = "abandoned_at")
    private OffsetDateTime abandonedAt;

    @Column(name = "last_engaged_at")
    private OffsetDateTime lastEngagedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(User user) {
        this.user = user;
        this.status = CartStatus.ACTIVE;
        this.createdAt = OffsetDateTime.now();
        this.lastActivityAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public CartStatus getStatus() {
        return status;
    }

    public OffsetDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public OffsetDateTime getAbandonedAt() {
        return abandonedAt;
    }

    public OffsetDateTime getLastEngagedAt() {
        return lastEngagedAt;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void addItem(CartItem item) {
        items.add(item);
    }

    public void touch() {
        this.lastActivityAt = OffsetDateTime.now();
        this.abandonedAt = null;
    }

    public void markAbandoned() {
        this.abandonedAt = OffsetDateTime.now();
    }

    public void markEngaged() {
        this.lastEngagedAt = OffsetDateTime.now();
    }

    public void checkout() {
        status = CartStatus.CHECKED_OUT;
    }
}
