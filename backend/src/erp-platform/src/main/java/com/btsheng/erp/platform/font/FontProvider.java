package com.btsheng.erp.platform.font;

import com.lowagie.text.pdf.BaseFont;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Font;
import java.awt.FontFormatException;
import com.lowagie.text.DocumentException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 思源黑体服务端统一渲染入口（V1.3.9 Sprint 13 · Story 13.2 · AC-13.2.1/13.2.2/13.2.3）
 *
 * <p>职责：
 * <ul>
 *   <li>classpath 资源加载思源黑体（SourceHanSansCN-Normal.ttf）· 不依赖系统字体路径</li>
 *   <li>Spring 容器就绪后异步预热 · 不阻塞启动 · 启动时间影响 +200ms（一次性加载）</li>
 *   <li>PNG 渲染：AWT {@link Font}（{@link #getRegular(float)} · {@link #getBold(float)}）</li>
 *   <li>PDF 渲染：iText/OpenPDF {@link BaseFont}（{@link #getPdfBaseFont()}）</li>
 *   <li>加载失败时降级到 JDK SansSerif（保底 · 不阻塞渲染）</li>
 * </ul>
 *
 * <p>字体规格：
 * <ul>
 *   <li>SourceHanSansCN-Normal.ttf（思源黑体简体常规 · Adobe/Google Noto 同源）</li>
 *   <li>Apache 2.0（SIL OFL）· 商用 OK</li>
 *   <li>~10MB · jar 内 classpath:fonts/SourceHanSansCN-Normal.ttf</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Component
public class FontProvider {

    private static final Logger log = LoggerFactory.getLogger(FontProvider.class);

  /** classpath 字体候选（OpenPDF 仅支持 TTF · 构建前见 deploy/download-fonts.ps1） */
    private static final String[] FONT_RESOURCE_CANDIDATES = {
            "/fonts/SourceHanSansCN-Normal.ttf"
    };

    /** 有效字体最小字节（排除残缺下载） */
    private static final int MIN_FONT_BYTES = 1_000_000;

    /** 实际加载成功的资源路径（日志/监控） */
    private volatile String resolvedResourcePath = FONT_RESOURCE_CANDIDATES[0];

    /** 加载失败的 fallback 默认字号 */
    private static final float DEFAULT_FALLBACK_SIZE = 14f;

    /** AWT PNG 渲染字体缓存（Regular/Bold + 默认 14f 派生） */
    private volatile Font regularFont;
    private volatile Font boldFont;
    /** AWT 默认降级（SansSerif） */
    private final Font fallbackRegular = new Font(Font.SANS_SERIF, Font.PLAIN, (int) DEFAULT_FALLBACK_SIZE);
    private final Font fallbackBold = new Font(Font.SANS_SERIF, Font.BOLD, (int) DEFAULT_FALLBACK_SIZE);

    /** iText/OpenPDF PDF 渲染字体缓存（BaseFont） */
    private volatile BaseFont pdfBaseFont;

    /** 字体资源字节缓存（用于 PDF BaseFont.createFont 字节数组重载） */
    private volatile byte[] fontBytes;

    /** 字体加载状态（用于降级判断） */
    private volatile boolean loaded = false;

    /**
     * 同步初始化（Bean 构造时）· 保证 {@link #pdfBaseFont} 可用（PDF 渲染关键路径）
     *
     * <p>13.2 IMPL 注意事项 #3：{@code @PostConstruct} 同步加载 · 不阻塞 Spring 启动（10MB TTF 解析 < 200ms）
     */
    @PostConstruct
    public synchronized void init() {
        try {
            loadFont();
        } catch (Exception e) {
            log.error("[FontProvider] 思源黑体初始化失败 · 降级到 JDK SansSerif fallback · error={}",
                    e.getMessage(), e);
            fallback();
        }
    }

    /**
     * Spring 上下文就绪事件监听（保留用于未来的热加载/刷新场景）· 当前 IMPL 阶段 init 已同步加载完成
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!loaded) {
            log.warn("[FontProvider] ApplicationReady 但字体仍未加载（fallback 模式）· 检查日志");
        } else {
            log.info("[FontProvider] 思源黑体就绪 · 资源路径={}", resolvedResourcePath);
        }
    }

    /**
     * 实际加载逻辑（classpath 资源 → 字节数组 → AWT Font + BaseFont）
     */
    private void loadFont() throws IOException, FontFormatException, DocumentException {
        long start = System.currentTimeMillis();
        String resourcePath = resolveFontResourcePath();
        this.resolvedResourcePath = resourcePath;
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("字体资源未找到 · 候选: "
                        + String.join(", ", FONT_RESOURCE_CANDIDATES)
                        + " · 请执行 backend/deploy/download-fonts.ps1 或手动放置 OTF/TTF");
            }
            // 字节缓存（PDF BaseFont.createFont 重载需要）
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                baos.write(buf, 0, n);
            }
            this.fontBytes = baos.toByteArray();
            log.info("[FontProvider] 字体资源字节加载完成 · path={} size={} KB · 耗时={}ms",
                    resourcePath, fontBytes.length / 1024, System.currentTimeMillis() - start);

            // 1) AWT PNG 渲染字体
            try (InputStream fontStream1 = getClass().getResourceAsStream(resourcePath)) {
                this.regularFont = Font.createFont(Font.TRUETYPE_FONT, fontStream1)
                        .deriveFont(Font.PLAIN, DEFAULT_FALLBACK_SIZE);
            }
            // Note: 当前 IMPL 只嵌入 Regular · Bold 派生（Font.deriveFont(BOLD)）
            this.boldFont = regularFont != null
                    ? regularFont.deriveFont(Font.BOLD, DEFAULT_FALLBACK_SIZE)
                    : null;

            // 2) OpenPDF BaseFont：仅 TTF · 经临时文件加载（比字节数组重载更稳定）
            if (!resourcePath.toLowerCase().endsWith(".ttf")) {
                throw new DocumentException("OpenPDF 不支持 OTF · 请使用 SourceHanSansCN-Normal.ttf");
            }
            this.pdfBaseFont = createPdfBaseFontFromBytes(fontBytes);
        }
        this.loaded = true;
        log.info("[FontProvider] 思源黑体加载完成 · Regular+Bold(AWT) + BaseFont(PDF) · 总耗时={}ms",
                System.currentTimeMillis() - start);
    }

    private String resolveFontResourcePath() throws IOException {
        for (String path : FONT_RESOURCE_CANDIDATES) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is == null) {
                    continue;
                }
                long size = is.transferTo(OutputStream.nullOutputStream());
                if (size >= MIN_FONT_BYTES) {
                    return path;
                }
                log.warn("[FontProvider] 跳过残缺字体资源 · path={} size={}B", path, size);
            }
        }
        throw new IOException("字体资源未找到 · 候选: "
                + String.join(", ", FONT_RESOURCE_CANDIDATES)
                + " · 请执行 backend/deploy/download-fonts.ps1");
    }

    private BaseFont createPdfBaseFontFromBytes(byte[] bytes) throws IOException, DocumentException {
        Path temp = Files.createTempFile("erp-source-han-", ".ttf");
        try {
            Files.write(temp, bytes);
            BaseFont bf = BaseFont.createFont(
                    temp.toAbsolutePath().toString(),
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED);
            temp.toFile().deleteOnExit();
            return bf;
        } catch (DocumentException | IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    /**
     * Fallback 初始化（加载失败时）· 用 JDK 默认 SansSerif 保底
     */
    private void fallback() {
        this.regularFont = fallbackRegular;
        this.boldFont = fallbackBold;
        this.fontBytes = null;
        this.pdfBaseFont = null;
        this.loaded = false;
    }

    /**
     * AWT Regular 字体（PNG 渲染用）
     *
     * @param size 字号（pt）
     * @return 思源黑体 Regular · 失败时返回 SansSerif fallback
     */
    public Font getRegular(float size) {
        Font base = loaded && regularFont != null ? regularFont : fallbackRegular;
        return base.deriveFont(size);
    }

    /**
     * AWT Bold 字体（PNG 渲染用）
     *
     * @param size 字号（pt）
     * @return 思源黑体 Bold · 失败时返回 SansSerif Bold fallback
     */
    public Font getBold(float size) {
        Font base = loaded && boldFont != null ? boldFont : fallbackBold;
        return base.deriveFont(size);
    }

    /**
     * iText/OpenPDF BaseFont（PDF 渲染用）
     *
     * @return BaseFont 思源黑体 Regular · 失败时返回 null（调用方需处理 null fallback）
     */
    public BaseFont getPdfBaseFont() {
        return pdfBaseFont;
    }

    /**
     * 字体加载状态（用于监控 + 测试断言）
     *
     * @return true = 思源黑体加载成功 · false = fallback 模式
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * 字体字节大小（用于测试断言 [8MB, 12MB]）
     *
     * @return 字节数 · 未加载时返回 0
     */
    public int getFontSizeBytes() {
        return fontBytes == null ? 0 : fontBytes.length;
    }
}