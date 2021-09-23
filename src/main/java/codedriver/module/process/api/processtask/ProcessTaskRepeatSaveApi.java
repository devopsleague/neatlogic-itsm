/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.api.processtask;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.process.auth.PROCESS_BASE;
import codedriver.framework.process.constvalue.ProcessTaskAuditType;
import codedriver.framework.process.constvalue.ProcessTaskOperationType;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.ProcessTaskRepeatVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import codedriver.framework.process.exception.processtask.ProcessTaskNotFoundException;
import codedriver.framework.process.notify.constvalue.TaskNotifyTriggerType;
import codedriver.framework.process.operationauth.core.ProcessAuthManager;
import codedriver.framework.process.service.ProcessTaskService;
import codedriver.framework.process.stephandler.core.IProcessStepHandlerUtil;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerFactory;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.SnowflakeUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author linbq
 * @since 2021/9/13 14:46
 **/
@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ProcessTaskRepeatSaveApi extends PrivateApiComponentBase {

    @Resource
    private ProcessTaskService processTaskService;

    @Resource
    private ProcessTaskMapper processTaskMapper;

    @Resource
    private IProcessStepHandlerUtil processStepHandlerUtil;

    @Override
    public String getToken() {
        return "processtask/repeat/save";
    }

    @Override
    public String getName() {
        return "标记重复工单接口";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "工单id"),
            @Param(name = "repeatProcessTaskIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "重复工单id列表")
    })
    @Output({
            @Param(explode = ProcessTaskVo[].class, desc = "工单列表")
    })
    @Description(desc = "标记重复工单接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long processTaskId = paramObj.getLong("processTaskId");
        processTaskService.checkProcessTaskParamsIsLegal(processTaskId);
        try {
            new ProcessAuthManager.TaskOperationChecker(processTaskId, ProcessTaskOperationType.PROCESSTASK_MARKREPEAT)
                    .build().checkAndNoPermissionThrowException();
        } catch (ProcessTaskNoPermissionException e) {
            throw new PermissionDeniedException();
        }
        JSONArray repeatProcessTaskIdArray = paramObj.getJSONArray("repeatProcessTaskIdList");
        if (CollectionUtils.isEmpty(repeatProcessTaskIdArray)) {
            throw new ParamNotExistsException("repeatProcessTaskIdList");
        }
        List<Long> repeatProcessTaskIdList = repeatProcessTaskIdArray.toJavaList(Long.class);
        Set<Long> repeatProcessTaskIdSet = new HashSet<>(repeatProcessTaskIdList);
        repeatProcessTaskIdList = new ArrayList<>(repeatProcessTaskIdSet);
        List<Long> processTaskIdList = processTaskMapper.checkProcessTaskIdListIsExists(repeatProcessTaskIdList);
        if (processTaskIdList.size() < repeatProcessTaskIdList.size()) {
            repeatProcessTaskIdList.removeAll(processTaskIdList);
            List<String> processTaskIdStrList = new ArrayList<>();
            for (Long id : repeatProcessTaskIdList) {
                processTaskIdStrList.add(id.toString());
            }
            throw new ProcessTaskNotFoundException(String.join("、",processTaskIdStrList));
        }
        List<Long> markedprocessTaskIdList = new ArrayList<>();
        Long repeatGroupId = processTaskMapper.getRepeatGroupIdByProcessTaskId(processTaskId);
        if (repeatGroupId != null) {
            markedprocessTaskIdList = processTaskMapper.getProcessTaskIdListByRepeatGroupId(repeatGroupId);
            repeatProcessTaskIdList.removeAll(markedprocessTaskIdList);
        } else {
            repeatGroupId = SnowflakeUtil.uniqueLong();
        }

        if (CollectionUtils.isEmpty(repeatProcessTaskIdList)) {
            return null;
        }
        Set<Long> allRepeatProcessTaskIdSet = new HashSet<>();
        for (Long repeatProcessTaskId : repeatProcessTaskIdList) {
            getRepeatProcessTaskList(repeatProcessTaskId, allRepeatProcessTaskIdSet);
        }
        List<Long> allRepeatProcessTaskIdList = new ArrayList<>(allRepeatProcessTaskIdSet);
        allRepeatProcessTaskIdList.removeAll(markedprocessTaskIdList);
        if (CollectionUtils.isEmpty(allRepeatProcessTaskIdList)) {
            return null;
        }
        List<Long> removeAll = ListUtils.removeAll(allRepeatProcessTaskIdList, repeatProcessTaskIdList);
        List<ProcessTaskVo> processTaskList = processTaskMapper.getProcessTaskListByIdList(allRepeatProcessTaskIdList);
        if (CollectionUtils.isNotEmpty(removeAll)) {
            return processTaskList;
        }
        List<ProcessTaskVo> runningProcessTaskList = new ArrayList<>();
        List<ProcessTaskRepeatVo> processTaskRepeatList = new ArrayList<>();
        for (ProcessTaskVo processTaskVo : processTaskList) {
            //1. 如果工单不是取消状态，则取消工单
            if (ProcessTaskStatus.RUNNING.getValue().equals(processTaskVo.getStatus())) {
                runningProcessTaskList.add(processTaskVo);
            }
            processTaskRepeatList.add(new ProcessTaskRepeatVo(processTaskVo.getId(), repeatGroupId));
            if (processTaskRepeatList.size() >= 1000) {
                processTaskMapper.replaceProcessTaskRepeatList(processTaskRepeatList);
                processTaskRepeatList.clear();
            }
            ProcessTaskStepVo processTaskStepVo = new ProcessTaskStepVo();
            processTaskStepVo.setProcessTaskId(processTaskVo.getId());
            processStepHandlerUtil.audit(processTaskStepVo, ProcessTaskAuditType.BINDREPEAT);
        }
        if (CollectionUtils.isNotEmpty(processTaskRepeatList)) {
            processTaskMapper.replaceProcessTaskRepeatList(processTaskRepeatList);
        }
        processTaskMapper.replaceProcessTaskRepeat(new ProcessTaskRepeatVo(processTaskId, repeatGroupId));
        ProcessTaskStepVo processTaskStepVo = new ProcessTaskStepVo();
        processTaskStepVo.setProcessTaskId(processTaskId);
        processStepHandlerUtil.audit(processTaskStepVo, ProcessTaskAuditType.BINDREPEAT);

        for (ProcessTaskVo processTaskVo : runningProcessTaskList) {
            //当前用户可能没有工单的取消权限，所以用系统用户操作
            UserContext.init(SystemUser.SYSTEM.getUserVo(), SystemUser.SYSTEM.getTimezone());
            ProcessStepHandlerFactory.getHandler().abortProcessTask(processTaskVo);
            ProcessTaskStepVo processTaskStep = new ProcessTaskStepVo();
            processTaskStep.setProcessTaskId(processTaskVo.getId());
            processStepHandlerUtil.notify(processTaskStep, TaskNotifyTriggerType.MARKREPEATPROCESSTASK);
        }
        return null;
    }

    private void getRepeatProcessTaskList(Long processTaskId, Set<Long> resultSet) {
        resultSet.add(processTaskId);
        //2. 如果工单在另一个重复工单组A中，则把工单组A的所有工单加到新的重复工单组B
        Long repeatGroupId = processTaskMapper.getRepeatGroupIdByProcessTaskId(processTaskId);
        if (repeatGroupId != null) {
            List<Long> repeatProcessTaskIdList = processTaskMapper.getProcessTaskIdListByRepeatGroupId(repeatGroupId);
            for (Long repeatProcessTaskId : repeatProcessTaskIdList) {
                if (resultSet.contains(repeatProcessTaskId)) {
                    continue;
                }
                getRepeatProcessTaskList(repeatProcessTaskId, resultSet);
            }
        }
    }
}