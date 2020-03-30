package codedriver.module.process.condition.handler;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.framework.process.condition.core.IWorkcenterCondition;
import codedriver.framework.process.constvalue.ProcessExpression;
import codedriver.framework.process.constvalue.ProcessFormHandlerType;
import codedriver.framework.process.constvalue.ProcessWorkcenterConditionModel;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.condition.ConditionVo;

@Component
public class ProcessTaskChannelTypeCondition implements IWorkcenterCondition{

	@Override
	public String getName() {
		return "channelType";
	}

	@Override
	public String getDisplayName() {
		return "服务类型";
	}

	@Override
	public String getHandler(String processWorkcenterConditionType) {
		if(ProcessWorkcenterConditionModel.SIMPLE.getValue().equals(processWorkcenterConditionType)) {
			return ProcessFormHandlerType.CHECKBOX.toString();
		}else {
			return ProcessFormHandlerType.SELECT.toString();
		}
	}
	
	@Override
	public String getType() {
		return ProcessFieldType.COMMON.getValue();
	}

	@Override
	public JSONObject getConfig() {
		//TODO 服务类型 还未定下来
		JSONArray jsonList = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("value", "change");
		jsonObj.put("text", "变更");
		jsonList.add(jsonObj);
		JSONObject jsonObj2 = new JSONObject();
		jsonObj2.put("value", "case");
		jsonObj2.put("text", "事件");
		jsonList.add(jsonObj2);
		JSONObject returnObj = new JSONObject();
		returnObj.put("dataList", jsonList);
		returnObj.put("isMultiple", true);
		return returnObj;
	}

	@Override
	public Integer getSort() {
		return 7;
	}

	@Override
	public List<ProcessExpression> getExpressionList() {
		return Arrays.asList(ProcessExpression.INCLUDE,ProcessExpression.EXCLUDE);
	}

	@Override
	public ProcessExpression getDefaultExpression() {
		return ProcessExpression.INCLUDE;
	}

	@Override
	public boolean predicate(ProcessTaskStepVo currentProcessTaskStepVo, ConditionVo workcenterConditionVo) {
		// TODO linbq暂时没有服务类型
		return false;
	}
}
