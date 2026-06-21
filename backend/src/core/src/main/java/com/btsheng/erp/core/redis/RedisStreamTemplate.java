package com.btsheng.erp.core.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream 模板（V1.3.7）
 *
 * <p>Stream 替代 RabbitMQ（运维成本降 50%）。本类提供最小封装：publish / consume。
 * Spring Data Redis 3.2.x API：{@code consume} 已重命名为 {@code read}，consumer group 通过
 * {@link Consumer} 对象传入。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
public class RedisStreamTemplate {

    private final StringRedisTemplate redis;

    @Autowired
    public RedisStreamTemplate(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String publish(String stream, Map<String, String> payload) {
        return redis.opsForStream().add(stream, payload).getValue();
    }

    public List<MapRecord<String, String, String>> consume(String stream, String consumerGroup, String consumer, int count) {
        StreamReadOptions options = StreamReadOptions.empty().count(count).block(Duration.ofSeconds(1));
        Consumer c = Consumer.from(consumerGroup, consumer);
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<MapRecord<String, String, String>> result =
                (List) redis.opsForStream().read(c, options, StreamOffset.create(stream, ReadOffset.lastConsumed()));
        return result;
    }

    public Long len(String stream) {
        return redis.opsForStream().size(stream);
    }

    public void ensureConsumerGroup(String stream, String group) {
        try {
            redis.opsForStream().createGroup(stream, group);
        } catch (Exception ignored) {
            // 消费组已存在
        }
    }

    public void acknowledge(String stream, String group, RecordId id) {
        redis.opsForStream().acknowledge(stream, group, id);
    }
}
