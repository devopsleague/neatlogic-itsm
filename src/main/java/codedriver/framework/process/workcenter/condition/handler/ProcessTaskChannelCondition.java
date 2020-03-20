package codedriver.framework.process.workcenter.condition.handler;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.workcenter.condition.core.IWorkcenterCondition;
import codedriver.module.process.constvalue.ProcessExpression;
import codedriver.module.process.constvalue.ProcessFormHandlerType;
import codedriver.module.process.constvalue.ProcessWorkcenterCondition;
import codedriver.module.process.constvalue.ProcessWorkcenterConditionType;
import codedriver.module.process.dto.ProcessTaskStepVo;
import codedriver.module.process.dto.ProcessTaskVo;
import codedriver.module.process.workcenter.dto.WorkcenterConditionVo;

@Component
public class ProcessTaskChannelCondition implements IWorkcenterCondition{
	
	@Autowired
	private ProcessTaskMapper processTaskMapper;

	@Override
	public String getName() {
		return ProcessWorkcenterCondition.CHANNEL.getValue();
	}

	@Override
	public String getDisplayName() {
		return ProcessWorkcenterCondition.CHANNEL.getName();
	}

	@Override
	public String getHandler(String processWorkcenterConditionType) {
		return ProcessFormHandlerType.SELECT.toString();
	}
	
	@Override
	public String getType() {
		return ProcessWorkcenterConditionType.COMMON.getValue();
	}

	@Override
	public JSONObject getConfig() {
		/*ChannelVo channel = new ChannelVo();
		channel.setIsActive(1);
		channel.setNeedPage(false);
 		List<ChannelVo> channelList = channelMapper.searchChannelList(channel);
		JSONArray jsonList = new JSONArray();
		for (ChannelVo channelVo : channelList) {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("value", channelVo.getUuid());
			jsonObj.put("text", channelVo.getName());
			jsonList.add(jsonObj);
		}*/
		JSONObject returnObj = new JSONObject();
		returnObj.put("url", "api/rest/process/channel/search");
		returnObj.put("isMultiple", true);
		returnObj.put("rootName", "channelList");
		JSONObject mappingObj = new JSONObject();
		mappingObj.put("value", "uuid");
		mappingObj.put("text", "name");
		returnObj.put("mapping", mappingObj);
		return returnObj;
	}

	@Override
	public Integer getSort() {
		return 9;
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
	public boolean predicate(ProcessTaskStepVo currentProcessTaskStepVo, WorkcenterConditionVo conditionVo) {
		boolean result = false;
		List<String> valueList = conditionVo.getValueList();
		if(!CollectionUtils.isEmpty(valueList)) {
			ProcessTaskVo processTask = processTaskMapper.getProcessTaskById(currentProcessTaskStepVo.getProcessTaskId());
			result = valueList.contains(processTask.getChannelUuid());
		}			
		if(ProcessExpression.INCLUDE.getExpression().equals(conditionVo.getExpression())) {
			return result;
		}else if(ProcessExpression.EXCLUDE.getExpression().equals(conditionVo.getExpression())) {
			return !result;
		}else {
			return false;
		}
	}

}
