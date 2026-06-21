package com.btsheng.erp.business.realtime;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** SSE 兜底（inventory:alert / payment:remind · Spec B.2） */
@RestController
@RequestMapping("/sse")
public class SseAlertController {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @GetMapping(value = "/inventory/alert", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter inventoryAlert() {
        return streamEvents("inventory:alert", Map.of("level", "WARN", "message", "库存预警"));
    }

    @GetMapping(value = "/payment/remind", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter paymentRemind() {
        return streamEvents("payment:remind", Map.of("level", "INFO", "message", "回款提醒"));
    }

    private SseEmitter streamEvents(String eventName, Map<String, Object> payload) {
        SseEmitter emitter = new SseEmitter(0L);
        executor.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 0, 600, TimeUnit.SECONDS);
        return emitter;
    }
}
