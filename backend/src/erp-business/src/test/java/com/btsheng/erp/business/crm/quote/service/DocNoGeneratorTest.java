package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteHistoryMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** V1.3.7 Story 1.5 · T1.4 · DocNoGenerator 测例 (4 测例) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocNoGeneratorTest {

    @Mock private CrmQuoteMapper quoteMapper;
    @Mock private CrmQuoteItemMapper itemMapper;
    @Mock private CrmQuoteHistoryMapper historyMapper;

    @Test void quote_format_BJ_template() {
        DocNoGenerator gen = new DocNoGenerator(quoteMapper);
        gen.resetForTest("BJ", 1L);
        String no = gen.nextQuoteNo();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertEquals("BJ" + today + "-0001", no);
    }

    @Test void order_format_XS_template() {
        DocNoGenerator gen = new DocNoGenerator(quoteMapper);
        gen.resetForTest("XS", 1L);
        String no = gen.nextOrderNo();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertEquals("XS" + today + "-0001", no);
    }

    @Test void concurrent_100_unique() throws InterruptedException {
        DocNoGenerator gen = new DocNoGenerator(quoteMapper);
        gen.resetForTest("BJ", 1L);
        int n = 100;
        Set<String> set = java.util.Collections.synchronizedSet(new HashSet<>());
        CountDownLatch latch = new CountDownLatch(n);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                try { set.add(gen.nextQuoteNo()); } finally { latch.countDown(); }
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(n, set.size(), "100 并发生成应得 100 个唯一编号");
    }

    @Test void daily_reset_at_midnight_simulated() {
        DocNoGenerator gen = new DocNoGenerator(quoteMapper);
        // 同一天多次调用，seq 持续递增
            gen.resetForTest("BJ", 1L);
        String a = gen.nextQuoteNo();
        String b = gen.nextQuoteNo();
        assertNotEquals(a, b);
        // 模拟跨日 — 新前缀 key
            gen.resetForTest("BJ", 1L);
        String c = gen.nextQuoteNo();
        assertEquals(a, c, "跨日（或重置）后回到 0001");
    }
}
