package codedriver.module.process.condition.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.framework.process.condition.core.IProcessTaskCondition;
import codedriver.framework.process.condition.core.ProcessTaskConditionBase;
import codedriver.framework.process.constvalue.ProcessConditionModel;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.constvalue.ProcessWorkcenterField;

@Component
public class ProcessTaskAboutMeCondition extends ProcessTaskConditionBase implements IProcessTaskCondition{

	private String formHandlerType = FormHandlerType.SELECT.toString();
	
	private Map<String, Function<String,String>> map = new HashMap<>();
	
	{
		map.put("willdo", sql->getMeWillDoCondition());
	}
	
	@Autowired
	UserMapper userMapper;
	
	@Override
	public String getName() {
		return "aboutme";
	}

	@Override
	public String getDisplayName() {
		return "与我相关";
	}

	@Override
	public String getHandler(String processWorkcenterConditionType) {
		if(ProcessConditionModel.SIMPLE.getValue().equals(processWorkcenterConditionType)) {
			formHandlerType = FormHandlerType.CHECKBOX.toString();
		}
		return formHandlerType;
	}
	
	@Override
	public String getType() {
		return ProcessFieldType.COMMON.getValue();
	}

	@Override
	public JSONObject getConfig() {		
		JSONArray dataList = new JSONArray();
		dataList.add(new ValueTextVo("willdo", "待办"));
		dataList.add(new ValueTextVo("done", "已办"));
		
		JSONObject config = new JSONObject();
		config.put("type", formHandlerType);
		config.put("search", false);
		config.put("multiple", true);
		config.put("value", "");
		config.put("defaultValue", "");
		config.put("dataList", dataList);
		/** 以下代码是为了兼容旧数据结构，前端有些地方还在用 **/
		config.put("isMultiple", true);
		return config;
	}

	@Override
	public Integer getSort() {
		return 8;
	}

	@Override
	public ParamType getParamType() {
		return ParamType.ARRAY;
	}
	
	@Override
	protected String getMyEsWhere(Integer index,List<ConditionVo> conditionList) {
		String where = StringUtils.EMPTY;
		Function<String, String> result = map.get("willdo");
        if (result != null) {
            //执行这段表达式获得String类型的结果
            return result.apply("");
        }
		return where;
	}
	
	/**
	 * 附加我的待办条件
	 * @return
	 */
	private String getMeWillDoCondition() {
		String meWillDoSql = StringUtils.EMPTY;
		//status
		List<String> statusList = Arrays.asList(ProcessTaskStatus.RUNNING.getValue()).stream().map(object -> object.toString()).collect(Collectors.toList());
		String statusSql = String.format(Expression.INCLUDE.getExpressionEs(), ProcessWorkcenterField.getConditionValue(ProcessWorkcenterField.STATUS.getValue()),String.format(" '%s' ", String.join("','",statusList)));
		//common.step.filtstatus
		List<String> stepStatusList = Arrays.asList(ProcessTaskStatus.PENDING.getValue(),ProcessTaskStatus.RUNNING.getValue()).stream().map(object -> object.toString()).collect(Collectors.toList());
		String stepStatusSql = String.format(Expression.INCLUDE.getExpressionEs(), ProcessWorkcenterField.getConditionValue(ProcessWorkcenterField.STEP.getValue())+".filtstatus",String.format(" '%s' ", String.join("','",stepStatusList)));
		//common.step.usertypelist.userlist
		List<String> userList = new ArrayList<String>();
		userList.add(GroupSearch.USER.getValuePlugin()+UserContext.get().getUserUuid());
		//如果是待处理状态，则需额外匹配角色和组
		UserVo userVo = userMapper.getUserByUuid(UserContext.get().getUserUuid());
		if(userVo != null) {
			List<String> teamList = userVo.getTeamUuidList();
			if(CollectionUtils.isNotEmpty(teamList)) {
				for(String team : teamList) {
					userList.add(GroupSearch.TEAM.getValuePlugin()+team);
				}
			}
			List<String> roleUuidList = userVo.getRoleUuidList();
			if(CollectionUtils.isNotEmpty(roleUuidList)) {
				for(String roleUuid : roleUuidList) {
					userList.add(GroupSearch.ROLE.getValuePlugin() + roleUuid);
				}
			}
		}
		meWillDoSql = String.format(" %s and %s and common.step.usertypelist.list.value contains any ( %s ) and common.step.usertypelist.list.status contains any ('pending','doing') and not common.step.isactive contains any (0,-1)", statusSql,stepStatusSql,String.format(" '%s' ", String.join("','",userList))) ;
//		meWillDoSql = String.format(" common.step.usertypelist.list.value contains any ( %s ) and common.step.usertypelist.list.status contains any ('pending','doing')", String.format(" '%s' ", String.join("','",userList))) ;
		return meWillDoSql;
	}

	@Override
	public Object valueConversionText(Object value, JSONObject config) {
		// TODO Auto-generated method stub
		return null;
	}
}