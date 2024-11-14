/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.process.steptaskhandler.component;

import neatlogic.framework.process.constvalue.ProcessStepHandlerType;
import neatlogic.framework.process.constvalue.ProcessUserType;
import neatlogic.framework.process.dto.ProcessTaskStepTaskUserVo;
import neatlogic.framework.process.dto.ProcessTaskStepTaskVo;
import neatlogic.framework.process.dto.ProcessTaskStepUserVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.steptaskhandler.core.ProcessStepTaskHandlerBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SubTaskHandler extends ProcessStepTaskHandlerBase {
    @Override
    public List<String> getHandlerList() {
        List<String> list = new ArrayList<>();
        list.add(ProcessStepHandlerType.OMNIPOTENT.getHandler());
        list.add("event");
        return list;
    }

    @Override
    public List<ProcessTaskStepUserVo> getMinorUserListForNotifyReceiver(ProcessTaskStepVo currentProcessTaskStepVo) {
        List<ProcessTaskStepUserVo> resultList = new ArrayList<>();
        /* 当前任务处理人 */
        ProcessTaskStepTaskVo stepTaskVo = currentProcessTaskStepVo.getProcessTaskStepTaskVo();
        if (stepTaskVo != null) {
            List<ProcessTaskStepTaskUserVo> taskUserVoList = stepTaskVo.getStepTaskUserVoList();
            if (CollectionUtils.isNotEmpty(taskUserVoList)) {
                for (ProcessTaskStepTaskUserVo taskUserVo : taskUserVoList) {
                    ProcessTaskStepUserVo processTaskStepUserVo = new ProcessTaskStepUserVo();
                    processTaskStepUserVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
                    processTaskStepUserVo.setProcessTaskStepId(currentProcessTaskStepVo.getId());
                    processTaskStepUserVo.setUserType(ProcessUserType.MINOR.getValue());
                    processTaskStepUserVo.setUserUuid(taskUserVo.getUserUuid());
                    resultList.add(processTaskStepUserVo);
                }
            }
        }
        return resultList;
    }
}
