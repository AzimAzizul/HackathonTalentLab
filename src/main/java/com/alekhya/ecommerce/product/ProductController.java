package com.alekhya.ecommerce.product;

import com.alekhya.ecommerce.user.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    public ProductController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping
    public List<ProductResponse> getProducts() {
        return productService.getCatalog();
    }

    @GetMapping("/all")
    public List<ProductResponse> getAllProducts(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        userService.requireAdmin(userId);
        return productService.getAllProducts();
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId) {
        return productService.getProduct(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        userService.requireAdmin(userId);
        return productService.createProduct(request);
    }

    @PatchMapping("/{productId}")
    public ProductResponse updateProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        userService.requireAdmin(userId);
        return productService.updateProduct(productId, request);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long productId
    ) {
        userService.requireAdmin(userId);
        productService.deleteProduct(productId);
    }
}
