package com.btsheng.erp.business.crm.vendor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btsheng.erp.business.crm.vendor.dto.CreateVendorRequest;
import com.btsheng.erp.business.crm.vendor.dto.UpdateVendorNotifyRequest;
import com.btsheng.erp.business.crm.vendor.dto.UpdateVendorRequest;
import com.btsheng.erp.business.crm.vendor.entity.OutsubVendor;
import com.btsheng.erp.business.crm.vendor.mapper.OutsubVendorMapper;
import com.btsheng.erp.core.model.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.web.multipart.MultipartFile;

@Service
public class VendorService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final String NOTIFY_CHANNEL = "email_163";

    private final OutsubVendorMapper vendorMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public VendorService(OutsubVendorMapper vendorMapper, ObjectMapper objectMapper) {
        this.vendorMapper = vendorMapper;
        this.objectMapper = objectMapper;
    }

    public Result<Map<String, Object>> list(String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<OutsubVendor> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(OutsubVendor::getVendorName, keyword)
                    .or().like(OutsubVendor::getVendorCode, keyword));
        }
        qw.orderByDesc(OutsubVendor::getId);
        List<OutsubVendor> all = vendorMapper.selectList(qw);

        int size = pageSize > 0 ? pageSize : 20;
        int page = Math.max(pageNum, 1);
        int from = (page - 1) * size;
        int to = Math.min(from + size, all.size());
        List<Map<String, Object>> items = new ArrayList<>();
        if (from < all.size()) {
            for (OutsubVendor v : all.subList(from, to)) {
                items.add(toVo(v));
            }
        }
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("items", items);
        pageData.put("records", items);
        pageData.put("total", all.size());
        pageData.put("pageNum", page);
        pageData.put("pageSize", size);
        return Result.ok(pageData);
    }

    public Result<Map<String, Object>> getDetail(Long id) {
        OutsubVendor v = vendorMapper.selectById(id);
        if (v == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "VENDOR_NOT_FOUND");
        }
        return Result.ok(toVo(v));
    }

    @Transactional
    public Result<Map<String, Object>> create(CreateVendorRequest req) {
        String err = validateCreate(req);
        if (err != null) {
            return Result.fail(Result.CODE_PARAM_FORMAT, err);
        }
        OutsubVendor v = new OutsubVendor();
        v.setVendorCode(nextVendorCode());
        v.setVendorName(req.getVendorName().trim());
        v.setContactName(req.getContactName().trim());
        v.setContactPhone(blankToNull(req.getContactPhone()));
        v.setContactEmail(req.getContactEmail().trim());
        v.setDefaultReconEmail(blankToNull(req.getDefaultReconEmail()));
        if (v.getDefaultReconEmail() == null) {
            v.setDefaultReconEmail(v.getContactEmail());
        }
        v.setCapabilitiesJson(toCapabilitiesJson(req.getCapabilities()));
        v.setCreditLevel(normalizeCredit(req.getCreditLevel()));
        v.setNotifyChannel(NOTIFY_CHANNEL);
        v.setStatus("ACTIVE");
        v.setCreatedAt(LocalDateTime.now());
        v.setUpdatedAt(LocalDateTime.now());
        vendorMapper.insert(v);
        return Result.ok(toVo(v));
    }

    @Transactional
    public Result<Map<String, Object>> update(Long id, UpdateVendorRequest req) {
        OutsubVendor v = vendorMapper.selectById(id);
        if (v == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "VENDOR_NOT_FOUND");
        }
        String err = validateUpdate(req);
        if (err != null) {
            return Result.fail(Result.CODE_PARAM_FORMAT, err);
        }
        if (req.getVendorName() != null && !req.getVendorName().isBlank()) {
            v.setVendorName(req.getVendorName().trim());
        }
        if (req.getContactName() != null && !req.getContactName().isBlank()) {
            v.setContactName(req.getContactName().trim());
        }
        if (req.getContactPhone() != null) {
            v.setContactPhone(blankToNull(req.getContactPhone()));
        }
        if (req.getContactEmail() != null && !req.getContactEmail().isBlank()) {
            v.setContactEmail(req.getContactEmail().trim());
        }
        if (req.getDefaultReconEmail() != null) {
            v.setDefaultReconEmail(blankToNull(req.getDefaultReconEmail()));
        }
        if (req.getCapabilities() != null) {
            v.setCapabilitiesJson(toCapabilitiesJson(req.getCapabilities()));
        }
        if (req.getCreditLevel() != null && !req.getCreditLevel().isBlank()) {
            v.setCreditLevel(normalizeCredit(req.getCreditLevel()));
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            v.setStatus(req.getStatus().trim());
        }
        v.setNotifyChannel(NOTIFY_CHANNEL);
        v.setUpdatedAt(LocalDateTime.now());
        vendorMapper.updateById(v);
        return Result.ok(toVo(v));
    }

    @Transactional
    public Result<Map<String, Object>> updateNotifyPref(Long id, UpdateVendorNotifyRequest req) {
        if (req == null || req.getNotifyEmail() == null || req.getNotifyEmail().isBlank()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "接收邮箱必填");
        }
        String email = req.getNotifyEmail().trim();
        if (!isValidEmail(email)) {
            return Result.fail(Result.CODE_PARAM_FORMAT, "邮箱格式错误，请检查");
        }
        OutsubVendor v = vendorMapper.selectById(id);
        if (v == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "VENDOR_NOT_FOUND");
        }
        v.setContactEmail(email);
        if (req.getDefaultReconEmail() != null && !req.getDefaultReconEmail().isBlank()) {
            String recon = req.getDefaultReconEmail().trim();
            if (!isValidEmail(recon)) {
                return Result.fail(Result.CODE_PARAM_FORMAT, "对账邮箱格式错误，请检查");
            }
            v.setDefaultReconEmail(recon);
        }
        v.setNotifyChannel(NOTIFY_CHANNEL);
        v.setUpdatedAt(LocalDateTime.now());
        vendorMapper.updateById(v);
        return Result.ok(toVo(v));
    }

    private String validateCreate(CreateVendorRequest req) {
        if (req == null) {
            return "请求体不能为空";
        }
        if (req.getVendorName() == null || req.getVendorName().isBlank()) {
            return "厂商名称必填";
        }
        if (req.getContactName() == null || req.getContactName().isBlank()) {
            return "联系人必填";
        }
        if (req.getContactEmail() == null || req.getContactEmail().isBlank()) {
            return "V1.3.7：接收邮箱为必填项（用于接收委外/对账通知）";
        }
        if (!isValidEmail(req.getContactEmail().trim())) {
            return "邮箱格式错误，请检查";
        }
        if (req.getDefaultReconEmail() != null && !req.getDefaultReconEmail().isBlank()
                && !isValidEmail(req.getDefaultReconEmail().trim())) {
            return "对账邮箱格式错误，请检查";
        }
        if (req.getContactPhone() != null && !req.getContactPhone().isBlank()
                && !PHONE_PATTERN.matcher(req.getContactPhone().trim()).matches()) {
            return "联系电话格式错误，应为 11 位手机号";
        }
        if (req.getCapabilities() == null || req.getCapabilities().isEmpty()) {
            return "加工能力分类必填";
        }
        return null;
    }

    private String validateUpdate(UpdateVendorRequest req) {
        if (req == null) {
            return "请求体不能为空";
        }
        if (req.getContactEmail() != null && !req.getContactEmail().isBlank()
                && !isValidEmail(req.getContactEmail().trim())) {
            return "邮箱格式错误，请检查";
        }
        if (req.getDefaultReconEmail() != null && !req.getDefaultReconEmail().isBlank()
                && !isValidEmail(req.getDefaultReconEmail().trim())) {
            return "对账邮箱格式错误，请检查";
        }
        if (req.getContactPhone() != null && !req.getContactPhone().isBlank()
                && !PHONE_PATTERN.matcher(req.getContactPhone().trim()).matches()) {
            return "联系电话格式错误，应为 11 位手机号";
        }
        return null;
    }

    private static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String normalizeCredit(String level) {
        if (level == null || level.isBlank()) {
            return "C";
        }
        return level.trim().toUpperCase();
    }

    private String toCapabilitiesJson(List<String> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(capabilities);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String nextVendorCode() {
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "VS" + day;
        OutsubVendor last = vendorMapper.selectOne(
                new LambdaQueryWrapper<OutsubVendor>()
                        .likeRight(OutsubVendor::getVendorCode, prefix)
                        .orderByDesc(OutsubVendor::getVendorCode)
                        .last("LIMIT 1"));
        int seq = 1;
        if (last != null && last.getVendorCode() != null && last.getVendorCode().length() > prefix.length()) {
            try {
                seq = Integer.parseInt(last.getVendorCode().substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + String.format("%03d", seq);
    }

    private Map<String, Object> toVo(OutsubVendor v) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", v.getId());
        m.put("vendorCode", v.getVendorCode());
        m.put("vendorName", v.getVendorName());
        m.put("contact", v.getContactName());
        m.put("contactName", v.getContactName());
        m.put("phone", v.getContactPhone());
        m.put("contactPhone", v.getContactPhone());
        m.put("notifyEmail", v.getContactEmail());
        m.put("contactEmail", v.getContactEmail());
        m.put("defaultReconEmail", v.getDefaultReconEmail());
        m.put("businessLicenseUrl", v.getBusinessLicenseUrl());
        m.put("businessLicenseExpireDate", v.getBusinessLicenseExpireDate());
        m.put("notifyChannel", v.getNotifyChannel());
        m.put("rating", v.getCreditLevel());
        m.put("creditLevel", v.getCreditLevel());
        m.put("capabilitiesJson", v.getCapabilitiesJson());
        m.put("status", v.getStatus());
        m.put("emailMissing", v.getContactEmail() == null || v.getContactEmail().isBlank());
        return m;
    }

    /**
     * V2.2 上传营业执照
     */
    @Transactional
    public Result<Map<String, Object>> uploadBusinessLicense(Long vendorId, MultipartFile file) {
        if (vendorId == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "VENDOR_ID_REQUIRED");
        }
        if (file == null || file.isEmpty()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "FILE_REQUIRED");
        }
        OutsubVendor v = vendorMapper.selectById(vendorId);
        if (v == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "VENDOR_NOT_FOUND");
        }
        // 简单实现：保存文件到本地 uploads 目录
        String filename = "license_" + vendorId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String uploadDir = System.getProperty("user.dir") + "/uploads/licenses/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            java.io.File dest = new java.io.File(uploadDir + filename);
            file.transferTo(dest);
            String url = "/uploads/licenses/" + filename;
            v.setBusinessLicenseUrl(url);
            v.setUpdatedAt(LocalDateTime.now());
            vendorMapper.updateById(v);
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", filename);
            return Result.ok(result);
        } catch (java.io.IOException e) {
            return Result.fail(Result.CODE_SYSTEM, "UPLOAD_FAILED");
        }
    }
}
