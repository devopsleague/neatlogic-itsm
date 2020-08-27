package codedriver.module.process.api.processtask;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.constvalue.ProcessFlowDirection;
import codedriver.framework.process.constvalue.ProcessTaskStepAction;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.stephandler.core.ProcessStepUtilHandlerFactory;
import codedriver.module.process.service.ProcessTaskService;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ProcessTaskNextStepListApi extends PrivateApiComponentBase{

	@Autowired
	private ProcessTaskMapper processTaskMapper;
	
	@Autowired
	private ProcessTaskService processTaskService;
	
	@Override
	public String getToken() {
		return "processtask/nextstep/list";
	}

	@Override
	public String getName() {
		return "下一可流转步骤列表接口";
	}

	@Override
	public String getConfig() {
		return null;
	}
	
	@Input({
		@Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "工单Id"),
		@Param(name = "processTaskStepId", type = ApiParamType.LONG, isRequired = true, desc = "当前步骤Id"),
		@Param(name = "action", type = ApiParamType.ENUM, rule = "complete,back", desc = "操作类型"),
	})
	@Output({
		@Param(name = "Return", explode = ProcessTaskStepVo[].class, desc = "下一可流转步骤列表")
	})
	@Description(desc = "下一可流转步骤列表接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long processTaskId = jsonObj.getLong("processTaskId");
        Long processTaskStepId = jsonObj.getLong("processTaskStepId");
        processTaskService.checkProcessTaskParamsIsLegal(processTaskId, processTaskStepId);
		ProcessTaskStepAction processTaskStepAction = ProcessTaskStepAction.COMPLETE;
		String action = jsonObj.getString("action");
		if(ProcessTaskStepAction.BACK.getValue().equals(action)) {
			processTaskStepAction = ProcessTaskStepAction.BACK;
		}else {
			action = ProcessTaskStepAction.COMPLETE.getValue();
		}
		ProcessStepUtilHandlerFactory.getHandler().verifyActionAuthoriy(processTaskId, processTaskStepId, processTaskStepAction);
		List<ProcessTaskStepVo> resultList = new ArrayList<>();
		List<ProcessTaskStepVo> processTaskStepList = processTaskMapper.getToProcessTaskStepByFromIdAndType(processTaskStepId,null);
		for(ProcessTaskStepVo processTaskStep : processTaskStepList) {
			if(processTaskStep.getIsActive() != null) {
				if(ProcessTaskStepAction.COMPLETE.getValue().equals(action) && ProcessFlowDirection.FORWARD.getValue().equals(processTaskStep.getFlowDirection())) {
					if(StringUtils.isNotBlank(processTaskStep.getAliasName())) {
						processTaskStep.setName(processTaskStep.getAliasName());
						processTaskStep.setFlowDirection("");
					}else {
						processTaskStep.setFlowDirection(ProcessFlowDirection.FORWARD.getText());
					}
					resultList.add(processTaskStep);
				}else if(ProcessTaskStepAction.BACK.getValue().equals(action) && ProcessFlowDirection.BACKWARD.getValue().equals(processTaskStep.getFlowDirection()) && processTaskStep.getIsActive().intValue() != 0){
					if(StringUtils.isNotBlank(processTaskStep.getAliasName())) {
						processTaskStep.setName(processTaskStep.getAliasName());
						processTaskStep.setFlowDirection("");
					}else {
						processTaskStep.setFlowDirection(ProcessFlowDirection.BACKWARD.getText());
					}
					resultList.add(processTaskStep);
				}
			}
		}
		return resultList;
	}

}
