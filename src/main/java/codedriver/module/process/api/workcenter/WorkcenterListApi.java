package codedriver.module.process.api.workcenter;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserAuthVo;
import codedriver.framework.process.auth.PROCESS_BASE;
import codedriver.framework.process.constvalue.ProcessWorkcenterType;
import codedriver.framework.process.dao.mapper.workcenter.WorkcenterMapper;
import codedriver.framework.process.workcenter.dto.WorkcenterUserProfileVo;
import codedriver.framework.process.workcenter.dto.WorkcenterVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.process.auth.WORKCENTER_MODIFY;
import codedriver.module.process.service.NewWorkcenterService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class WorkcenterListApi extends PrivateApiComponentBase {

	@Autowired
	WorkcenterMapper workcenterMapper;
	
	@Autowired
	UserMapper userMapper;
	
	@Autowired
	TeamMapper teamMapper;
	
	@Autowired
	NewWorkcenterService newWorkcenterService;
	
	@Override
	public String getToken() {
		return "workcenter/list";
	}

	@Override
	public String getName() {
		return "获取工单中心分类列表接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name = "isAll", type = ApiParamType.INTEGER, desc = "获取类型，1:返回所有，用于定时更新; 0:仅返回列表", isRequired = true)
	})
	@Output({
		@Param(name="workcenter", explode = WorkcenterVo.class, desc="分类信息")
	})
	@Description(desc = "获取工单中心分类列表接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Integer isAll = jsonObj.getInteger("isAll");
		JSONObject workcenterJson = new JSONObject();
		String userUuid = UserContext.get().getUserUuid(true);
		List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
		List<String>  workcenterUuidList = workcenterMapper.getAuthorizedWorkcenterUuidList(userUuid,teamUuidList,UserContext.get().getRoleUuidList());
		List<WorkcenterVo> workcenterList = workcenterMapper.getAuthorizedWorkcenterListByUuidList(workcenterUuidList);
		List<UserAuthVo> userAuthList = userMapper.searchUserAllAuthByUserAuth(new UserAuthVo(userUuid,WORKCENTER_MODIFY.class.getSimpleName()));
		WorkcenterUserProfileVo userProfile= workcenterMapper.getWorkcenterUserProfileByUserUuid(userUuid);
		Map<String,Integer> workcenterUserSortMap = new HashMap<String,Integer>();
		String viewType = "table";//默认table展示
		if(userProfile != null) {
	    	JSONObject userConfig = JSONObject.parseObject(userProfile.getConfig());
	    	if(userConfig.containsKey("viewType")) {
	    		viewType = userConfig.getString("viewType");
	    	}
	    	if(userConfig.containsKey("workcenterList")) {
	    		JSONArray workcenterSortList = userConfig.getJSONArray("workcenterList");
	    		for(Object workcenterSort : workcenterSortList) {
	    			JSONObject workcenterSortJson =  (JSONObject)workcenterSort;
	    			workcenterUserSortMap.put(workcenterSortJson.getString("uuid"), workcenterSortJson.getInteger("sort"));
	    		}
	    	}
	    }
		for (WorkcenterVo workcenter : workcenterList) {
			if (workcenter.getType().equals(ProcessWorkcenterType.FACTORY.getValue())) {
				workcenter.setIsCanEdit(0);
			}
			if (workcenter.getType().equals(ProcessWorkcenterType.SYSTEM.getValue()) && CollectionUtils.isNotEmpty(userAuthList)) {
				workcenter.setIsCanEdit(1);
				workcenter.setIsCanRole(1);
			} else if (workcenter.getType().equals(ProcessWorkcenterType.CUSTOM.getValue())) {
				if (UserContext.get().getUserUuid(true).equalsIgnoreCase(workcenter.getOwner())) {
					workcenter.setIsCanEdit(1);
					if (CollectionUtils.isNotEmpty(userAuthList)) {
						workcenter.setIsCanRole(1);
					} else {
						workcenter.setIsCanRole(0);
					}
				} else {
					workcenter.setIsCanEdit(0);
					workcenter.setIsCanRole(0);
				}
			}

			//查询数量
			if (isAll == 1) {
				JSONObject conditionJson = JSONObject.parseObject(workcenter.getConditionConfig());
				JSONObject conditionConfig = conditionJson.getJSONObject("conditionConfig");
				conditionConfig.put("isProcessingOfMine", 1);
				conditionJson.put("pageSize",100);
				WorkcenterVo wcProcessingOfMine = new WorkcenterVo(conditionJson);
				Integer ProcessingOfMineCount = newWorkcenterService.doSearchLimitCount(wcProcessingOfMine);
				workcenter.setProcessingOfMineCount(ProcessingOfMineCount > 99 ? "99+" : ProcessingOfMineCount.toString());
			}
			workcenter.setConditionConfig(null);
			//排序 用户设置的排序优先
			if (workcenterUserSortMap.containsKey(workcenter.getUuid())) {
				workcenter.setSort(workcenterUserSortMap.get(workcenter.getUuid()));
			}
			//去除返回前端的多余字段
			workcenter.setConditionGroupList(null);
			workcenter.setConditionGroupRelList(null);
			workcenter.setIsProcessingOfMine(null);
		}
	    workcenterJson.put("viewType", viewType);
	    workcenterJson.put("workcenterList", workcenterList.stream().sorted(Comparator.comparing(WorkcenterVo::getSort)).collect(Collectors.toList()));
		return workcenterJson;
	}
}
