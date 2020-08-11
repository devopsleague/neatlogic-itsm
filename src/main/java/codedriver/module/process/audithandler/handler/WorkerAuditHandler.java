package codedriver.module.process.audithandler.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;

import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.RoleVo;
import codedriver.framework.dto.TeamVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.process.audithandler.core.ProcessTaskStepAuditDetailHandlerBase;
import codedriver.framework.process.constvalue.ProcessTaskAuditDetailType;
import codedriver.framework.process.dto.ProcessTaskStepAuditDetailVo;
@Service
public class WorkerAuditHandler extends ProcessTaskStepAuditDetailHandlerBase {
	
	@Autowired
	private UserMapper userMapper;
	
	@Autowired
	private TeamMapper teamMapper;
	
	@Autowired
	private RoleMapper roleMapper;
	
	@Override
	public String getType() {
		return ProcessTaskAuditDetailType.WORKERLIST.getValue();
	}

	private String parse(String content) {
		List<Map<String, String>> resultList = new ArrayList<>();
		List<String> workerList = JSON.parseArray(content, String.class);
		for(String worker : workerList) {
			String[] split = worker.split("#");
			if(GroupSearch.USER.getValue().equals(split[0])) {
				UserVo userVo = userMapper.getUserBaseInfoByUuid(split[1]);
				if(userVo != null) {
					Map<String, String> userMap = new HashMap<>();
					userMap.put("type", GroupSearch.USER.getValue());
					userMap.put("value", userVo.getUuid());
					userMap.put("text", userVo.getUserName());
					resultList.add(userMap);
				}
			}else if(GroupSearch.TEAM.getValue().equals(split[0])) {
				TeamVo teamVo = teamMapper.getTeamByUuid(split[1]);
				if(teamVo != null) {
					Map<String, String> teamMap = new HashMap<>();
					teamMap.put("type", GroupSearch.TEAM.getValue());
					teamMap.put("value", teamVo.getUuid());
					teamMap.put("text", teamVo.getName());
					resultList.add(teamMap);
				}
			}else if(GroupSearch.ROLE.getValue().equals(split[0])) {
				RoleVo roleVo = roleMapper.getRoleByUuid(split[1]);
				if(roleVo != null) {
					Map<String, String> roleMap = new HashMap<>();
					roleMap.put("type", GroupSearch.ROLE.getValue());
					roleMap.put("value", roleVo.getUuid());
					roleMap.put("text", roleVo.getName());
					resultList.add(roleMap);
				}
			}
		}
		return JSON.toJSONString(resultList);
	}

	@Override
	protected void myHandle(ProcessTaskStepAuditDetailVo processTaskStepAuditDetailVo) {
		String oldContent = processTaskStepAuditDetailVo.getOldContent();
		if(StringUtils.isNotBlank(oldContent)) {
			processTaskStepAuditDetailVo.setOldContent(parse(oldContent));
		}
		String newContent = processTaskStepAuditDetailVo.getNewContent();
		if(StringUtils.isNotBlank(newContent)) {
			processTaskStepAuditDetailVo.setNewContent(parse(newContent));
		}		
	}
}
