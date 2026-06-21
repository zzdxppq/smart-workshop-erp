package com.btsheng.erp.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 雪花 ID 生成器（V1.3.7）
 *
 * <p>61 位 = 1 符号位 + 41 时间戳 + 10 工作机器（5 datacenter + 5 worker）+ 12 序列号。
 * 单实例单机 4096/ms（理论）；多 worker 可水平扩展。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class SnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    /** 起始时间戳（2026-01-01） */
    private static final long EPOCH = 1767225600000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        this(0L, 0L);
    }

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId out of range [0, " + MAX_WORKER_ID + "]");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId out of range [0, " + MAX_DATACENTER_ID + "]");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        log.info("[Snowflake] workerId={} datacenterId={} epoch={}", workerId, datacenterId, EPOCH);
    }

    public synchronized long nextId() {
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            log.warn("[Snowflake] clock moved backwards. Refusing to generate id for {} ms",
                    lastTimestamp - timestamp);
            timestamp = waitUntil(lastTimestamp);
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0L) {
                timestamp = nextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitUntil(long target) {
        long ts = currentTimeMillis();
        while (ts <= target) {
            ts = currentTimeMillis();
        }
        return ts;
    }

    private long nextMillis(long lastTimestamp) {
        long ts = currentTimeMillis();
        while (ts <= lastTimestamp) {
            ts = currentTimeMillis();
        }
        return ts;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
