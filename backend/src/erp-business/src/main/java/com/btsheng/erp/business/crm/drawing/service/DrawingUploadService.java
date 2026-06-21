package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.dto.DrawingCreateRequest;
import com.btsheng.erp.business.crm.drawing.dto.DrawingReleaseRequest;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * V1.3.8 · Epic 3 · 图纸文件上传 + 快速建档
 *
 * <p>PRD V2.0：上传仅生成图纸记录（DWG-），不自动生成物料编码；材质/规格必填。
 */
@Service
public class DrawingUploadService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "dwg", "dxf", "step", "stp", "nc");

    private final DrawingService drawingService;
    private final DocNoGenerator docNoGenerator;
    private final MinioClient minioClient;
    private final String bucketDrawing;

    @Autowired
    public DrawingUploadService(
            DrawingService drawingService,
            DocNoGenerator docNoGenerator,
            @Value("${app.minio.endpoint}") String minioEndpoint,
            @Value("${app.minio.access-key}") String accessKey,
            @Value("${app.minio.secret-key}") String secretKey,
            @Value("${app.minio.bucket-drawing}") String bucketDrawing) {
        this.drawingService = drawingService;
        this.docNoGenerator = docNoGenerator;
        this.bucketDrawing = bucketDrawing;
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(minioEndpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            ensureBucketExists();
        } catch (Exception e) {
            throw new RuntimeException("MinIO 初始化失败: " + e.getMessage(), e);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketDrawing).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketDrawing).build());
        }
    }

    /**
     * 上传图纸文件并创建档案：仅 DWG 记录，不生成 WL 料号
     */
    public Result<CrmDrawing> uploadAndCreate(MultipartFile file, String title,
                                              String materialGrade, String specSize,
                                              String customerDrawingNo, java.math.BigDecimal unitWeight,
                                              Long operatorUserId, boolean releaseAfter) {
        if (file == null || file.isEmpty()) {
            return Result.fail(40001, "FILE_REQUIRED");
        }
        if (operatorUserId == null) {
            return Result.fail(40001, "OPERATOR_USER_ID_REQUIRED");
        }
        if (materialGrade == null || materialGrade.isBlank()) {
            return Result.fail(40001, "MATERIAL_GRADE_REQUIRED");
        }
        if (specSize == null || specSize.isBlank()) {
            return Result.fail(40001, "SPEC_SIZE_REQUIRED");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            return Result.fail(40001, "FILE_NAME_REQUIRED");
        }

        String ext = extension(originalName);
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.fail(40001, "FILE_TYPE_NOT_ALLOWED");
        }
        if (file.getSize() > 50L * 1024 * 1024) {
            return Result.fail(40001, "FILE_TOO_LARGE");
        }

        String objectName;
        try {
            objectName = uploadToMinio(file, originalName);
        } catch (Exception e) {
            return Result.fail(50001, "MINIO_UPLOAD_FAILED: " + e.getMessage());
        }

        String customerNo = customerDrawingNo != null && !customerDrawingNo.isBlank()
                ? customerDrawingNo.trim()
                : stripExtension(originalName);

        DrawingCreateRequest req = new DrawingCreateRequest();
        req.setDrawingNo(docNoGenerator.nextDrawingNo());
        req.setCustomerDrawingNo(customerNo);
        req.setTitle(title != null && !title.isBlank() ? title.trim() : customerNo);
        req.setMaterialGrade(materialGrade.trim());
        req.setSpecSize(specSize.trim());
        req.setUnitWeight(unitWeight);
        req.setMaterialCode(null);
        req.setProcessRoute("[{\"step\":1,\"name\":\"待定\",\"cost\":0}]");
        req.setPdfPath("minio://" + bucketDrawing + "/" + objectName);
        req.setComment("图纸上传 · 待工程师工程转化后绑定料号");
        req.setIsNew(1);

        Result<CrmDrawing> created = drawingService.createDrawing(req, operatorUserId);
        if (created.getCode() != 0 || created.getData() == null) {
            return created;
        }

        if (releaseAfter) {
            DrawingReleaseRequest releaseReq = new DrawingReleaseRequest();
            Result<CrmDrawing> released = drawingService.releaseDrawing(
                    created.getData().getId(), releaseReq, operatorUserId);
            if (released.getCode() == 0 && released.getData() != null) {
                return released;
            }
        }
        return created;
    }

    private String uploadToMinio(MultipartFile file, String originalName) throws Exception {
        String day = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String safeName = UUID.randomUUID().toString().replace("-", "")
                + "_" + originalName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        String objectName = day + "/" + safeName;

        byte[] bytes = file.getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketDrawing)
                        .object(objectName)
                        .stream(bais, bytes.length, -1)
                        .contentType(file.getContentType())
                        .build()
        );

        return objectName;
    }

    public String getPresignedUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketDrawing)
                        .object(objectName)
                        .expiry(1, TimeUnit.HOURS)
                        .build()
        );
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
