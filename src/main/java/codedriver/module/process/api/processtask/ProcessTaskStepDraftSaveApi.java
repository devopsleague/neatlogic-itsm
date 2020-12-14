package codedriver.module.process.api.processtask;

import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.process.constvalue.ProcessTaskOperationType;
import codedriver.framework.process.constvalue.ProcessTaskStepDataType;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dao.mapper.ProcessTaskStepDataMapper;
import codedriver.framework.process.dto.ProcessTaskStepDataVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.exception.process.ProcessStepUtilHandlerNotFoundException;
import codedriver.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import codedriver.framework.process.operationauth.core.ProcessOperateManager;
import codedriver.framework.process.stephandler.core.IProcessStepUtilHandler;
import codedriver.framework.process.stephandler.core.ProcessStepUtilHandlerFactory;
import codedriver.module.process.service.ProcessTaskService;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
public class ProcessTaskStepDraftSaveApi extends PrivateApiComponentBase {

    @Autowired
    private ProcessTaskMapper processTaskMapper;

    @Autowired
    private ProcessTaskService processTaskService;

    @Autowired
    private ProcessTaskStepDataMapper processTaskStepDataMapper;

    @Override
    public String getToken() {
        return "processtask/step/draft/save";
    }

    @Override
    public String getName() {
        return "工单步骤暂存接口";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "工单id"),
        @Param(name = "processTaskStepId", type = ApiParamType.LONG, isRequired = true, desc = "步骤id"),
        @Param(name = "priorityUuid", type = ApiParamType.STRING, desc = "优先级uuid"),
        @Param(name = "formAttributeDataList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "表单属性数据列表"),
        @Param(name = "hidecomponentList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "隐藏表单属性列表"),
        @Param(name = "readcomponentList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "只读表单属性列表"),
        @Param(name = "content", type = ApiParamType.STRING, desc = "描述"),
        @Param(name = "fileIdList", type = ApiParamType.JSONARRAY, desc = "附件id列表"),
        @Param(name = "handlerStepInfo", type = ApiParamType.JSONOBJECT, desc = "处理器特有的步骤信息")})
    @Output({@Param(name = "auditId", type = ApiParamType.LONG, desc = "活动id")})
    @Description(desc = "工单步骤暂存接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long processTaskId = jsonObj.getLong("processTaskId");
        Long processTaskStepId = jsonObj.getLong("processTaskStepId");
        ProcessTaskVo processTaskVo =
            processTaskService.checkProcessTaskParamsIsLegal(processTaskId, processTaskStepId);
        // 锁定当前流程
        processTaskMapper.getProcessTaskLockById(processTaskId);
        ProcessTaskStepVo processTaskStepVo = processTaskVo.getCurrentProcessTaskStep();
        IProcessStepUtilHandler handler = ProcessStepUtilHandlerFactory.getHandler(processTaskStepVo.getHandler());
        if (handler == null) {
            throw new ProcessStepUtilHandlerNotFoundException(processTaskStepVo.getHandler());
        }
        try {
            new ProcessOperateManager.StepOperationChecker(processTaskStepId, ProcessTaskOperationType.SAVE).build()
                .checkAndNoPermissionThrowException();
        } catch (ProcessTaskNoPermissionException e) {
            throw new PermissionDeniedException();
        }
        ProcessTaskStepDataVo processTaskStepDataVo = new ProcessTaskStepDataVo(true);
        processTaskStepDataVo.setProcessTaskId(processTaskId);
        processTaskStepDataVo.setProcessTaskStepId(processTaskStepId);
        processTaskStepDataVo.setFcu(UserContext.get().getUserUuid(true));
        processTaskStepDataVo.setType(ProcessTaskStepDataType.STEPDRAFTSAVE.getValue());
        processTaskStepDataMapper.deleteProcessTaskStepData(processTaskStepDataVo);
        processTaskStepDataVo.setData(jsonObj.toJSONString());
        processTaskStepDataMapper.replaceProcessTaskStepData(processTaskStepDataVo);
        return null;
    }

}
