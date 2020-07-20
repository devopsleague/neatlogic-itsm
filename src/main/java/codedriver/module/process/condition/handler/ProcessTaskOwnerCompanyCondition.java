package codedriver.module.process.condition.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dto.TeamVo;
import codedriver.framework.process.condition.core.IProcessTaskCondition;
import codedriver.framework.process.condition.core.ProcessTaskConditionBase;
import codedriver.framework.process.constvalue.ProcessFieldType;

@Component
public class ProcessTaskOwnerCompanyCondition extends ProcessTaskConditionBase implements IProcessTaskCondition {

	@Autowired
	private TeamMapper teamMapper;
	
	@Override
	public String getName() {
		return "ownercompany";
	}

	@Override
	public String getDisplayName() {
		return "上报人公司";
	}

	@Override
	public String getHandler(String processWorkcenterConditionType) {
		return FormHandlerType.SELECT.toString();
	}

	@Override
	public String getType() {
		return ProcessFieldType.COMMON.getValue();
	}

	@Override
	public JSONObject getConfig() {
		JSONObject config = new JSONObject();
		config.put("type", FormHandlerType.SELECT.toString());
		config.put("search", true);
		config.put("url", "/api/rest/team/search?currentPage=1&pageSize=20&level=company");
		config.put("rootName", "teamList");
		config.put("valueName", "uuid");
		config.put("textName", "name");
		config.put("multiple", true);
		config.put("value", "");
		config.put("defaultValue", "");
//		config.put("name", "ownercompany");
//		config.put("label", "");
//		config.put("validateList", Arrays.asList("required"));
//		config.put("dataList", "");
		/** 以下代码是为了兼容旧数据结构，前端有些地方还在用 **/
		config.put("isMultiple", true);
		JSONObject mappingObj = new JSONObject();
		mappingObj.put("value", "uuid");
		mappingObj.put("text", "name");
		config.put("mapping", mappingObj);
		return config;
	}

	@Override
	public Integer getSort() {
		return 11;
	}

	@Override
	public ParamType getParamType() {
		return ParamType.ARRAY;
	}

	@Override
	public Object valueConversionText(Object value, JSONObject config) {
		if(value != null) {
			if(value instanceof String) {
				TeamVo teamVo = teamMapper.getTeamByUuid((String)value);
				if(teamVo != null) {
					return teamVo.getName();
				}
			}else if(value instanceof List){
				List<String> valueList = JSON.parseArray(JSON.toJSONString(value), String.class);
				List<String> textList = new ArrayList<>();
				for(String valueStr : valueList) {
					TeamVo teamVo = teamMapper.getTeamByUuid(valueStr);
					if(teamVo != null) {
						textList.add(teamVo.getName());					
					}else {
						textList.add(valueStr);
					}
				}
				return String.join("、", textList);
			}
		}	
		return value;
	}

}