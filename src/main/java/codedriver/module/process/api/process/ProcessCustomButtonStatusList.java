package codedriver.module.process.api.process;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.exception.process.ProcessStepUtilHandlerNotFoundException;
import codedriver.framework.process.stephandler.core.IProcessStepUtilHandler;
import codedriver.framework.process.stephandler.core.ProcessStepUtilHandlerFactory;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
@Service
public class ProcessCustomButtonStatusList extends PrivateApiComponentBase {

	@Override
	public String getToken() {
		return "process/custombuttonstatus/list";
	}

	@Override
	public String getName() {
		return "获取流程步骤自定义按钮及状态列表";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name = "handler", type = ApiParamType.STRING, isRequired = true, desc = "步骤节点处理器")
	})
	@Output({
		@Param(name = "customButtonList", type = ApiParamType.JSONARRAY, desc = "按钮列表"),
		@Param(name = "customStatusList", type = ApiParamType.JSONARRAY, desc = "状态列表")
	})
	@Description(desc = "获取流程步骤自定义按钮及状态列表")
	@SuppressWarnings("serial")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONObject resultObj = new JSONObject();
		String handler = jsonObj.getString("handler");
		IProcessStepUtilHandler processStepUtilHandler = ProcessStepUtilHandlerFactory.getHandler(handler);
		if(processStepUtilHandler == null) {
			throw new ProcessStepUtilHandlerNotFoundException(handler);
		}
		JSONObject handlerConfig = processStepUtilHandler.makeupConfig(null);
		if(MapUtils.isNotEmpty(handlerConfig)) {
			JSONArray authorityList = handlerConfig.getJSONArray("authorityList");
			if(CollectionUtils.isNotEmpty(authorityList)) {
				resultObj.put("authorityList", authorityList);
			}
			JSONArray customButtonList = handlerConfig.getJSONArray("customButtonList");
			if(CollectionUtils.isNotEmpty(customButtonList)) {
				resultObj.put("customButtonList", customButtonList);
			}			
		}		
		JSONArray customStatusList = new JSONArray();
		for (ProcessTaskStatus status : ProcessTaskStatus.values()) {
			customStatusList.add(new JSONObject() {{this.put("name", status.getValue());this.put("text", status.getText());this.put("value", "");}});
		}
		resultObj.put("customStatusList", customStatusList);
		return resultObj;
	}

}
