package codedriver.module.process.workcenter.column.handler;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.process.column.core.IProcessTaskColumn;
import codedriver.framework.process.column.core.ProcessTaskColumnBase;
import codedriver.framework.process.constvalue.ProcessFieldType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

@Component
public class ProcessTaskFocusUsersColumn extends ProcessTaskColumnBase implements IProcessTaskColumn{

	@Override
	public String getName() {
		return "focususers";
	}

	@Override
	public String getDisplayName() {
		return "关注此工单的用户";
	}

	@Override
	public Object getMyValue(JSONObject json) throws RuntimeException {
		JSONObject focusUserObj = new JSONObject();
		JSONArray focusUsers = json.getJSONArray(this.getName());
		focusUserObj.put("focusUserList",focusUsers);
		boolean isCurrentUserFocus = false;
		if(CollectionUtils.isNotEmpty(focusUsers)){
			String userUuid = "user#" + UserContext.get().getUserUuid();
			isCurrentUserFocus = focusUsers.contains(userUuid);
		}
		focusUserObj.put("isCurrentUserFocus",isCurrentUserFocus);
		return focusUserObj;
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
		return null;
	}

	@Override
	public Integer getSort() {
		return 15;
	}

}
