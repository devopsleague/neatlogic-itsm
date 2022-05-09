/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.api.workcenter;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.auth.PROCESS_BASE;
import codedriver.framework.process.auth.WORKCENTER_MODIFY;
import codedriver.framework.process.constvalue.ProcessWorkcenterType;
import codedriver.framework.process.dao.mapper.workcenter.WorkcenterMapper;
import codedriver.framework.process.exception.workcenter.WorkcenterNoAuthException;
import codedriver.framework.process.exception.workcenter.WorkcenterNotFoundException;
import codedriver.framework.process.exception.workcenter.WorkcenterParamException;
import codedriver.framework.process.workcenter.dto.WorkcenterAuthorityVo;
import codedriver.framework.process.workcenter.dto.WorkcenterCatalogVo;
import codedriver.framework.process.workcenter.dto.WorkcenterVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Transactional
@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class WorkcenterSaveApi extends PrivateApiComponentBase {

    @Resource
    WorkcenterMapper workcenterMapper;

    @Override
    public String getToken() {
        return "workcenter/save";
    }

    @Override
    public String getName() {
        return "工单中心分类保存接口";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "uuid", type = ApiParamType.STRING, desc = "分类uuid"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", desc = "分类名", xss = true),
            @Param(name = "type", type = ApiParamType.STRING, desc = "分类类型，system|custom 默认custom"),
            @Param(name = "catalogName", type = ApiParamType.STRING, desc = "菜单分类"),
            @Param(name = "support", type = ApiParamType.ENUM, rule = "all,mobile,pc", desc = "使用范围，all|pc|mobile，默认值是：all"),
            @Param(name = "conditionConfig", type = ApiParamType.JSONOBJECT, desc = "分类过滤配置，json格式", isRequired = true),
            @Param(name = "authList", type = ApiParamType.JSONARRAY, desc = "授权列表，如果type是system,则必填")
    })
    @Output({@Param(type = ApiParamType.STRING, desc = "分类uuid")})
    @Description(desc = "工单中心分类新增接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        WorkcenterVo workcenterVo = JSONObject.toJavaObject(jsonObj, WorkcenterVo.class);
        String uuid = jsonObj.getString("uuid");
        WorkcenterVo oldWorkcenterVo = null;
        if (StringUtils.isNotBlank(uuid)) {
            oldWorkcenterVo = workcenterMapper.getWorkcenterByUuid(uuid);
            if (oldWorkcenterVo != null) {
                if (Objects.equals(oldWorkcenterVo.getType(), ProcessWorkcenterType.FACTORY.getValue())) {//如果是出厂类型，则不允许修改类型
                    workcenterVo.setType(ProcessWorkcenterType.FACTORY.getValue());
                }
            } else {
                throw new WorkcenterNotFoundException(uuid);
            }
        }
        Set<String> systemAuthSet = new HashSet<String>() {{
            this.add(ProcessWorkcenterType.FACTORY.getValue());
            this.add(ProcessWorkcenterType.SYSTEM.getValue());
        }};
        if (systemAuthSet.contains(workcenterVo.getType()) || (oldWorkcenterVo != null && systemAuthSet.contains(oldWorkcenterVo.getType()))) {
            //判断是否有管理员权限
            if (!AuthActionChecker.check(WORKCENTER_MODIFY.class.getSimpleName())) {
                throw new WorkcenterNoAuthException("管理");
            }
            workcenterMapper.deleteWorkcenterAuthorityByUuid(workcenterVo.getUuid());
        }
        if (systemAuthSet.contains(workcenterVo.getType())) {
            if (CollectionUtils.isEmpty(workcenterVo.getAuthList())) {
                throw new WorkcenterParamException("valueList");
            }
            //更新角色
            for (String value : workcenterVo.getAuthList()) {
                WorkcenterAuthorityVo authorityVo = new WorkcenterAuthorityVo(value);
                authorityVo.setWorkcenterUuid(workcenterVo.getUuid());
                workcenterMapper.insertWorkcenterAuthority(authorityVo);
            }
        } else {
            if (StringUtils.isBlank(uuid)) {
                workcenterMapper.insertWorkcenterOwner(UserContext.get().getUserUuid(true), workcenterVo.getUuid());
            }
        }
        if (StringUtils.isNotBlank(workcenterVo.getCatalogName())) {
            WorkcenterCatalogVo workcenterCatalogVo = workcenterMapper.getWorkcenterCatalogByName(workcenterVo.getCatalogName());
            if (workcenterCatalogVo == null) {
                workcenterCatalogVo = new WorkcenterCatalogVo();
                workcenterCatalogVo.setName(workcenterVo.getCatalogName());
                workcenterMapper.insertWorkcenterCatalog(workcenterCatalogVo);
            }
            workcenterVo.setCatalogId(workcenterCatalogVo.getId());
        }
        if (StringUtils.isBlank(uuid)) {
            workcenterMapper.insertWorkcenter(workcenterVo);
        } else {
            workcenterMapper.updateWorkcenter(workcenterVo);
        }
        return workcenterVo.getUuid();
    }

}