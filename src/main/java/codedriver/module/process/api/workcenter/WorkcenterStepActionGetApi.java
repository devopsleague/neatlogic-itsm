package codedriver.module.process.api.workcenter;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.workcenter.dto.WorkcenterVo;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.process.service.WorkcenterService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class WorkcenterStepActionGetApi extends ApiComponentBase {

	@Autowired
	WorkcenterService workcenterService;
	
	@Override
	public String getToken() {
		return "workcenter/processtask/get";
	}

	@Override
	public String getName() {
		return "获取工单中心工单";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
			@Param(name = "taskId", desc="工单ID" ,type = ApiParamType.STRING, isRequired = true),
	})
	@Output({
	})
	@Description(desc = "获取工单中心工单")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String jsonStr = "{\"conditionGroupList\":[{\"uuid\":\"185fc3127dfb4566b4d5a303cb4797b9\",\"channelUuidList\":[],\"conditionRelList\":[],\"conditionList\":[{\"uuid\":\"cbeae47566914b488fd1c8939d465dd8\",\"name\":\"id\",\"type\":\"common\",\"valueList\":[\"163426156421120\"],\"expression\":\"like\"}]}]}";
		JSONObject jsonObject = JSON.parseObject(jsonStr);
		String taskId = jsonObj.getString("taskId");
		jsonObject.getJSONArray("conditionGroupList")
				.getJSONObject(0)
				.getJSONArray("conditionList")
				.getJSONObject(0)
				.getJSONArray("valueList")
				.set(0,taskId);

		WorkcenterVo workcenterVo = JSON.parseObject(jsonObject.toJSONString(), new TypeReference<WorkcenterVo>(){});
		JSONObject result = workcenterService.doSearch(workcenterVo);
		JSONObject json = null;
		if(result != null){
			JSONArray tbodyList = result.getJSONArray("tbodyList");
			if(tbodyList != null){
				json = tbodyList.getJSONObject(0);
			}
		}
		return json;
	}
}
