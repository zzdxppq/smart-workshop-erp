package com.btsheng.erp.platform.sysparam.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.sysparam.entity.SysParam;
import com.btsheng.erp.platform.sysparam.mapper.SysParamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysParamService {

    private final SysParamMapper paramMapper;

    @Autowired
    public SysParamService(SysParamMapper paramMapper) { this.paramMapper = paramMapper; }

    public Result<List<SysParam>> listByGroup(String group) {
        return Result.ok(paramMapper.selectByGroup(group));
    }

    public Result<SysParam> getByKey(String key) {
        SysParam p = paramMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysParam>().eq("param_key", key));
        if (p == null) return Result.fail(40403, "PARAM_NOT_FOUND");
        return Result.ok(p);
    }

    public Result<SysParam> updateParam(String key, String value, Long operatorUserId) {
        SysParam p = paramMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysParam>().eq("param_key", key));
        if (p == null) return Result.fail(40403, "PARAM_NOT_FOUND");
        p.setParamValue(value);
        p.setUpdatedBy(operatorUserId);
        paramMapper.updateById(p);
        return Result.ok(p);
    }

    public Result<Void> refreshAll() {
        // Nacos 推送：5s 内所有节点生效
            return Result.ok();
    }
}
