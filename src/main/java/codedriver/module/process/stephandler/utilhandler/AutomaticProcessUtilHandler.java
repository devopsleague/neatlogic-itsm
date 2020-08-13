package codedriver.module.process.stephandler.utilhandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.process.constvalue.ProcessStepHandler;
import codedriver.framework.process.constvalue.ProcessTaskStepAction;
import codedriver.framework.process.dto.ProcessStepVo;
import codedriver.framework.process.dto.ProcessStepWorkerPolicyVo;
import codedriver.framework.process.stephandler.core.ProcessStepUtilHandlerBase;
import codedriver.module.process.notify.handler.ProcessNotifyPolicyHandler;
@Service
public class AutomaticProcessUtilHandler extends ProcessStepUtilHandlerBase {

	@Override
	public String getHandler() {
		return ProcessStepHandler.AUTOMATIC.getHandler();
	}

	@Override
	public Object getHandlerStepInfo(Long processTaskStepId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getHandlerStepInitInfo(Long processTaskStepId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void makeupProcessStep(ProcessStepVo processStepVo, JSONObject stepConfigObj) {
		/** 组装通知策略id **/
		JSONObject notifyPolicyConfig = stepConfigObj.getJSONObject("notifyPolicyConfig");
        Long policyId = notifyPolicyConfig.getLong("policyId");
        if(policyId != null) {
        	processStepVo.setNotifyPolicyId(policyId);
        }
		/** 组装分配策略 **/
		JSONObject workerPolicyConfig = stepConfigObj.getJSONObject("workerPolicyConfig");
		if (MapUtils.isNotEmpty(workerPolicyConfig)) {
			JSONArray policyList = workerPolicyConfig.getJSONArray("policyList");
			if (CollectionUtils.isNotEmpty(policyList)) {
				List<ProcessStepWorkerPolicyVo> workerPolicyList = new ArrayList<>();
				for (int k = 0; k < policyList.size(); k++) {
					JSONObject policyObj = policyList.getJSONObject(k);
					if (!"1".equals(policyObj.getString("isChecked"))) {
						continue;
					}
					ProcessStepWorkerPolicyVo processStepWorkerPolicyVo = new ProcessStepWorkerPolicyVo();
					processStepWorkerPolicyVo.setProcessUuid(processStepVo.getProcessUuid());
					processStepWorkerPolicyVo.setProcessStepUuid(processStepVo.getUuid());
					processStepWorkerPolicyVo.setPolicy(policyObj.getString("type"));
					processStepWorkerPolicyVo.setSort(k + 1);
					processStepWorkerPolicyVo.setConfig(policyObj.getString("config"));
					workerPolicyList.add(processStepWorkerPolicyVo);
				}
				processStepVo.setWorkerPolicyList(workerPolicyList);
			}
		}
		/** 收集引用的外部调用uuid **/
		JSONObject automaticConfig = stepConfigObj.getJSONObject("automaticConfig");
		if(MapUtils.isNotEmpty(automaticConfig)) {
			JSONObject requestConfig = automaticConfig.getJSONObject("requestConfig");
			if(MapUtils.isNotEmpty(requestConfig)) {
				String integrationUuid = requestConfig.getString("integrationUuid");
				if(StringUtils.isNotBlank(integrationUuid)) {
					processStepVo.getIntegrationUuidList().add(integrationUuid);
				}
			}
			JSONObject callbackConfig = automaticConfig.getJSONObject("callbackConfig");
			if(MapUtils.isNotEmpty(callbackConfig)) {
				JSONObject config = callbackConfig.getJSONObject("config");
				if(MapUtils.isNotEmpty(config)) {
					String integrationUuid = config.getString("integrationUuid");
					if(StringUtils.isNotBlank(integrationUuid)) {
						processStepVo.getIntegrationUuidList().add(integrationUuid);
					}
				}
			}
		}
	}
	
	@Override
	public void updateProcessTaskStepUserAndWorker(Long processTaskId, Long processTaskStepId) {
	}
	
	@SuppressWarnings("serial")
	@Override
	public JSONObject makeupConfig(JSONObject configObj) {
		if(configObj == null) {
			configObj = new JSONObject();
		}
		JSONObject resultObj = new JSONObject();
		
		/** 授权 **/
		JSONArray authorityArray = new JSONArray();
		ProcessTaskStepAction[] stepActions = {
				ProcessTaskStepAction.VIEW, 
				//ProcessTaskStepAction.ABORT, 
				ProcessTaskStepAction.TRANSFER, 
				ProcessTaskStepAction.UPDATE, 
				ProcessTaskStepAction.URGE
		};
		for(ProcessTaskStepAction stepAction : stepActions) {
			authorityArray.add(new JSONObject() {{
				this.put("action", stepAction.getValue());
				this.put("text", stepAction.getText());
				this.put("acceptList", stepAction.getAcceptList());
				this.put("groupList", stepAction.getGroupList());
			}});
		}
		JSONArray authorityList = configObj.getJSONArray("authorityList");
		if(CollectionUtils.isNotEmpty(authorityList)) {
			Map<String, JSONArray> authorityMap = new HashMap<>();
			for(int i = 0; i < authorityList.size(); i++) {
				JSONObject authority = authorityList.getJSONObject(i);
				authorityMap.put(authority.getString("action"), authority.getJSONArray("acceptList"));
			}
			for(int i = 0; i < authorityArray.size(); i++) {
				JSONObject authority = authorityArray.getJSONObject(i);
				JSONArray acceptList = authorityMap.get(authority.getString("action"));
				if(acceptList != null) {
					authority.put("acceptList", acceptList);
				}
			}
		}
		resultObj.put("authorityList", authorityArray);
		
		/** 按钮映射 **/
		JSONArray customButtonArray = new JSONArray();
		ProcessTaskStepAction[] stepButtons = {
				ProcessTaskStepAction.COMPLETE, 
				ProcessTaskStepAction.BACK, 
				//ProcessTaskStepAction.COMMENT, 
				ProcessTaskStepAction.TRANSFER, 
				ProcessTaskStepAction.START//,
				//ProcessTaskStepAction.ABORT, 
				//ProcessTaskStepAction.RECOVER
		};
		for(ProcessTaskStepAction stepButton : stepButtons) {
			customButtonArray.add(new JSONObject() {{
				this.put("name", stepButton.getValue());
				this.put("customText", stepButton.getText());
				this.put("value", "");
			}});
		}
		/** 子任务按钮映射列表 **/
		ProcessTaskStepAction[] subtaskButtons = {
				ProcessTaskStepAction.ABORTSUBTASK, 
				ProcessTaskStepAction.COMMENTSUBTASK, 
				ProcessTaskStepAction.COMPLETESUBTASK, 
				ProcessTaskStepAction.CREATESUBTASK, 
				ProcessTaskStepAction.REDOSUBTASK, 
				ProcessTaskStepAction.EDITSUBTASK
		};
		for(ProcessTaskStepAction subtaskButton : subtaskButtons) {
			customButtonArray.add(new JSONObject() {{
				this.put("name", subtaskButton.getValue());
				this.put("customText", subtaskButton.getText() + "(子任务)");
				this.put("value", "");
			}});
		}
		JSONArray customButtonList = configObj.getJSONArray("customButtonList");
		if(CollectionUtils.isNotEmpty(customButtonList)) {
			Map<String, String> customButtonMap = new HashMap<>();
			for(int i = 0; i < customButtonList.size(); i++) {
				JSONObject customButton = customButtonList.getJSONObject(i);
				customButtonMap.put(customButton.getString("name"), customButton.getString("value"));
			}
			for(int i = 0; i < customButtonArray.size(); i++) {
				JSONObject customButton = customButtonArray.getJSONObject(i);
				String value = customButtonMap.get(customButton.getString("name"));
				if(StringUtils.isNotBlank(value)) {
					customButton.put("value", value);
				}
			}
		}
		resultObj.put("customButtonList", customButtonArray);
		
		/** 通知 **/
		JSONObject notifyPolicyObj = new JSONObject();
		JSONObject notifyPolicyConfig = configObj.getJSONObject("notifyPolicyConfig");
		if(MapUtils.isNotEmpty(notifyPolicyConfig)) {
			notifyPolicyObj.putAll(notifyPolicyConfig);
		}
		notifyPolicyObj.put("handler", ProcessNotifyPolicyHandler.class.getName());
		resultObj.put("notifyPolicyConfig", notifyPolicyObj);
		
		return resultObj;
	}

}