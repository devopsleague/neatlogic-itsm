package codedriver.module.process.condition.handler;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.process.condition.core.IProcessTaskCondition;
import codedriver.framework.process.condition.core.ProcessTaskConditionBase;
import codedriver.framework.process.constvalue.ProcessExpression;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.framework.process.constvalue.ProcessFormHandlerType;
import codedriver.framework.process.constvalue.ProcessWorkcenterField;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.condition.ConditionVo;

@Component
public class ProcessTaskIdCondition extends ProcessTaskConditionBase implements IProcessTaskCondition{
	@Override
	public String getName() {
		return "id";
	}

	@Override
	public String getDisplayName() {
		return "工单id";
	}

	@Override
	public String getHandler(String processWorkcenterConditionType) {
		return ProcessFormHandlerType.INPUT.toString();
	}
	
	@Override
	public String getType() {
		return ProcessFieldType.COMMON.getValue();
	}

	@Override
	public JSONObject getConfig() {
		return null;
	}

	@Override
	public Integer getSort() {
		return 1;
	}

	@Override
	public List<ProcessExpression> getExpressionList() {
		return Arrays.asList(ProcessExpression.LIKE);
	}

	@Override
	public ProcessExpression getDefaultExpression() {
		return ProcessExpression.LIKE;
	}

	@Override
	public boolean predicate(ProcessTaskStepVo currentProcessTaskStepVo, ConditionVo conditionVo) {
		if(ProcessExpression.LIKE.getExpression().equals(conditionVo.getExpression())) {
			List<String> valueList = conditionVo.getValueList();
			if(CollectionUtils.isEmpty(valueList)) {
				return false;
			}
			String id = conditionVo.getValueList().get(0);
			if(StringUtils.isBlank(id)) {
				return false;
			}
			return currentProcessTaskStepVo.getProcessTaskId().toString().contains(id);
		}else {
			return false;
		}
	}
	
	@Override
	protected String getMyEsWhere(ConditionVo condition,List<ConditionVo> conditionList) {
		String where = "(";
		if(condition.getValueList().size() == 1) {
			Object value = condition.getValueList().get(0);
			where += String.format(ProcessExpression.getExpressionEs(condition.getExpression()),ProcessWorkcenterField.getConditionValue(ProcessWorkcenterField.ID.getValue()),String.format("'%s'",  value));
		}else {
			List<String> keywordList = condition.getValueList();
			for(int i=0;i<keywordList.size();i++) {
				if(i!=0) {
					where += " or ";
				}
				where += String.format(ProcessExpression.getExpressionEs(condition.getExpression()),ProcessWorkcenterField.getConditionValue(ProcessWorkcenterField.ID.getValue()),String.format("'%s'",  keywordList.get(i)));
			}
		}
		return where+")";
	}
}
