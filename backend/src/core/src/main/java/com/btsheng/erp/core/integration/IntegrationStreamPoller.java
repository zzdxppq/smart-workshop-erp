package com.btsheng.erp.core.integration;

import com.btsheng.erp.core.redis.RedisStreamTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Redis Stream 消费组轮询基类 */
public abstract class IntegrationStreamPoller {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RedisStreamTemplate redisStreamTemplate;
    private final String consumerGroup;
    private final String consumerName;
    private volatile boolean streamUnsupported;

    protected IntegrationStreamPoller(RedisStreamTemplate redisStreamTemplate,
                                      String consumerGroup,
                                      String consumerName) {
        this.redisStreamTemplate = redisStreamTemplate;
        this.consumerGroup = consumerGroup;
        this.consumerName = consumerName;
    }

    protected void poll(int batchSize, Consumer<MapRecord<String, String, String>> handler) {
        if (streamUnsupported) {
            return;
        }
        try {
            redisStreamTemplate.ensureConsumerGroup(IntegrationStreams.STREAM_INTEGRATION, consumerGroup);
            List<MapRecord<String, String, String>> records = redisStreamTemplate.consume(
                    IntegrationStreams.STREAM_INTEGRATION, consumerGroup, consumerName, batchSize);
            if (records == null || records.isEmpty()) {
                return;
            }
            for (MapRecord<String, String, String> record : records) {
                try {
                    handler.accept(record);
                    RecordId id = record.getId();
                    if (id != null) {
                        redisStreamTemplate.acknowledge(IntegrationStreams.STREAM_INTEGRATION, consumerGroup, id);
                    }
                } catch (Exception e) {
                    log.warn("[{}] integration event handle failed id={}: {}",
                            getClass().getSimpleName(), record.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            if (isStreamUnsupported(e)) {
                streamUnsupported = true;
                log.warn("[{}] Redis Stream not supported (need Redis 5.0+); integration polling disabled: {}",
                        getClass().getSimpleName(), rootMessage(e));
                return;
            }
            throw e;
        }
    }

    private static boolean isStreamUnsupported(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("unknown command") && msg.toUpperCase().contains("XREAD")) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    protected static String field(Map<String, String> body, String key) {
        return body == null ? null : body.get(key);
    }
}
