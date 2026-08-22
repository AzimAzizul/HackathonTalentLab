package com.alekhya.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alekhya.ecommerce.common.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void getCatalogReturnsOnlyActiveProducts() {
        when(productRepository.findByStatus(ProductStatus.ACTIVE)).thenReturn(List.of(activeProduct()));

        List<ProductResponse> catalog = productService.getCatalog();

        assertThat(catalog).hasSize(1);
        assertThat(catalog.getFirst().status()).isEqualTo("ACTIVE");
    }

    @Test
    void createProductSavesANewActiveProduct() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(
                new CreateProductRequest("USB-C Hub", "Hub with HDMI ports.", new BigDecimal("2199.00"), 8));

        assertThat(response.name()).isEqualTo("USB-C Hub");
        assertThat(response.availableQuantity()).isEqualTo(8);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void updateProductChangesOnlyTheProvidedFields() {
        Product product = activeProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.updateProduct(
                1L, new UpdateProductRequest(null, "Compact mechanical keyboard.", null, 25));

        assertThat(response.name()).isEqualTo("Keyboard");
        assertThat(response.description()).isEqualTo("Compact mechanical keyboard.");
        assertThat(response.availableQuantity()).isEqualTo(25);
        assertThat(response.price()).isEqualByComparingTo("3499.00");
    }

    @Test
    void deleteProductSoftDeletesWithoutRemovingTheRow() {
        Product product = activeProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void catalogLookupHidesDiscontinuedProducts() {
        Product product = activeProduct();
        product.markDiscontinued();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.getProduct(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("was not found");
    }

    @Test
    void createProductStoresVariants() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(
                new CreateProductRequest("T-Shirt", "Regular fit.", new BigDecimal("799.00"), 25, "Size", "M,L"));

        assertThat(response.variantName()).isEqualTo("Size");
        assertThat(response.variantOptions()).containsExactly("M", "L");
    }

    @Test
    void updateProductSetsVariants() {
        Product product = activeProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.updateProduct(
                1L, new UpdateProductRequest(null, null, null, null, "Switch Type", "Tactile,Linear"));

        assertThat(response.variantName()).isEqualTo("Switch Type");
        assertThat(response.variantOptions()).containsExactly("Tactile", "Linear");
    }

    private Product activeProduct() {
        return new Product("Keyboard", "Mechanical keyboard", new BigDecimal("3499.00"), 10);
    }
}
