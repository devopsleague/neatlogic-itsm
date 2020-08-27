package codedriver.module.process.api.processtask;

import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.constvalue.ProcessTaskStepAction;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.ProcessTaskStepSubtaskVo;
import codedriver.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import codedriver.framework.process.exception.processtask.ProcessTaskStepSubtaskNotFoundException;
import codedriver.module.process.service.ProcessTaskService;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.UPDATE)
public class ProcessTaskStepSubtaskRedoApi extends PrivateApiComponentBase {
	
	@Autowired
	private ProcessTaskMapper processTaskMapper;

	@Autowired
	private ProcessTaskService processTaskService;

	@Override
	public String getToken() {
		return "processtask/step/subtask/redo";
	}

	@Override
	public String getName() {
		return "子任务打回重做接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name = "processTaskStepSubtaskId", type = ApiParamType.LONG, isRequired = true, desc = "子任务id"),
		@Param(name = "content", type = ApiParamType.STRING, isRequired = true, desc = "描述")
	})
	@Output({})
	@Description(desc = "子任务打回重做接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long processTaskStepSubtaskId = jsonObj.getLong("processTaskStepSubtaskId");
		ProcessTaskStepSubtaskVo processTaskStepSubtaskVo = processTaskMapper.getProcessTaskStepSubtaskById(processTaskStepSubtaskId);
		if(processTaskStepSubtaskVo == null) {
			throw new ProcessTaskStepSubtaskNotFoundException(processTaskStepSubtaskId.toString());
		}
		if(processTaskStepSubtaskVo.getIsRedoable().intValue() == 1) {
			// 锁定当前流程
			processTaskMapper.getProcessTaskLockById(processTaskStepSubtaskVo.getProcessTaskId());
			processTaskStepSubtaskVo.setParamObj(jsonObj);
			processTaskService.redoSubtask(processTaskStepSubtaskVo);
		}else {
			throw new ProcessTaskNoPermissionException(ProcessTaskStepAction.REDOSUBTASK.getText());
		}
		return null;
	}

}
