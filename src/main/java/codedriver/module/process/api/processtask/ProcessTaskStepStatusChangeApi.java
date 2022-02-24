/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */


package codedriver.module.process.api.processtask;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.exception.user.UserNotFoundException;
import codedriver.framework.process.constvalue.ProcessStepHandlerType;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.constvalue.ProcessTaskStepUserStatus;
import codedriver.framework.process.constvalue.ProcessUserType;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.*;
import codedriver.framework.process.exception.processtask.ProcessTaskStepFoundMultipleException;
import codedriver.framework.process.exception.processtask.ProcessTaskStepNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.OPERATE)
public class ProcessTaskStepStatusChangeApi extends PublicApiComponentBase {

    @Resource
    private ProcessTaskMapper processTaskMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public String getToken() {
        return "processtask/step/status/change";
    }

    @Override
    public String getName() {
        return "手动更改工单步骤状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "processTaskId", type = ApiParamType.LONG, desc = "工单Id"),
            @Param(name = "processTaskStepName", type = ApiParamType.STRING, desc = "工单步骤名称"),
            @Param(name = "processTaskNextStepName", type = ApiParamType.STRING, desc = "需要激活的下一步骤名称(更改步骤状态为succeed时需要填此参数)"),
            @Param(name = "processTaskStepId", type = ApiParamType.LONG, desc = "工单步骤Id(待更改状态的步骤名称重复时需要填此参数)"),
            @Param(name = "processTaskNextStepId", type = ApiParamType.LONG, desc = "工单步骤Id(待激活的下一步骤名称重复时需要填此参数)"),
            @Param(name = "status", type = ApiParamType.ENUM, rule = "pending,running,succeed,hang", isRequired = true, desc = "工单步骤状态"),
            @Param(name = "userId", type = ApiParamType.STRING, desc = "处理人userId"),
    })
    @Description(desc = "手动更改工单步骤状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long processTaskId = jsonObj.getLong("processTaskId");
        String processTaskStepName = jsonObj.getString("processTaskStepName");
        String processTaskNextStepName = jsonObj.getString("processTaskNextStepName");
        Long processTaskStepId = jsonObj.getLong("processTaskStepId");
        Long processTaskNextStepId = jsonObj.getLong("processTaskNextStepId");
        String status = jsonObj.getString("status");
        String userId = jsonObj.getString("userId");
        if (processTaskId == null && processTaskStepId == null) {
            throw new ParamNotExistsException("processTaskId", "processTaskStepId");
        }
        ProcessTaskStepVo processTaskStep;
        if (processTaskId != null) {
            if (StringUtils.isBlank(processTaskStepName)) {
                throw new ParamNotExistsException("processTaskStepName");
            }
            List<ProcessTaskStepVo> stepList = processTaskMapper.getProcessTaskStepByProcessTaskIdAndStepName(new ProcessTaskStepVo(processTaskId, processTaskStepName));
            if (stepList.isEmpty()) {
                throw new ProcessTaskStepNotFoundException(processTaskStepName);
            }
            if (stepList.size() > 1) {
                throw new ProcessTaskStepFoundMultipleException(processTaskStepName);
            }
            processTaskStep = stepList.get(0);
        } else {
            processTaskStep = processTaskMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
            if (processTaskStep == null) {
                throw new ProcessTaskStepNotFoundException(processTaskStepId);
            }
        }
        if (StringUtils.isNotBlank(userId)) {
            UserVo user = userMapper.getUserByUserId(userId);
            if (user == null) {
                throw new UserNotFoundException(userId);
            }
            processTaskStep.setOriginalUserVo(user);
        }
        processTaskStep.setNextStepName(processTaskNextStepName);
        processTaskStep.setNextStepId(processTaskNextStepId);
        processTaskMapper.getProcessTaskLockById(processTaskStep.getProcessTaskId());
        map.get(status).accept(processTaskStep);
        return null;
    }

    static Map<String, Consumer<ProcessTaskStepVo>> map = new HashMap<>();

    @PostConstruct
    private void init() {
        map.put(ProcessTaskStatus.PENDING.getValue(), processTaskStepVo -> {
            if (processTaskStepVo.getOriginalUserVo() == null) {
                throw new ApiRuntimeException("必须指定处理人");
            }
            changeProcessTaskStepStatusToPending(processTaskStepVo);
        });
        map.put(ProcessTaskStatus.RUNNING.getValue(), processTaskStepVo -> {
            if (processTaskStepVo.getOriginalUserVo() == null) {
                // 不指定处理人时，旧处理人必须存在
                List<ProcessTaskStepUserVo> processTaskStepUserList = processTaskMapper.getProcessTaskStepUserByStepId(processTaskStepVo.getId(), ProcessUserType.MAJOR.getValue());
                if (processTaskStepUserList.isEmpty()) {
                    throw new ApiRuntimeException("必须指定处理人");
                }
                ProcessTaskStepUserVo majorUser = processTaskStepUserList.get(0);
                changeProcessTaskStepStatusToRunning(processTaskStepVo, majorUser.getUserUuid(), majorUser.getUserName());
            } else {
                processTaskMapper.deleteProcessTaskStepUser(new ProcessTaskStepUserVo(processTaskStepVo.getId(), ProcessUserType.MAJOR.getValue()));
                processTaskMapper.insertProcessTaskStepUser(new ProcessTaskStepUserVo(
                        processTaskStepVo.getProcessTaskId(),
                        processTaskStepVo.getId(),
                        processTaskStepVo.getOriginalUserVo().getUuid(),
                        ProcessUserType.MAJOR.getValue()
                ));
                changeProcessTaskStepStatusToRunning(processTaskStepVo, processTaskStepVo.getOriginalUserVo().getUuid(), processTaskStepVo.getOriginalUserVo().getUserName());
            }
            processTaskMapper.updateProcessTaskStatus(new ProcessTaskVo(processTaskStepVo.getProcessTaskId(), ProcessTaskStatus.RUNNING.getValue()));
        });
        map.put(ProcessTaskStatus.SUCCEED.getValue(), processTaskStepVo -> {
            if (!ProcessStepHandlerType.END.getHandler().equals(processTaskStepVo.getHandler()) && StringUtils.isBlank(processTaskStepVo.getNextStepName())
                    && processTaskStepVo.getNextStepId() == null) {
                throw new ApiRuntimeException("必须指定需要激活的下一步骤名称或步骤id");
            }
            ProcessTaskStepVo nextStep = null;
            // 检查下一步骤是否合法
            if (StringUtils.isNotBlank(processTaskStepVo.getNextStepName())) {
                List<ProcessTaskStepVo> nextStepList = processTaskMapper.getProcessTaskStepByProcessTaskIdAndStepName(new ProcessTaskStepVo(processTaskStepVo.getProcessTaskId(), processTaskStepVo.getNextStepName()));
                if (nextStepList.isEmpty()) {
                    throw new ProcessTaskStepNotFoundException(processTaskStepVo.getNextStepName());
                }
                if (nextStepList.size() > 1) {
                    throw new ProcessTaskStepFoundMultipleException(processTaskStepVo.getNextStepName());
                }
                List<ProcessTaskStepRelVo> stepRelVoList = processTaskMapper.getProcessTaskStepRelByFromId(processTaskStepVo.getId());
                if (stepRelVoList.stream().noneMatch(o -> Objects.equals(o.getToProcessTaskStepId(), nextStepList.get(0).getId()))) {
                    throw new ApiRuntimeException(processTaskStepVo.getNextStepName() + "不是步骤：" + processTaskStepVo.getName() + "的下一步骤");
                }
                nextStep = nextStepList.get(0);
            } else if (processTaskStepVo.getNextStepId() != null) {
                nextStep = processTaskMapper.getProcessTaskStepBaseInfoById(processTaskStepVo.getNextStepId());
                if (nextStep == null) {
                    throw new ProcessTaskStepNotFoundException(processTaskStepVo.getNextStepId());
                }
                List<ProcessTaskStepRelVo> stepRelVoList = processTaskMapper.getProcessTaskStepRelByFromId(processTaskStepVo.getId());
                if (stepRelVoList.stream().noneMatch(o -> Objects.equals(o.getToProcessTaskStepId(), processTaskStepVo.getNextStepId()))) {
                    throw new ApiRuntimeException(processTaskStepVo.getNextStepId() + "不是步骤：" + processTaskStepVo.getName() + "的下一步骤");
                }
            }
            // 清空当前步骤worker
            processTaskMapper.deleteProcessTaskStepWorker(new ProcessTaskStepWorkerVo(processTaskStepVo.getId(), ProcessUserType.MAJOR.getValue()));
            // 更改当前步骤处理人状态为DONE
            if (processTaskStepVo.getOriginalUserVo() == null) {
                ProcessTaskStepUserVo processTaskStepUserVo = new ProcessTaskStepUserVo(processTaskStepVo.getId(), ProcessUserType.MAJOR.getValue());
                processTaskStepUserVo.setStatus(ProcessTaskStepUserStatus.DONE.getValue());
                processTaskMapper.updateProcessTaskStepUserStatus(processTaskStepUserVo);
            } else {
                processTaskMapper.updateProcessTaskStepMajorUserAndStatus(new ProcessTaskStepUserVo(processTaskStepVo.getId(), processTaskStepVo.getOriginalUserVo().getUuid(), processTaskStepVo.getOriginalUserVo().getUserName(), ProcessTaskStepUserStatus.DONE.getValue()));
            }
            // 更改当前步骤状态为SUCCEED
            processTaskStepVo.setIsActive(2);
            processTaskStepVo.setStatus(ProcessTaskStatus.SUCCEED.getValue());
            processTaskStepVo.setUpdateEndTime(1);
            processTaskMapper.updateProcessTaskStepStatus(processTaskStepVo);
            // 激活与下个节点之间的路径
            if (ProcessStepHandlerType.END.getHandler().equals(processTaskStepVo.getHandler())) {
                processTaskMapper.updateProcessTaskStatus(new ProcessTaskVo(processTaskStepVo.getProcessTaskId(), ProcessTaskStatus.SUCCEED.getValue()));
            } else if (nextStep != null) {
                processTaskMapper.updateProcessTaskStepRelIsHit(new ProcessTaskStepRelVo(processTaskStepVo.getId(), nextStep.getId(), 1));
                processTaskMapper.updateProcessTaskStatus(new ProcessTaskVo(processTaskStepVo.getProcessTaskId(), ProcessTaskStatus.RUNNING.getValue()));
            }
        });
        map.put(ProcessTaskStatus.HANG.getValue(), processTaskStepVo -> {
            ProcessTaskStepUserVo processTaskStepUserVo = new ProcessTaskStepUserVo(processTaskStepVo.getId(), ProcessUserType.MAJOR.getValue());
            processTaskStepUserVo.setStatus(ProcessTaskStepUserStatus.DONE.getValue());
            processTaskMapper.updateProcessTaskStepUserStatus(processTaskStepUserVo);
            processTaskStepVo.setIsActive(0);
            processTaskStepVo.setStatus(ProcessTaskStatus.HANG.name());
            processTaskStepVo.setUpdateEndTime(1);
            processTaskMapper.updateProcessTaskStepStatus(processTaskStepVo);
            processTaskMapper.updateProcessTaskStatus(new ProcessTaskVo(processTaskStepVo.getProcessTaskId(), ProcessTaskStatus.HANG.getValue()));
        });
    }

    /**
     * 更改步骤状态为待处理
     *
     * @param processTaskStepVo
     */
    private void changeProcessTaskStepStatusToPending(ProcessTaskStepVo processTaskStepVo) {
        processTaskMapper.deleteProcessTaskStepUser(new ProcessTaskStepUserVo(processTaskStepVo.getId(), ProcessUserType.MAJOR.getValue()));
        processTaskMapper.deleteProcessTaskStepWorker(new ProcessTaskStepWorkerVo(processTaskStepVo.getId(), ProcessUserType.MAJOR.getValue()));
        processTaskMapper.insertIgnoreProcessTaskStepWorker(new ProcessTaskStepWorkerVo(processTaskStepVo.getProcessTaskId(), processTaskStepVo.getId()
                , GroupSearch.USER.getValue(), processTaskStepVo.getOriginalUserVo().getUuid(), ProcessUserType.MAJOR.getValue()));
        processTaskMapper.updateProcessTaskStepStatusByStepId(new ProcessTaskStepVo(processTaskStepVo.getId(), ProcessTaskStatus.PENDING.getValue(), 1));
        processTaskMapper.updateProcessTaskStatus(new ProcessTaskVo(processTaskStepVo.getProcessTaskId(), ProcessTaskStatus.RUNNING.getValue()));
    }

    /**
     * 更改步骤状态为处理中
     *
     * @param processTaskStep 步骤
     * @param userUuid        处理人uuid
     * @param userName        处理人userName
     */
    private void changeProcessTaskStepStatusToRunning(ProcessTaskStepVo processTaskStep, String userUuid, String userName) {
        processTaskMapper.deleteProcessTaskStepWorker(new ProcessTaskStepWorkerVo(processTaskStep.getId(), ProcessUserType.MAJOR.getValue()));
        processTaskMapper.insertIgnoreProcessTaskStepWorker(new ProcessTaskStepWorkerVo(processTaskStep.getProcessTaskId(), processTaskStep.getId(), GroupSearch.USER.getValue(), userUuid, ProcessUserType.MAJOR.getValue()));
        processTaskMapper.updateProcessTaskStepStatusByStepId(new ProcessTaskStepVo(processTaskStep.getId(), ProcessTaskStatus.RUNNING.getValue(), 1));
        processTaskMapper.updateProcessTaskStepMajorUserAndStatus(new ProcessTaskStepUserVo(processTaskStep.getId(), userUuid, userName, ProcessTaskStepUserStatus.DOING.getValue()));
    }

}
