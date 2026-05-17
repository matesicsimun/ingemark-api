package com.ingemark.products;

import com.ingemark.products.entity.Product;
import com.ingemark.products.repository.ProductRepository;
import com.ingemark.products.service.ExchangeRateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductApiIntegrationTest {

    private static final String PRODUCT_JSON = """
            {
              "code": "ABC1234567",
              "name": "Widget",
              "price_eur": 100.00,
              "is_available": true
            }""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository repository;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void stubExchangeRate() {
        when(exchangeRateService.convertFromEur(any(), any()))
                .thenReturn(new BigDecimal("108.50"));
    }

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void createProduct_returns201WithLocationAndBody() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/products/")))
                .andExpect(jsonPath("$.code").value("ABC1234567"))
                .andExpect(jsonPath("$.price_usd").value(108.50));
    }

    @Test
    void createProduct_returns409WhenCodeAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void getProduct_returns200WithProduct() throws Exception {
        Long id = repository.save(
                new Product("ABC1234567", "Widget", new BigDecimal("100.00"), new BigDecimal("108.50"), true)
        ).getId();

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.code").value("ABC1234567"));
    }

    @Test
    void listProducts_returnsAllProducts() throws Exception {
        repository.save(new Product("ABC1234567", "A", new BigDecimal("10.00"), new BigDecimal("11.00"), true));
        repository.save(new Product("XYZ9876543", "B", new BigDecimal("20.00"), new BigDecimal("21.00"), false));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getProduct_returns400WhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createProduct_returns400WhenBodyIsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
