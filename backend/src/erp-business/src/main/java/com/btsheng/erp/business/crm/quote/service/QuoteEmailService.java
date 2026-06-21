package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.business.crm.sales.entity.CrmCustomer;
import com.btsheng.erp.business.crm.sales.mapper.CrmCustomerMapper;
import com.btsheng.erp.business.integration.client.EmailClient;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QuoteEmailService {

    private final CrmQuoteMapper quoteMapper;
    private final CrmCustomerMapper customerMapper;
    private final PdfExportService exportService;
    private final EmailClient emailClient;

    @Autowired
    public QuoteEmailService(CrmQuoteMapper quoteMapper, CrmCustomerMapper customerMapper,
                             PdfExportService exportService, EmailClient emailClient) {
        this.quoteMapper = quoteMapper;
        this.customerMapper = customerMapper;
        this.exportService = exportService;
        this.emailClient = emailClient;
    }

    @AuditLog(module = "quote", action = "quote.send_email")
    public Result<Map<String, Object>> sendToCustomer(Long quoteId, Long operatorUserId) {
        CrmQuote quote = quoteMapper.selectById(quoteId);
        if (quote == null) {
            return Result.fail(40401, "QUOTE_NOT_FOUND");
        }
        if (!"APPROVED".equals(quote.getStatus())) {
            return Result.fail(40904, "QUOTE_MUST_BE_APPROVED");
        }
        if (quote.getCustomerId() == null) {
            return Result.fail(40001, "CUSTOMER_REQUIRED");
        }
        CrmCustomer customer = customerMapper.selectById(quote.getCustomerId());
        if (customer == null) {
            return Result.fail(40404, "CUSTOMER_NOT_FOUND");
        }
        String email = customer.getContactEmail();
        if (email == null || email.isBlank()) {
            return Result.fail(40001, "CUSTOMER_EMAIL_MISSING");
        }

        Result<byte[]> pdfResult = exportService.exportPdf(quoteId, operatorUserId);
        if (!pdfResult.isSuccess() || pdfResult.getData() == null) {
            return Result.fail(pdfResult.getCode(), pdfResult.getMessage());
        }

        String filename = (quote.getQuoteNo() != null ? quote.getQuoteNo() : "quote-" + quoteId) + ".pdf";
        String subject = "报价单 " + (quote.getQuoteNo() != null ? quote.getQuoteNo() : quoteId);
        String body = "您好，\n\n附件为报价单（" + quote.getQuoteNo() + "），请查收。\n\n昆山佰泰胜精密机械有限公司";

        Map<String, Object> req = new HashMap<>();
        req.put("toAddress", email.trim());
        req.put("subject", subject);
        req.put("body", body);
        req.put("attachmentBase64", Base64.getEncoder().encodeToString(pdfResult.getData()));
        req.put("attachmentFilename", filename);

        Result<Map<String, Object>> sendResult = emailClient.send(req);
        if (!sendResult.isSuccess()) {
            return Result.fail(sendResult.getCode(), sendResult.getMessage());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("toAddress", email.trim());
        data.put("quoteNo", quote.getQuoteNo());
        if (sendResult.getData() != null) {
            data.putAll(sendResult.getData());
        }
        return Result.ok(data);
    }
}
