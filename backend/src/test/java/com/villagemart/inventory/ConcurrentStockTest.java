package com.villagemart.inventory;

import com.villagemart.common.ApiException;
import com.villagemart.payment.PaymentDtos.PaymentLine;
import com.villagemart.sales.PaymentMode;
import com.villagemart.sales.SaleDtos.SaleItemRequest;
import com.villagemart.sales.SaleDtos.SaleRequest;
import com.villagemart.sales.SaleService;
import com.villagemart.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Parallel sales against the same product must never oversell or create negative stock. */
class ConcurrentStockTest extends IntegrationTestSupport {

    @Autowired
    SaleService saleService;

    @Autowired
    StockMovementRepository movementRepository;

    @Test
    void concurrentSalesNeverOversell() throws Exception {
        long product = createProduct("10.00", "15.00", "1", "20");
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                try {
                    saleService.create(request(product, "3"), UUID.randomUUID().toString());
                    return "OK";
                } catch (ApiException e) {
                    return e.getCode();
                }
            });
        }
        List<String> results = new ArrayList<>();
        for (Future<String> f : pool.invokeAll(tasks, 120, TimeUnit.SECONDS)) {
            results.add(f.get());
        }
        pool.shutdown();

        assertThat(results.stream().filter("OK"::equals).count()).isEqualTo(6);
        assertThat(results.stream().filter("INSUFFICIENT_STOCK"::equals).count()).isEqualTo(4);
        assertThat(stock(product)).isEqualByComparingTo("2");
        long saleMovements = movementRepository.findAll().stream()
                .filter(m -> m.getProduct().getId().equals(product) && m.getMovementType() == MovementType.SALE).count();
        assertThat(saleMovements).isEqualTo(6);
    }

    @Test
    void concurrentRetriesWithSameKeyCreateOneSale() throws Exception {
        long product = createProduct("10.00", "15.00", "1", "20");
        String key = UUID.randomUUID().toString();
        SaleRequest req = request(product, "2");
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> {
                try {
                    return saleService.create(req, key).id();
                } catch (RuntimeException e) {
                    return -1L;
                }
            });
        }
        List<Long> ids = new ArrayList<>();
        for (Future<Long> f : pool.invokeAll(tasks, 120, TimeUnit.SECONDS)) {
            ids.add(f.get());
        }
        pool.shutdown();
        assertThat(ids.stream().filter(id -> id > 0).distinct().count()).isEqualTo(1);
        assertThat(stock(product)).isEqualByComparingTo("18");
    }

    private static SaleRequest request(long productId, String qty) {
        return new SaleRequest(null, null, null, null,
                List.of(new SaleItemRequest(productId, new BigDecimal(qty), null, null)),
                null, PaymentMode.CASH, List.<PaymentLine>of(), false, false, null);
    }
}
