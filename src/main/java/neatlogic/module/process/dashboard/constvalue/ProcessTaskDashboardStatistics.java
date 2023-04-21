/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.process.dashboard.constvalue;

import neatlogic.framework.dashboard.constvalue.IDashboardGroupField;
import neatlogic.framework.util.I18nUtils;

public enum ProcessTaskDashboardStatistics implements IDashboardGroupField {
    AVG_HANDLE_COST_TIME("avgCostTime","enum.process.processtaskdashboardstatistics.avg_handle_cost_time"),
    AVG_RESPONSE_COST_TIME("avgResponseCostTime","enum.process.processtaskdashboardstatistics.avg_response_cost_time"),
    RESPONSE_PUNCTUALITY("responsePunctuality","enum.process.processtaskdashboardstatistics.response_punctuality"),
    HANDLE_PUNCTUALITY("handlePunctuality","enum.process.processtaskdashboardstatistics.handle_punctuality"),
    ;

    private final String value;
    private final String text;

    ProcessTaskDashboardStatistics(String _value, String _text){
        value = _value;
        text = _text;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getText() {
        return I18nUtils.getMessage(text);
    }

    public static String getValue(String _value) {
        for (ProcessTaskDashboardStatistics s : ProcessTaskDashboardStatistics.values()) {
            if (s.getValue().equals(_value)) {
                return s.getValue();
            }
        }
        return null;
    }
}