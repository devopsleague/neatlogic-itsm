/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.api.processtask.agent;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.process.auth.PROCESS_BASE;
import codedriver.framework.process.dao.mapper.ProcessTaskAgentMapper;
import codedriver.framework.process.dto.agent.ProcessTaskAgentCompobVo;
import codedriver.framework.process.dto.agent.ProcessTaskAgentInfoVo;
import codedriver.framework.process.dto.agent.ProcessTaskAgentTargetVo;
import codedriver.framework.process.dto.agent.ProcessTaskAgentVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ProcessTaskAgentGetApi extends PrivateApiComponentBase {

    @Resource
    private ProcessTaskAgentMapper processTaskAgentMapper;

    @Override
    public String getToken() {
        return "processtask/agent/get";
    }

    @Override
    public String getName() {
        return "获取用户任务授权信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Output({
            @Param(explode = ProcessTaskAgentInfoVo.class, desc = "任务授权信息")
    })
    @Description(desc = "获取用户任务授权信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String fromUserUuid = UserContext.get().getUserUuid(true);
        List<ProcessTaskAgentVo> processTaskAgentList = processTaskAgentMapper.getProcessTaskAgentListByFromUserUuid(fromUserUuid);
        if (CollectionUtils.isNotEmpty(processTaskAgentList)) {
            List<ProcessTaskAgentCompobVo> combopList = new ArrayList<>();
            for (ProcessTaskAgentVo processTaskAgentVo : processTaskAgentList) {
//                List<String> targetList = new ArrayList<>();
                List<ProcessTaskAgentTargetVo> processTaskAgentTargetList = processTaskAgentMapper.getProcessTaskAgentTargetListByProcessTaskAgentId(processTaskAgentVo.getId());
//                for (ProcessTaskAgentTargetVo processTaskAgentTargetVo : processTaskAgentTargetList) {
//                    targetList.add(processTaskAgentTargetVo.getType() + "#" + processTaskAgentTargetVo.getTarget());
//                }
                ProcessTaskAgentCompobVo processTaskAgentCompobVo = new ProcessTaskAgentCompobVo();
                processTaskAgentCompobVo.setToUserUuid(GroupSearch.USER.getValuePlugin() + processTaskAgentVo.getToUserUuid());
                processTaskAgentCompobVo.setTargetList(processTaskAgentTargetList);
                combopList.add(processTaskAgentCompobVo);
            }
            ProcessTaskAgentVo processTaskAgentVo = processTaskAgentList.get(0);
            ProcessTaskAgentInfoVo processTaskAgentInfoVo = new ProcessTaskAgentInfoVo();
            processTaskAgentInfoVo.setBeginTime(processTaskAgentVo.getBeginTime());
            processTaskAgentInfoVo.setEndTime(processTaskAgentVo.getEndTime());
            processTaskAgentInfoVo.setIsActive(processTaskAgentVo.getIsActive());
            processTaskAgentInfoVo.setCompobList(combopList);
            return processTaskAgentInfoVo;
        }
        return null;
    }
}