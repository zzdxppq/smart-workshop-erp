package com.btsheng.erp.business.crm.drawing.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * 图纸文件 MinIO / 本地路径统一读写（预览 PDF · CAD 附件下载）
 */
@Slf4j
@Service
public class DrawingMinioFileService {

    private static final Map<String, String> EXT_CONTENT_TYPE = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("dxf", "application/dxf"),
            Map.entry("dwg", "application/acad"),
            Map.entry("step", "application/step"),
            Map.entry("stp", "application/step"),
            Map.entry("nc", "text/plain")
    );

    private final MinioClient minioClient;
    private final String defaultBucket;
    private final Path localFallbackDir;
    private final boolean useLocalFallback;

    @Autowired
    public DrawingMinioFileService(
            @Value("${app.minio.endpoint}") String minioEndpoint,
            @Value("${app.minio.access-key}") String accessKey,
            @Value("${app.minio.secret-key}") String secretKey,
            @Value("${app.minio.bucket-drawing}") String bucketDrawing,
            @Value("${app.minio.local-fallback-dir:./data/drawing-files}") String localFallbackDir,
            @Value("${app.minio.use-local-fallback:false}") boolean useLocalFallback) {
        this.defaultBucket = bucketDrawing;
        this.localFallbackDir = Path.of(localFallbackDir).toAbsolutePath().normalize();
        MinioClient client = null;
        boolean localOnly = useLocalFallback;
        if (!useLocalFallback) {
            try {
                client = MinioClient.builder()
                        .endpoint(minioEndpoint)
                        .credentials(accessKey, secretKey)
                        .build();
                ensureBucket(client, defaultBucket);
            } catch (Exception e) {
                log.warn("MinIO 不可用，启用本地文件回退: {}", e.getMessage());
                localOnly = true;
            }
        } else {
            log.info("MinIO 本地回退模式已启用: {}", this.localFallbackDir);
        }
        this.minioClient = client;
        this.useLocalFallback = localOnly;
        if (this.useLocalFallback) {
            try {
                Files.createDirectories(this.localFallbackDir);
            } catch (Exception e) {
                throw new RuntimeException("本地附件目录创建失败: " + e.getMessage(), e);
            }
        }
    }

    public MinioClient client() {
        return minioClient;
    }

    public String defaultBucket() {
        return defaultBucket;
    }

    public boolean isLocalFallback() {
        return useLocalFallback;
    }

    public byte[] readBytes(String filePath) throws Exception {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("FILE_PATH_EMPTY");
        }
        String path = filePath.trim();
        if (path.startsWith("minio://")) {
            if (useLocalFallback || minioClient == null) {
                throw new IllegalStateException("MINIO_UNAVAILABLE_FOR_PATH");
            }
            String rest = path.substring("minio://".length());
            int slash = rest.indexOf('/');
            if (slash <= 0) {
                throw new IllegalArgumentException("MINIO_PATH_INVALID");
            }
            String bucket = rest.substring(0, slash);
            String objectKey = rest.substring(slash + 1);
            try (InputStream in = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
                return in.readAllBytes();
            }
        }
        if (path.startsWith("local://")) {
            return Files.readAllBytes(Path.of(path.substring("local://".length())));
        }
        if (path.startsWith("/") || path.matches("^[A-Za-z]:[\\\\/].*")) {
            return Files.readAllBytes(Path.of(path));
        }
        throw new IllegalArgumentException("UNSUPPORTED_FILE_PATH");
    }

    public void putObject(String objectName, InputStream stream, long size, String contentType) throws Exception {
        if (useLocalFallback || minioClient == null) {
            Path target = resolveLocalObject(objectName);
            Files.createDirectories(target.getParent());
            try (InputStream in = stream) {
                Files.copy(in, target);
            }
            return;
        }
        ensureBucket(minioClient, defaultBucket);
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(defaultBucket)
                .object(objectName)
                .stream(stream, size, -1)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build());
    }

    public String toMinioUri(String objectName) {
        if (useLocalFallback || minioClient == null) {
            Path target = resolveLocalObject(objectName);
            return "local://" + target.toAbsolutePath().normalize();
        }
        return "minio://" + defaultBucket + "/" + objectName;
    }

    private Path resolveLocalObject(String objectName) {
        Path p = localFallbackDir;
        for (String segment : objectName.split("/")) {
            if (!segment.isBlank()) {
                p = p.resolve(segment);
            }
        }
        return p;
    }

    public static boolean isPdf(byte[] data) {
        return data != null && data.length >= 5
                && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F';
    }

    public static String contentTypeForFileName(String fileName) {
        if (fileName == null) return "application/octet-stream";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return "application/octet-stream";
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return EXT_CONTENT_TYPE.getOrDefault(ext, "application/octet-stream");
    }

    private void ensureBucket(MinioClient client, String bucket) throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
