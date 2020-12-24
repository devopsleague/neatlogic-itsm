package codedriver.module.process.workcenter.column.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.RoleVo;
import codedriver.framework.dto.TeamVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.process.column.core.IProcessTaskColumn;
import codedriver.framework.process.column.core.ProcessTaskColumnBase;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.framework.process.constvalue.ProcessStepHandlerType;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.constvalue.ProcessUserType;
import codedriver.framework.process.constvalue.ProcessWorkcenterField;

@Component
public class ProcessTaskCurrentStepWorkerColumn extends ProcessTaskColumnBase implements IProcessTaskColumn{
	@Autowired
	UserMapper userMapper;
	@Autowired
	RoleMapper roleMapper;
	@Autowired
	TeamMapper teamMapper;
	@Override
	public String getName() {
		return "currentstepworker";
	}

	@Override
	public String getDisplayName() {
		return "当前步骤处理人";
	}

	@Override
	public Object getMyValue(JSONObject json) throws RuntimeException {
	    JSONArray stepArray = null;
        try {
         stepArray = (JSONArray) json.getJSONArray(ProcessWorkcenterField.STEP.getValue());
        }catch(Exception ex){
            return "";
        }
        String processTaskStatus = json.getString("status");
        if(CollectionUtils.isEmpty(stepArray)) {
            return CollectionUtils.EMPTY_COLLECTION;
        }
        JSONArray stepResultArray = JSONArray.parseArray(stepArray.toJSONString());
        ListIterator<Object> stepIterator = stepResultArray.listIterator();
        JSONArray workerArray = new JSONArray();
        List<String> workerList = new ArrayList<String>();
        while(stepIterator.hasNext()) {
            JSONObject currentStepJson = (JSONObject)stepIterator.next();
            String stepStatus =currentStepJson.getString("status");
            Integer isActive =currentStepJson.getInteger("isactive");
            if(ProcessTaskStatus.RUNNING.getValue().equals(processTaskStatus)&&(ProcessTaskStatus.DRAFT.getValue().equals(stepStatus)||(ProcessTaskStatus.PENDING.getValue().equals(stepStatus)&& isActive == 1)||ProcessTaskStatus.RUNNING.getValue().equals(stepStatus))) {
                JSONObject stepStatusJson = new JSONObject();
                stepStatusJson.put("name", stepStatus);
                stepStatusJson.put("text", ProcessTaskStatus.getText(stepStatus));
                stepStatusJson.put("color", ProcessTaskStatus.getColor(stepStatus));
                currentStepJson.put("status", stepStatusJson);
                JSONArray userTypeArray = currentStepJson.getJSONArray("usertypelist"); 
                if(CollectionUtils.isNotEmpty(userTypeArray)) {
                    ListIterator<Object> userTypeIterator = userTypeArray.listIterator();
                    while(userTypeIterator.hasNext()) {
                        JSONObject userTypeJson = (JSONObject) userTypeIterator.next();
                        //判断子任务|变更步骤
                        if(ProcessStepHandlerType.CHANGEHANDLE.getHandler().equals(currentStepJson.getString("handler"))
                            && ProcessUserType.MINOR.getValue().equals(userTypeJson.getString("usertype"))) {
                            userTypeJson.put("usertypename", "变更步骤");
                        }else if(ProcessUserType.MINOR.getValue().equals(userTypeJson.getString("usertype"))){
                            userTypeJson.put("usertypename", "子任务");
                        }
                        
                        JSONArray userArray = userTypeJson.getJSONArray("userlist");
                        JSONArray userArrayTmp = new JSONArray();
                        if(CollectionUtils.isNotEmpty(userArray)) {
                            List<String> userList = userArray.stream().map(object -> object.toString()).collect(Collectors.toList());
                            for(String user :userList) {
                                if(StringUtils.isNotBlank(user.toString())) {
                                    if(!workerList.contains(user+userTypeJson.getString("usertypename"))) {
                                        workerList.add(user+userTypeJson.getString("usertypename"));
                                        if(user.toString().startsWith(GroupSearch.USER.getValuePlugin())) {
                                            UserVo userVo =userMapper.getUserBaseInfoByUuid(user.toString().replaceFirst(GroupSearch.USER.getValuePlugin(), StringUtils.EMPTY));
                                            if(userVo != null) {
                                                JSONObject userJson = new JSONObject();
                                                userJson.put("type", GroupSearch.USER.getValue());
                                                userJson.put("worker", user);
                                                userJson.put("workername", userVo.getUserName());
                                                userJson.put("workerAvatar", userVo.getAvatar());
                                                userJson.put("workerVipLevel",userVo.getVipLevel());
                                                userJson.put("workTypename",userTypeJson.getString("usertypename"));
                                                userArrayTmp.add(userJson);
                                            }
                                        }else if(user.toString().startsWith(GroupSearch.ROLE.getValuePlugin())) {
                                            RoleVo roleVo = roleMapper.getRoleByUuid(user.toString().replaceFirst(GroupSearch.ROLE.getValuePlugin(), StringUtils.EMPTY));
                                            if(roleVo != null) {
                                                JSONObject roleJson = new JSONObject();
                                                roleJson.put("type", GroupSearch.ROLE.getValue());
                                                roleJson.put("worker", roleVo.getUuid());
                                                roleJson.put("workername", roleVo.getName());
                                                userArrayTmp.add(roleJson);
                                            }
                                        }else if(user.toString().startsWith(GroupSearch.TEAM.getValuePlugin())) {
                                            TeamVo teamVo = teamMapper.getTeamByUuid(user.toString().replaceFirst(GroupSearch.TEAM.getValuePlugin(), StringUtils.EMPTY));
                                            if(teamVo != null) {
                                                JSONObject teamJson = new JSONObject();
                                                teamJson.put("type", GroupSearch.TEAM.getValue());
                                                teamJson.put("worker", teamVo.getUuid());
                                                teamJson.put("workername", teamVo.getName());
                                                userArrayTmp.add(teamJson);
                                            }
                                        }
                                    }
                                }

                            }
                            workerArray.addAll(userArrayTmp);
                        }
                        
                    }
                }
            }else {
                stepIterator.remove();
            }
        }
        return workerArray;
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
		return 5;
	}

	@Override
	public Object getSimpleValue(Object json) {
		StringBuilder sb = new StringBuilder();
		if(json != null){
			JSONArray array = JSONArray.parseArray(json.toString());
			if(CollectionUtils.isNotEmpty(array)){
				for(int i = 0;i < array.size();i++){
					sb.append(array.getJSONObject(i).getString("workername") + ";");
				}
			}
		}
		return sb.toString();
	}
}