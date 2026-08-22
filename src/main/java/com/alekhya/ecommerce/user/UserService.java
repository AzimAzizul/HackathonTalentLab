package com.alekhya.ecommerce.user;

import com.alekhya.ecommerce.common.ForbiddenException;
import com.alekhya.ecommerce.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse login(String email) {
        return userRepository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("No account with email " + email + " was found."));
    }

    public User requireAdmin(Long userId) {
        if (userId == null) {
            throw new ForbiddenException("Admin access requires the X-User-Id header.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " was not found."));
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("User " + userId + " does not have admin access.");
        }
        return user;
    }

    public User requireCustomer(Long userId) {
        if (userId == null) {
            throw new ForbiddenException("Customer access is required to use the chatbot.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " was not found."));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ForbiddenException("The chatbot is only available to customers.");
        }
        return user;
    }
}
