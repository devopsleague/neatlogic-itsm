package neatlogic.module.process.workcenter.operate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.constvalue.ProcessTaskStatus;
import neatlogic.framework.process.constvalue.ProcessTaskStepOperationType;
import neatlogic.framework.process.dto.ProcessTaskVo;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Comparator;
import java.util.Objects;

public class WorkcenterOperateBuilder {

    private JSONArray operateArray = new JSONArray();

    public JSONArray build() {
        operateArray.sort(Comparator.comparing(obj -> ((JSONObject) obj).getInteger("sort")));
        return operateArray;
    }

    public WorkcenterOperateBuilder setHandleOperate(JSONArray handleArray) {
        int isEnable = 0;
        if (CollectionUtils.isNotEmpty(handleArray)) {
            isEnable = 1;
        }
        JSONObject operateJson = new WorkcenterBuildOperateBuilder().setOperate(ProcessTaskStepOperationType.STEP_WORK).setSort(1)
                .setIsEnable(isEnable).setHandleArray(handleArray).build();
        operateJson.put("text", "处理");
        operateArray.add(operateJson);
        return this;
    }

    public WorkcenterOperateBuilder setAbortRecoverOperate(Boolean isHasAbort, Boolean isHasRecover, ProcessTaskVo processTaskVo) {
        JSONObject operateJson = null;
        JSONObject configJson = new JSONObject();
        if (isHasRecover) {
            configJson.put("taskid", processTaskVo.getId());
            configJson.put("interfaceurl", "api/rest/processtask/recover?processTaskId=" + processTaskVo.getId());
            operateJson = new WorkcenterBuildOperateBuilder().setOperate(ProcessTaskOperationType.PROCESSTASK_RECOVER).setSort(2).setConfig(configJson).setIsEnable(1).build();
        } else {
            if (isHasAbort) {
                configJson.put("taskid", processTaskVo.getId());
                configJson.put("interfaceurl", "api/rest/processtask/abort?processTaskId=" + processTaskVo.getId());
                operateJson = new WorkcenterBuildOperateBuilder().setOperate(ProcessTaskOperationType.PROCESSTASK_ABORT).setSort(2).setConfig(configJson).setIsEnable(1).build();
            } else {
                operateJson = new WorkcenterBuildOperateBuilder().setOperate(ProcessTaskOperationType.PROCESSTASK_ABORT).setSort(2).setIsEnable(0).build();
            }
        }
        operateArray.add(operateJson);
        return this;
    }

    public WorkcenterOperateBuilder setUrgeOperate(Boolean isHasUrge, ProcessTaskVo processTaskVo) {
        JSONObject operateJson = null;
        if (isHasUrge) {
            JSONObject configJson = new JSONObject();
            configJson.put("taskid", processTaskVo.getId());
            configJson.put("interfaceurl", "api/rest/processtask/urge?processTaskId=" + processTaskVo.getId());
            operateJson = new WorkcenterBuildOperateBuilder().setOperate(ProcessTaskOperationType.PROCESSTASK_URGE).setSort(3)
                    .setConfig(configJson).setIsEnable(1).build();
        } else {
            operateJson = new WorkcenterBuildOperateBuilder().setOperate(ProcessTaskOperationType.PROCESSTASK_URGE).setSort(3).setIsEnable(0).build();
        }
        operateArray.add(operateJson);
        return this;
    }

    public WorkcenterOperateBuilder setShowHideOperate(ProcessTaskVo processTaskVo) {
        if (processTaskVo.getParamObj().getBoolean("isHasProcessTaskAuth")) {
            int isShowParam = 1;
            ProcessTaskOperationType type = ProcessTaskOperationType.PROCESSTASK_SHOW;
            if (processTaskVo.getIsShow() == 1) {
                type = ProcessTaskOperationType.PROCESSTASK_HIDE;
                isShowParam = 0;
            }
            JSONObject configJson = new JSONObject();
            configJson.put("taskid", processTaskVo.getId());
            configJson.put("interfaceurl", String.format("api/rest/processtask/show/hide?processTaskId=%s&isShow=%d", processTaskVo.getId(), isShowParam));
            JSONObject operateJson = new WorkcenterBuildOperateBuilder().setOperate(type).setSort(4).setIsEnable(1).setConfig(configJson).build();
            operateArray.add(operateJson);
        }
        return this;
    }

    public WorkcenterOperateBuilder setDeleteOperate(ProcessTaskVo processTaskVo) {
        //工单管理权限 || 草稿状态的工单且上报人是当前登录人
        if (processTaskVo.getParamObj().getBoolean("isHasProcessTaskAuth") || (Objects.equals(ProcessTaskStatus.DRAFT.getValue(), processTaskVo.getStatus()) && Objects.equals(processTaskVo.getOwner(), UserContext.get().getUserUuid(true)))) {
            JSONObject configJson = new JSONObject();
            configJson.put("taskid", processTaskVo.getId());
            configJson.put("interfaceurl", String.format("api/rest/processtask/delete?processTaskId=%s", processTaskVo.getId()));
            JSONObject operateJson = new WorkcenterBuildOperateBuilder().setOperate(ProcessTaskOperationType.PROCESSTASK_DELETE).setSort(5).setIsEnable(1).setConfig(configJson).build();
            operateArray.add(operateJson);
        }
        return this;
    }
}
