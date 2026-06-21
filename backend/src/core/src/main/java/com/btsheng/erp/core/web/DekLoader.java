package com.btsheng.erp.core.web;

import com.btsheng.erp.core.model.AesGcmUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;

/**
 * DEK 加载器（V1.3.6 P1 修补 · dev 兜底）
 *
 * <p>启动时读取 {@code /etc/erp/dek.key}：<br>
 * 1) 文件存在 → 读 32 字节（支持 hex 64 字符）<br>
 * 2) 文件不存在 → 回退到 {@code app.crypto.dek-dev}（仅 dev profile），logback 告警<br>
 * 3) 启动期不阻断；运行时检测加密字段 → 缺失时 fail-fast
 *
 * <p><b>红线</b>：甲方 IT 独立保管 DEK（{@code chmod 600}）；生产环境绝不允许 dev 兜底。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class DekLoader {

    private static final Logger log = LoggerFactory.getLogger(DekLoader.class);

    private static volatile byte[] DEK;
    private static volatile boolean devFallback;
    private static volatile String loadedFrom;

    private DekLoader() {
    }

    /**
     * 启动期加载 DEK（不抛异常，dev 兜底 + 告警）。
     */
    public static synchronized void loadOrFallback(String filePath, String devBase64) {
        Path p = Paths.get(filePath);
        if (Files.exists(p)) {
            try {
                byte[] raw = Files.readAllBytes(p);
                DEK = parseDek(raw);
                loadedFrom = "file:" + filePath;
                devFallback = false;
                log.info("[DEK] loaded from {} ({} bytes)", loadedFrom, DEK.length);
                return;
            } catch (IOException e) {
                log.error("[DEK] fail to read {}, cause={}", filePath, e.getMessage());
            }
        }
        log.warn("[DEK] {} 缺失，启动 dev 兜底（仅 dev profile 生效）。生产环境请配置甲方 IT 保管的 32 字节密钥。", filePath);
        if (devBase64 != null && !devBase64.isEmpty()) {
            try {
                byte[] raw = java.util.Base64.getDecoder().decode(devBase64);
                DEK = parseDek(raw);
                loadedFrom = "nacos:app.crypto.dek-dev";
                devFallback = true;
                log.warn("[DEK] dev 兜底已激活（devFallback=true），严禁生产使用！");
            } catch (Exception ex) {
                DEK = AesGcmUtil.generateDek();
                loadedFrom = "memory:random-32B";
                devFallback = true;
                log.error("[DEK] dev 兜底 Base64 解码失败，fallback 至内存随机 32B（重启后失效）", ex);
            }
        } else {
            DEK = AesGcmUtil.generateDek();
            loadedFrom = "memory:random-32B";
            devFallback = true;
        }
    }

    /**
     * 解析 DEK 字节（支持 32 字节二进制或 64 字符 hex）。
     */
    private static byte[] parseDek(byte[] raw) {
        if (raw.length == AesGcmUtil.DEK_LENGTH) {
            return raw;
        }
        String s = new String(raw).trim();
        if (s.length() == 64) {
            try {
                return HexFormat.of().parseHex(s);
            } catch (IllegalArgumentException ignored) {
                // fallthrough
            }
        }
        throw new IllegalStateException("DEK 长度/格式错误：期望 32 字节二进制 或 64 字符 hex（实际 "
                + raw.length + "B / '" + s.substring(0, Math.min(20, s.length())) + "...'）");
    }

    public static byte[] requireDek() {
        if (DEK == null) {
            synchronized (DekLoader.class) {
                if (DEK == null) {
                    String envDev = System.getenv("DEK_DEV");
                    loadOrFallback("/etc/erp/dek.key", envDev != null ? envDev : "");
                }
            }
        }
        if (DEK == null) {
            throw new IllegalStateException("DEK 缺失：请在 /etc/erp/dek.key 部署 32 字节数据加密密钥，或配置 app.crypto.dek-dev / DEK_DEV（V1.3.6 数据安全红线）");
        }
        return DEK;
    }

    public static boolean isDevFallback() {
        return devFallback;
    }

    public static String getLoadedFrom() {
        return loadedFrom;
    }

    public static boolean isReady() {
        return DEK != null;
    }
}
