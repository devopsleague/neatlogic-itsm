package neatlogic.module.process.workerdispatcher.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dto.TeamUserVo;
import neatlogic.framework.process.constvalue.ProcessUserType;
import neatlogic.module.process.dao.mapper.processtask.ProcessTaskMapper;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskStepWorkerVo;
import neatlogic.framework.process.workerdispatcher.core.WorkerDispatcherBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WorkloadDispatcher extends WorkerDispatcherBase {
	
	@Autowired
	private TeamMapper teamMapper;
	
	@Autowired
	private ProcessTaskMapper processTaskMapper;

	@Override
	public String getName() {
		return "nmpwh.workloaddispatcher.getname";
	}	

	@Override
	protected List<ProcessTaskStepWorkerVo> myGetWorker(ProcessTaskStepVo processTaskStepVo, JSONObject configObj) {
		List<ProcessTaskStepWorkerVo> resultList = new ArrayList<>();
		if(MapUtils.isEmpty(configObj)) {
			return resultList;
		}
		String team = configObj.getString("team");
		if(StringUtils.isBlank(team)) {
			return resultList;
		}
		if (!team.startsWith(GroupSearch.TEAM.getValuePlugin())) {
			return resultList;
		}
		String[] split = team.split("#");
		List<TeamUserVo> teamUserList = teamMapper.getTeamUserListByTeamUuid(split[1]);
		if(CollectionUtils.isEmpty(teamUserList)) {
			return resultList;
		}
		List<String> teamUserUuidList = teamUserList.stream().map(TeamUserVo::getUserUuid).collect(Collectors.toList());
		/** 找出组员中工单量最小的组员集合 **/
		List<String> minWorkloadUserUuidList = new ArrayList<>();
		List<Map<String, Object>> workloadList = processTaskMapper.getWorkloadByTeamUuid(split[1]);
		if(CollectionUtils.isNotEmpty(workloadList)) {
			List<String> userUuidList = new ArrayList<>();
			for (Map<String, Object> workloadMap : workloadList) {
				userUuidList.add(workloadMap.get("userUuid").toString());
			}
			// 先看看有没有无工作量的员工
			teamUserUuidList.removeAll(userUuidList);
			if (CollectionUtils.isNotEmpty(teamUserUuidList)) {
				minWorkloadUserUuidList = teamUserUuidList;
			} else {
				long minWorkload = Integer.MAX_VALUE;
				for(Map<String, Object> workloadMap : workloadList) {
					long count = (long) workloadMap.get("count");
					if(count <= minWorkload) {
						minWorkload = count;
						Object userUuid = workloadMap.get("userUuid");
						if(userUuid != null) {
							minWorkloadUserUuidList.add(userUuid.toString());
						}
					}else {
						break;
					}
				}
			}
		} else {
			minWorkloadUserUuidList = teamUserUuidList;
		}
		/** 随机选取一位用户作为处理人 **/
		if(CollectionUtils.isNotEmpty(minWorkloadUserUuidList)) {
			Random random = new Random();
			int index = random.nextInt(minWorkloadUserUuidList.size());
			ProcessTaskStepWorkerVo worker = new ProcessTaskStepWorkerVo(processTaskStepVo.getProcessTaskId(), processTaskStepVo.getId(), GroupSearch.USER.getValue(), minWorkloadUserUuidList.get(index), ProcessUserType.MAJOR.getValue());
			resultList.add(worker);
		}
		return resultList;
	}

	@Override
	public String getHelp() {
		return "在选择的组中，找出工作量最少（只看待处理和处理中的任务数量）的用户作为当前步骤的处理人";
	}

	@Override
	public JSONArray getConfig() {
		JSONArray resultArray = new JSONArray();
		/** 选择处理组 **/
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("type", "userselect");
		jsonObj.put("name", "team");
		jsonObj.put("label", "处理组");
		jsonObj.put("validateList", Arrays.asList("required"));
		jsonObj.put("multiple", false);
		jsonObj.put("value", "");
		jsonObj.put("defaultValue", "");
		jsonObj.put("groupList", Arrays.asList("team"));
		resultArray.add(jsonObj);
		return resultArray;
	}

}
