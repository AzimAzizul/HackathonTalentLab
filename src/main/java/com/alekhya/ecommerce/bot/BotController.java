package com.alekhya.ecommerce.bot;

import com.alekhya.ecommerce.user.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final BotService botService;
    private final UserService userService;

    public BotController(BotService botService, UserService userService) {
        this.botService = botService;
        this.userService = userService;
    }

    @GetMapping("/engagement/{userId}")
    public EngagementResponse engagement(@PathVariable Long userId) {
        userService.requireCustomer(userId);
        return botService.getEngagement(userId);
    }

    @PostMapping("/message")
    public BotMessageResponse message(@Valid @RequestBody BotMessageRequest request) {
        userService.requireCustomer(request.userId());
        return botService.sendMessage(request.userId(), request.message());
    }
}
