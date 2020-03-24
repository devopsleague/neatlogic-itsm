package codedriver.module.process.api.workcenter;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.apiparam.core.ApiParamType;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.process.workcenter.dao.mapper.WorkcenterMapper;
import codedriver.framework.process.workcenter.dto.WorkcenterTheadVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.IsActived;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;

@Transactional
@IsActived
@Service
public class WorkcenterTheadSaveApi extends ApiComponentBase {

	@Autowired
	WorkcenterMapper workcenterMapper;
	
	@Override
	public String getToken() {
		return "workcenter/thead/save";
	}

	@Override
	public String getName() {
		return "工单中心thead保存接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name="uuid", type = ApiParamType.STRING, desc="分类uuid",isRequired = true),
		@Param(name="theadList", type = ApiParamType.JSONARRAY, desc="分类uuid",isRequired = true),
		@Param(name="theadList[0].name", type = ApiParamType.STRING, desc="字段名"),
		@Param(name="theadList[0].width", type = ApiParamType.INTEGER, desc="字段宽度"),
		@Param(name="theadList[0].isShow", type = ApiParamType.INTEGER, desc="字段是否展示"),
		@Param(name="theadList[0].type", type = ApiParamType.STRING, desc="字段类型"),
		@Param(name="theadList[0].sort", type = ApiParamType.INTEGER, desc="字段排序")
	})
	@Output({
		
	})
	@Description(desc = "工单中心thead保存接口，用于用户自定义保存字段显示与排序")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String uuid = jsonObj.getString("uuid");
		JSONArray theadArray = jsonObj.getJSONArray("theadList");
		if(CollectionUtils.isNotEmpty(theadArray)) {
			workcenterMapper.deleteWorkcenterThead(new WorkcenterTheadVo(uuid,UserContext.get().getUserId()));
			for(Object thead : theadArray) {
				WorkcenterTheadVo workcenterTheadVo = new WorkcenterTheadVo((JSONObject)thead);
				workcenterTheadVo.setWorkcenterUuid(uuid);
				workcenterMapper.insertWorkcenterThead(workcenterTheadVo);
			}
		}
		return null;
	}

}