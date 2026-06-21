package com.btsheng.erp.core.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通用分页响应（V1.3.7）
 *
 * <p>与 OpenAPI {@code PageResponse} schema 对齐：records / total / pageNum / pageSize / pages。
 *
 * @param <T> 记录类型
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Schema(description = "分页响应")
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录列表")
    private List<T> records;

    @Schema(description = "总记录数", example = "100")
    private long total;

    @Schema(description = "页码（1-based）", example = "1")
    private long pageNum;

    @Schema(description = "每页大小", example = "20")
    private long pageSize;

    @Schema(description = "总页数", example = "5")
    private long pages;

    public PageResponse() {
        this.records = Collections.emptyList();
    }

    public PageResponse(List<T> records, long total, long pageNum, long pageSize) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize;
    }

    /**
     * 适配 Spring Data {@link Page} 的便捷构造。
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(),
                page.getNumber() + 1, page.getSize());
    }

    /**
     * 适配 MyBatis-Plus {@code IPage}（兼容 long 类型）。
     */
    public static <T> PageResponse<T> of(List<T> records, long total, long pageNum, long pageSize) {
        return new PageResponse<>(records, total, pageNum, pageSize);
    }

    /**
     * 将当前分页结果映射为另一种类型（如 Entity → DTO）。
     */
    public <R> PageResponse<R> map(Function<T, R> converter) {
        List<R> mapped = this.records.stream().map(converter).collect(Collectors.toList());
        return new PageResponse<>(mapped, this.total, this.pageNum, this.pageSize);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPageNum() {
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        this.pageNum = pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }
}
