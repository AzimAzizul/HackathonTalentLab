package com.alekhya.ecommerce.product;

import com.alekhya.ecommerce.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getCatalog() {
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream().map(ProductResponse::from).toList();
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    public ProductResponse getProduct(Long productId) {
        return ProductResponse.from(findActiveProduct(productId));
    }

    public Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product " + productId + " was not found."));
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = new Product(request.name(), request.description(), request.price(), request.availableQuantity());
        product.setVariantName(request.variantName());
        product.setVariantOptions(request.variantOptions());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request) {
        Product product = findProduct(productId);
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.availableQuantity() != null) {
            product.setAvailableQuantity(request.availableQuantity());
        }
        if (request.variantName() != null) {
            product.setVariantName(request.variantName());
        }
        if (request.variantOptions() != null) {
            product.setVariantOptions(request.variantOptions());
        }
        return ProductResponse.from(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        findProduct(productId).markDiscontinued();
    }

    private Product findActiveProduct(Long productId) {
        Product product = findProduct(productId);
        if (product.getStatus() == ProductStatus.DISCONTINUED) {
            throw new NotFoundException("Product " + productId + " was not found.");
        }
        return product;
    }
}
