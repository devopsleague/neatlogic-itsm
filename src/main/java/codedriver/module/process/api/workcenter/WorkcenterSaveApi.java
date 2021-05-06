package codedriver.module.process.api.workcenter;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.AuthorityVo;
import codedriver.framework.process.auth.PROCESS_BASE;
import codedriver.framework.process.constvalue.ProcessWorkcenterType;
import codedriver.framework.process.dao.mapper.workcenter.WorkcenterMapper;
import codedriver.framework.process.exception.workcenter.WorkcenterNoAuthException;
import codedriver.framework.process.exception.workcenter.WorkcenterNotFoundException;
import codedriver.framework.process.exception.workcenter.WorkcenterParamException;
import codedriver.framework.process.workcenter.dto.WorkcenterVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.process.auth.WORKCENTER_MODIFY;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Transactional
@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class WorkcenterSaveApi extends PrivateApiComponentBase {

	@Autowired
	WorkcenterMapper workcenterMapper;
	@Autowired
	UserMapper userMapper;	
	@Autowired
	RoleMapper roleMapper;
	
	@Override
	public String getToken() {
		return "workcenter/save";
	}

	@Override
	public String getName() {
		return "工单中心分类保存接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name="uuid", type = ApiParamType.STRING, desc="分类uuid"),
		@Param(name="name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", desc="分类名",isRequired = true,xss = true),
		@Param(name="type", type = ApiParamType.STRING, desc="分类类型，system|custom 默认custom"),
		@Param(name="conditionConfig", type = ApiParamType.JSONOBJECT, desc="分类过滤配置，json格式",isRequired = true),
		@Param(name="valueList", type = ApiParamType.JSONARRAY, desc="授权列表，如果是system,则必填", isRequired = false)
	})
	@Output({
		
	})
	@Description(desc = "工单中心分类新增接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String uuid = jsonObj.getString("uuid");
		String name = jsonObj.getString("name");
		String type = StringUtils.isBlank(jsonObj.getString("type"))?ProcessWorkcenterType.CUSTOM.getValue():jsonObj.getString("type");
		JSONArray valueList = jsonObj.getJSONArray("valueList");
		String userUuid = UserContext.get().getUserUuid(true);
		WorkcenterVo workcenterVo = new WorkcenterVo(name);
		List<WorkcenterVo> workcenterList = null;
		if(StringUtils.isNotBlank(uuid)) {
			workcenterList = workcenterMapper.getWorkcenterByNameAndUuid(null, uuid);
			if(CollectionUtils.isNotEmpty(workcenterList)) {
				workcenterVo = workcenterList.get(0);
			}else {
				throw new WorkcenterNotFoundException(uuid);
			}
		}
		//判断重名
		/* 去重范围不确定，暂不去重
		 * if(workcenterMapper.checkWorkcenterNameIsRepeat(name,uuid)>0) { throw new
		 * WorkcenterNameRepeatException(name); }
		 */
		
		if((CollectionUtils.isNotEmpty(workcenterList)&&ProcessWorkcenterType.SYSTEM.getValue().equals(workcenterList.get(0).getType()))||ProcessWorkcenterType.SYSTEM.getValue().equals(type)) {
			//判断是否有管理员权限
			if(!AuthActionChecker.check(WORKCENTER_MODIFY.class.getSimpleName())) {
				throw new WorkcenterNoAuthException("管理");
			}
			workcenterMapper.deleteWorkcenterAuthorityByUuid(workcenterVo.getUuid());
		}
		if(type.equals(ProcessWorkcenterType.SYSTEM.getValue())) {
			if(CollectionUtils.isEmpty(valueList)) {
				throw new WorkcenterParamException("valueList");
			}
			workcenterVo.setType(ProcessWorkcenterType.SYSTEM.getValue());
			//更新角色
			for(Object value:valueList) {
				AuthorityVo authorityVo = new AuthorityVo();
				if(value.toString().startsWith(GroupSearch.ROLE.getValuePlugin())) {
					authorityVo.setType(GroupSearch.ROLE.getValue());
					authorityVo.setUuid(value.toString().replaceAll(GroupSearch.ROLE.getValuePlugin(), StringUtils.EMPTY));
				}else if(value.toString().startsWith(GroupSearch.USER.getValuePlugin())) {
					authorityVo.setType(GroupSearch.USER.getValue());
					authorityVo.setUuid(value.toString().replaceAll(GroupSearch.USER.getValuePlugin(), StringUtils.EMPTY));
				}else if(value.toString().startsWith(GroupSearch.COMMON.getValuePlugin())) {
					authorityVo.setType(GroupSearch.COMMON.getValue());
					authorityVo.setUuid(value.toString().replaceAll(GroupSearch.COMMON.getValuePlugin(), StringUtils.EMPTY));
				}else{
					throw new WorkcenterParamException("valueList");
				}
				workcenterMapper.insertWorkcenterAuthority(authorityVo,workcenterVo.getUuid());
			}
		}else {
			workcenterVo.setType(ProcessWorkcenterType.CUSTOM.getValue());
			if(StringUtils.isBlank(workcenterVo.getOwner())) {
				workcenterMapper.insertWorkcenterOwner(userUuid, workcenterVo.getUuid());
			}
		}
		
		if(StringUtils.isBlank(uuid)) {
			workcenterVo.setConditionConfig(jsonObj.toJSONString());
			workcenterMapper.insertWorkcenter(workcenterVo);
		}else { 
			workcenterVo.setName(name);
			workcenterMapper.updateWorkcenter(workcenterVo);
		}
		
		return null;
	}

}
