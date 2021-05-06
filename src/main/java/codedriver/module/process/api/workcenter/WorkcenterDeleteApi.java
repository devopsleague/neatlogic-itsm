package codedriver.module.process.api.workcenter;

import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.process.auth.WORKCENTER_MODIFY;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.dao.mapper.workcenter.WorkcenterMapper;
import codedriver.framework.process.workcenter.dto.WorkcenterTheadVo;

@Transactional
@Service
@OperationType(type = OperationTypeEnum.DELETE)
@AuthAction(action = WORKCENTER_MODIFY.class)
public class WorkcenterDeleteApi extends PrivateApiComponentBase {

	@Autowired
	WorkcenterMapper workcenterMapper;
	
	@Override
	public String getToken() {
		return "workcenter/delete";
	}

	@Override
	public String getName() {
		return "工单中心分类删除接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name="uuid", type = ApiParamType.STRING, desc="分类uuid",isRequired = true)
	})
	@Output({
		
	})
	@Description(desc = "工单中心分类删除接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String uuid = jsonObj.getString("uuid");
		workcenterMapper.deleteWorkcenterAuthorityByUuid(uuid);
		workcenterMapper.deleteWorkcenterByUuid(uuid);
		workcenterMapper.deleteWorkcenterThead(new WorkcenterTheadVo(uuid,null));
		return null;
	}

}
