package codedriver.module.process.workcenter.column.handler;

import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dashboard.dto.DashboardDataGroupVo;
import codedriver.framework.dashboard.dto.DashboardDataSubGroupVo;
import codedriver.framework.dashboard.dto.DashboardWidgetDataVo;
import codedriver.framework.process.column.core.IProcessTaskColumn;
import codedriver.framework.process.column.core.ProcessTaskColumnBase;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.workcenter.dto.*;
import codedriver.framework.process.workcenter.table.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProcessTaskStepNameColumn extends ProcessTaskColumnBase implements IProcessTaskColumn{
	@Autowired
	UserMapper userMapper;
	@Autowired
	RoleMapper roleMapper;
	@Autowired
	TeamMapper teamMapper;

	@Override
	public String getName() {
		return "stepname";
	}

	@Override
	public String getDisplayName() {
		return "步骤名";
	}

	@Override
	public Boolean getDisabled() {
		return true;
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
	public Boolean getMyIsShow() {
		return false;
	}

	/*@Override
	public Object getSimpleValue(Object json) {
		return null;
	}*/

	@Override
	public Object getValue(ProcessTaskVo processTaskVo) {
		return null;
	}

	@Override
	public List<TableSelectColumnVo> getTableSelectColumn() {
		return new ArrayList<TableSelectColumnVo>() {
			{
				add(new TableSelectColumnVo(new ProcessTaskStepSqlTable(), Arrays.asList(
						new SelectColumnVo(ProcessTaskStepSqlTable.FieldEnum.ID.getValue(), "processTaskStepId", true),
						new SelectColumnVo(ProcessTaskStepSqlTable.FieldEnum.NAME.getValue(), "processTaskStepName", true)
				)));
			}
		};
	}

	@Override
	public List<JoinTableColumnVo> getMyJoinTableColumnList() {
		return new ArrayList<JoinTableColumnVo>() {
			{
				add(new JoinTableColumnVo(new ProcessTaskSqlTable(), new ProcessTaskStepSqlTable(), new ArrayList<JoinOnVo>() {{
					add(new JoinOnVo(ProcessTaskSqlTable.FieldEnum.ID.getValue(), ProcessTaskStepSqlTable.FieldEnum.PROCESSTASK_ID.getValue()));
				}}));
			}
		};
	}

	@Override
	public void getMyDashboardDataVo(DashboardWidgetDataVo dashboardDataVo, WorkcenterVo workcenterVo, List<Map<String, Object>> mapList) {
		if (getName().equals(workcenterVo.getDashboardWidgetChartConfigVo().getGroup())) {
			DashboardDataGroupVo dashboardDataGroupVo = new DashboardDataGroupVo("processTaskStepName", workcenterVo.getDashboardWidgetChartConfigVo().getGroup(), "processTaskStepName", workcenterVo.getDashboardWidgetChartConfigVo().getGroupDataCountMap());
			dashboardDataVo.setDataGroupVo(dashboardDataGroupVo);
		}
		//如果存在子分组
		if (getName().equals(workcenterVo.getDashboardWidgetChartConfigVo().getSubGroup())) {
			DashboardDataSubGroupVo dashboardDataSubGroupVo = null;
			dashboardDataSubGroupVo = new DashboardDataSubGroupVo("processTaskStepName", workcenterVo.getDashboardWidgetChartConfigVo().getSubGroup(), "processTaskStepName");
			dashboardDataVo.setDataSubGroupVo(dashboardDataSubGroupVo);
		}
	}

	@Override
	public LinkedHashMap<String, Object> getMyExchangeToDashboardGroupDataMap(List<Map<String, Object>> mapList) {
		LinkedHashMap<String, Object> groupDataMap = new LinkedHashMap<>();
		for (Map<String, Object> dataMap : mapList) {
			if(dataMap.containsKey("processTaskStepName")) {
				groupDataMap.put(dataMap.get("processTaskStepName").toString(), dataMap.get("count"));
			}
		}
		return groupDataMap;
	}
}
