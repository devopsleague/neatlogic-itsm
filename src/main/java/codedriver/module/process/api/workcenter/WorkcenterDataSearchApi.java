package codedriver.module.process.api.workcenter;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.apiparam.core.ApiParamType;
import codedriver.framework.process.workcenter.WorkcenterHandler;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.process.workcenter.dto.WorkcenterVo;

@Service
public class WorkcenterDataSearchApi extends ApiComponentBase {

	@Override
	public String getToken() {
		return "workcenter/search";
	}

	@Override
	public String getName() {
		return "工单中心搜索接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name = "uuid", type = ApiParamType.STRING, desc = "分类uuid", isRequired = true),
		@Param(name = "conditionGroupList", type = ApiParamType.JSONARRAY, desc = "条件组条件", isRequired = false),
		@Param(name = "conditionGroupRelList", type = ApiParamType.JSONARRAY, desc = "条件组连接类型", isRequired = false),
		@Param(name = "headerList", type = ApiParamType.JSONARRAY, desc = "显示的字段", isRequired = false),
		@Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页数", isRequired = false),
		@Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目", isRequired = false)
	})
	@Output({
		@Param(name="headerList", type = ApiParamType.JSONARRAY, desc="展示的字段"),
		@Param(name="dataList", type = ApiParamType.JSONARRAY, desc="展示的值"),
		@Param(name="rowNum", type = ApiParamType.INTEGER, desc="总数"),
		@Param(name="pageSize", type = ApiParamType.INTEGER, desc="每页数据条目"),
		@Param(name="currentPage", type = ApiParamType.INTEGER, desc="当前页数"),
		@Param(name="pageCount", type = ApiParamType.INTEGER, desc="总页数"),
	})
	@Description(desc = "工单中心搜索接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		return WorkcenterHandler.doSearch(new WorkcenterVo(jsonObj));
	}

}