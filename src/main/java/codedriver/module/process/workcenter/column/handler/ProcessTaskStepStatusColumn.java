/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.workcenter.column.handler;

import codedriver.framework.dashboard.dto.DashboardWidgetAllGroupDefineVo;
import codedriver.framework.dashboard.dto.DashboardWidgetChartConfigVo;
import codedriver.framework.dashboard.dto.DashboardWidgetGroupDefineVo;
import codedriver.framework.process.column.core.IProcessTaskColumn;
import codedriver.framework.process.column.core.ProcessTaskColumnBase;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.workcenter.dto.SelectColumnVo;
import codedriver.framework.process.workcenter.dto.TableSelectColumnVo;
import codedriver.framework.process.workcenter.table.ProcessTaskStepSqlTable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ProcessTaskStepStatusColumn extends ProcessTaskColumnBase implements IProcessTaskColumn {
    @Override
    public String getName() {
        return "stepstatus";
    }

    @Override
    public String getDisplayName() {
        return "步骤状态";
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
        return 5;
    }

    @Override
    public Object getValue(ProcessTaskVo processTaskVo) {
        return null;
    }

    @Override
    public Boolean getMyIsShow() {
        return false;
    }

    @Override
    public Boolean getDisabled() {
        return true;
    }

    @Override
    public List<TableSelectColumnVo> getTableSelectColumn() {
        return new ArrayList<TableSelectColumnVo>() {
            {
                add(new TableSelectColumnVo(new ProcessTaskStepSqlTable(), Collections.singletonList(
                        new SelectColumnVo(ProcessTaskStepSqlTable.FieldEnum.STATUS.getValue(), ProcessTaskStepSqlTable.FieldEnum.STATUS.getProName(), true)
                )));
            }
        };
    }

    @Override
    public void getMyDashboardAllGroupDefine(DashboardWidgetAllGroupDefineVo dashboardWidgetAllGroupDefineVo, List<Map<String, Object>> mapList) {
        //补充text
        for (int i = 0; i < mapList.size(); i++) {
            Map<String, Object> tmpMap = new HashMap<>();
            Map<String, Object> map = mapList.get(i);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();
                if (key.equals(ProcessTaskStepSqlTable.FieldEnum.STATUS.getProName())) {
                    tmpMap.put("statusText", ProcessTaskStatus.getText(value));
                }
                tmpMap.put(key, value);
            }
            mapList.set(i, tmpMap);
        }
        DashboardWidgetChartConfigVo dashboardWidgetChartConfigVo = dashboardWidgetAllGroupDefineVo.getChartConfigVo();
        if (getName().equals(dashboardWidgetChartConfigVo.getGroup())) {
            DashboardWidgetGroupDefineVo dashboardDataGroupVo = new DashboardWidgetGroupDefineVo(ProcessTaskStepSqlTable.FieldEnum.STATUS.getProName(), dashboardWidgetChartConfigVo.getGroup(), "statusText");
            dashboardWidgetAllGroupDefineVo.setGroupDefineVo(dashboardDataGroupVo);
        }
        //如果存在子分组
        if (getName().equals(dashboardWidgetChartConfigVo.getSubGroup())) {
            DashboardWidgetGroupDefineVo dashboardDataSubGroupVo = new DashboardWidgetGroupDefineVo(ProcessTaskStepSqlTable.FieldEnum.STATUS.getProName(), dashboardWidgetChartConfigVo.getSubGroup(), "statusText");
            dashboardWidgetAllGroupDefineVo.setSubGroupDefineVo(dashboardDataSubGroupVo);
        }
    }

    @Override
    public LinkedHashMap<String, Object> getMyExchangeToDashboardGroupDataMap(List<Map<String, Object>> mapList) {
        LinkedHashMap<String, Object> groupDataMap = new LinkedHashMap<>();
        for (Map<String, Object> dataMap : mapList) {
            if(dataMap.containsKey(ProcessTaskStepSqlTable.FieldEnum.STATUS.getProName())) {
                groupDataMap.put(dataMap.get(ProcessTaskStepSqlTable.FieldEnum.STATUS.getProName()).toString(), dataMap.get("count"));
            }
        }
        return groupDataMap;
    }
}
