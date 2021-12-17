package codedriver.module.process.api.processtask;


import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.process.auth.PROCESS_BASE;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.constvalue.ProcessTaskStepDataType;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dao.mapper.ProcessTaskStepDataMapper;
import codedriver.framework.process.dao.mapper.SelectContentByHashMapper;
import codedriver.framework.process.dto.ProcessTaskStepDataVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.automatic.AutomaticConfigVo;
import codedriver.framework.process.exception.processtask.ProcessTaskStepNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.process.service.ProcessTaskAutomaticService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
@AuthAction(action = PROCESS_BASE.class)
public class ProcessTaskAutomaticRetryApi extends PrivateApiComponentBase {

	@Resource
	private ProcessTaskMapper processTaskMapper;
	
	@Resource
	ProcessTaskAutomaticService processTaskAutomaticService;
	
	@Resource
	ProcessTaskStepDataMapper processTaskStepDataMapper;
	
	@Resource
	SelectContentByHashMapper selectContentByHashMapper;

	@Override
	public String getToken() {
		return "processtask/automatic/retry";
	}

	@Override
	public String getName() {
		return "工单automatic步骤重试接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Override
	@Input({
			@Param(name = "processTaskStepId", type = ApiParamType.LONG, desc = "工单步骤Id", isRequired = true)
	})
	@Output({})
	@Description(desc = "工单automatic步骤重试接口")
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long processTaskStepId = jsonObj.getLong("processTaskStepId");
		ProcessTaskStepVo  processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
		if (processTaskStepVo == null) {
			throw new ProcessTaskStepNotFoundException(processTaskStepId);
		}
		ProcessTaskStepDataVo searchVo = new ProcessTaskStepDataVo(
				processTaskStepVo.getProcessTaskId(),
				processTaskStepVo.getId(),
				ProcessTaskStepDataType.AUTOMATIC.getValue(),
				SystemUser.SYSTEM.getUserUuid()
		);
		ProcessTaskStepDataVo data = processTaskStepDataMapper.getProcessTaskStepData(searchVo);
		if (data == null) {
			return null;
		}
		JSONObject dataObject = data.getData();
		if (MapUtils.isEmpty(dataObject)) {
			return null;
		}
		JSONObject requestAudit = dataObject.getJSONObject("requestAudit");
		if(MapUtils.isEmpty(requestAudit)) {
			return null;
		}
		JSONObject requestStatusObj = requestAudit.getJSONObject("status");
		if(MapUtils.isEmpty(requestStatusObj)) {
			return null;
		}
		String requestStatus = requestStatusObj.getString("value");
		if (ProcessTaskStatus.SUCCEED.getValue().equals(requestStatus)) {
			JSONObject callbackAudit = dataObject.getJSONObject("callbackAudit");
			if(MapUtils.isEmpty(callbackAudit)) {
				return null;
			}
			JSONObject callbackStatusObj = callbackAudit.getJSONObject("status");
			if(MapUtils.isEmpty(callbackStatusObj)) {
				return null;
			}
			String callbackStatus = callbackStatusObj.getString("value");
			if(!ProcessTaskStatus.SUCCEED.getValue().equals(callbackStatus) && !ProcessTaskStatus.FAILED.getValue().equals(callbackStatus)) {
				//发送回调j请求
				processTaskAutomaticService.callbackRequest(processTaskStepVo);
			}
		} else {
			//发送第一次请求
			processTaskAutomaticService.firstRequest(processTaskStepVo);
		}
		return null;
	}

}
