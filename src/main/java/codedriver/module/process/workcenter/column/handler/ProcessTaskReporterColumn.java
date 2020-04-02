package codedriver.module.process.workcenter.column.handler;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserVo;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.framework.process.workcenter.column.core.IWorkcenterColumn;
import codedriver.framework.process.workcenter.column.core.WorkcenterColumnBase;

@Component
public class ProcessTaskReporterColumn extends WorkcenterColumnBase implements IWorkcenterColumn{
	@Autowired
	UserMapper userMapper;
	@Override
	public String getName() {
		return "reporter";
	}

	@Override
	public String getDisplayName() {
		return "代报人";
	}

	@Override
	public Object getMyValue(JSONObject json) throws RuntimeException {
		JSONObject userJson = new JSONObject();
		String userId = json.getString(this.getName());
		if(StringUtils.isNotBlank(userId)) {
			userId =userId.replaceFirst(GroupSearch.USER.getValuePlugin(), StringUtils.EMPTY);
		}
		UserVo userVo =userMapper.getUserByUserId(userId);
		if(userVo != null) {
			userJson.put("username", userVo.getUserName());
		}
		userJson.put("userid", userId);
		return userJson;
	}

	@Override
	public Boolean allowSort() {
		return false;
	}
	
	@Override
	public String getType() {
		return ProcessFieldType.COMMON.getValue();
	}

	@Override
	public String getClassName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getSort() {
		return 4;
	}

}
