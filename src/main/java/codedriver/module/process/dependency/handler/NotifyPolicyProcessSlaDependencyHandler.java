/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.dependency.handler;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.dependency.constvalue.CalleeType;
import codedriver.framework.dependency.core.DependencyHandlerBase;
import codedriver.framework.dependency.core.ICalleeType;
import codedriver.framework.process.dao.mapper.ProcessMapper;
import codedriver.framework.process.dto.ProcessSlaVo;
import codedriver.framework.process.dto.ProcessVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 流程时效引用通知策略处理器
 *
 * @author: linbq
 * @since: 2021/4/5 14:31
 **/
@Service
public class NotifyPolicyProcessSlaDependencyHandler extends DependencyHandlerBase {
    @Resource
    private ProcessMapper processMapper;

    /**
     * 表名
     *
     * @return
     */
    @Override
    protected String getTableName() {
        return "process_sla_notify_policy";
    }

    /**
     * 被调用者字段
     *
     * @return
     */
    @Override
    protected String getCalleeField() {
        return "notify_policy_id";
    }

    /**
     * 调用者字段
     *
     * @return
     */
    @Override
    protected String getCallerField() {
        return "sla_uuid";
    }

    /**
     * 解析数据，拼装跳转url，返回引用下拉列表一个选项数据结构
     *
     * @param caller 调用者值
     * @return
     */
    @Override
    protected ValueTextVo parse(Object caller) {
        if (caller instanceof String) {
            ProcessSlaVo processSlaVo = processMapper.getProcessSlaByUuid((String) caller);
            if (processSlaVo != null) {
                ProcessVo processVo = processMapper.getProcessByUuid(processSlaVo.getProcessUuid());
                if (processVo != null) {
                    ValueTextVo valueTextVo = new ValueTextVo();
                    valueTextVo.setValue(caller);
                    valueTextVo.setText(String.format("<a href=\"/%s/process.html#/flow-edit?uuid=%s\" target=\"_blank\">%s-%s</a>", TenantContext.get().getTenantUuid(), processVo.getUuid(), processVo.getName(), processSlaVo.getName()));
                    return valueTextVo;
                }
            }
        }
        return null;
    }

    /**
     * 被调用方名
     *
     * @return
     */
    @Override
    public ICalleeType getCalleeType() {
        return CalleeType.NOTIFY_POLICY;
    }
}
