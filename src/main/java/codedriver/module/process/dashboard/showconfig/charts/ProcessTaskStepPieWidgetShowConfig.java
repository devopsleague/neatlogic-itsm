/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.dashboard.showconfig.charts;

import codedriver.framework.common.constvalue.dashboard.ChartType;
import codedriver.module.process.dashboard.showconfig.ProcessTaskStepDashboardWidgetShowConfigBase;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;

@Service
public class ProcessTaskStepPieWidgetShowConfig extends ProcessTaskStepDashboardWidgetShowConfigBase {
    @Override
    public String[] getSupportChart() {
        return new String[] { ChartType.PIECHART.getValue(),ChartType.DONUTCHART.getValue()  };
    }

    @Override
    public JSONArray getStatisticsOptionList() {
        return new JSONArray();
    }

}