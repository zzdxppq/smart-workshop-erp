package com.btsheng.erp.business.crm.hr.performance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "招聘录入请求")
public class RecruitmentRequest {
    @Schema(description = "候选人姓名") private String candidateName;
    @Schema(description = "岗位") private String position;
    @Schema(description = "部门") private String department;
    @Schema(description = "电话") private String phone;
    @Schema(description = "邮箱") private String email;
}
