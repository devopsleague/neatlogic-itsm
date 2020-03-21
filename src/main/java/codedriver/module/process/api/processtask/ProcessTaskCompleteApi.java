package codedriver.module.process.api.processtask;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.apiparam.core.ApiParamType;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.exception.core.ProcessTaskRuntimeException;
import codedriver.framework.process.exception.process.ProcessStepHandlerNotFoundException;
import codedriver.framework.process.exception.processtask.ProcessTaskNoPermissionException;
import codedriver.framework.process.exception.processtask.ProcessTaskStepNotFoundException;
import codedriver.framework.process.stephandler.core.IProcessStepHandler;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerFactory;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.process.constvalue.ProcessStepType;
import codedriver.module.process.constvalue.ProcessTaskStepAction;
import codedriver.module.process.dto.ProcessTaskStepVo;
import codedriver.module.process.service.ProcessTaskService;

@Service
public class ProcessTaskCompleteApi extends ApiComponentBase {

	@Autowired
	private ProcessTaskMapper processTaskMapper;
	
	@Autowired
	private ProcessTaskService processTaskService;

	@Override
	public String getToken() {
		return "processtask/complete";
	}

	@Override
	public String getName() {
		return "工单完成接口";
	}

	@Override
	public String getConfig() {
		return null;
	}
	
	@Input({
		@Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "工单Id"),
		@Param(name = "processTaskStepId", type = ApiParamType.LONG, isRequired = true, desc = "当前步骤Id"),
		@Param(name = "nextStepId", type = ApiParamType.LONG, isRequired = true, desc = "激活下一步骤Id"),
		@Param(name = "content", type = ApiParamType.STRING, xss = true, desc = "原因")
	})
	@Description(desc = "工单完成接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {

		Long processTaskId = jsonObj.getLong("processTaskId");
		Long processTaskStepId = jsonObj.getLong("processTaskStepId");
		if(!processTaskService.verifyActionAuthoriy(processTaskId, processTaskStepId, ProcessTaskStepAction.COMPLETE)) {
			throw new ProcessTaskNoPermissionException(ProcessTaskStepAction.COMPLETE.getText());
		}
		ProcessTaskStepVo processTaskStepVo = null;
		if(processTaskStepId != null) {
			processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
		}else {
			List<ProcessTaskStepVo> processTaskStepList = processTaskMapper.getProcessTaskStepByProcessTaskIdAndType(processTaskId, ProcessStepType.START.getValue());
			if(processTaskStepList.size() != 1) {
				throw new ProcessTaskRuntimeException("工单：'" + processTaskId + "'有" + processTaskStepList.size() + "个开始步骤");
			}
			processTaskStepVo = processTaskStepList.get(0);
		}
		
		Long nextStepId = jsonObj.getLong("nextStepId");
		if(nextStepId != null) {
			ProcessTaskStepVo nextProcessTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(nextStepId);
			if(nextProcessTaskStepVo == null) {
				throw new ProcessTaskStepNotFoundException(nextStepId.toString());
			}
			if(!processTaskId.equals(nextProcessTaskStepVo.getProcessTaskId())) {
				throw new ProcessTaskRuntimeException("步骤：'" + nextStepId + "'不是工单：'" + processTaskId + "'的步骤");
			}
		}
		processTaskStepVo.setParamObj(jsonObj);
		IProcessStepHandler handler = ProcessStepHandlerFactory.getHandler(processTaskStepVo.getHandler());
		if(handler != null) {
			handler.complete(processTaskStepVo);
		}else {
			throw new ProcessStepHandlerNotFoundException(processTaskStepVo.getHandler());
		}
		return null;
	}

}
