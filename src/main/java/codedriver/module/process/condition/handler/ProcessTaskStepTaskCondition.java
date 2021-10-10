package codedriver.module.process.condition.handler;

import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.process.condition.core.IProcessTaskCondition;
import codedriver.framework.process.condition.core.ProcessTaskConditionBase;
import codedriver.framework.process.constvalue.ConditionConfigType;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.framework.process.dao.mapper.task.TaskMapper;
import codedriver.framework.process.dto.ProcessTaskStepTaskVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.module.process.service.ProcessTaskStepTaskService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ProcessTaskStepTaskCondition extends ProcessTaskConditionBase implements IProcessTaskCondition {
    @Resource
    TaskMapper taskMapper;
    @Resource
    ProcessTaskStepTaskService processTaskStepTaskService;

    @Override
    public String getName() {
        return "steptask";
    }

    @Override
    public String getDisplayName() {
        return "子任务";
    }

    @Override
    public String getHandler(FormConditionModel processWorkcenterConditionType) {
        return FormHandlerType.SELECT.toString();
    }

    @Override
    public String getType() {
        return ProcessFieldType.COMMON.getValue();
    }

    @Override
    public JSONObject getConfig(ConditionConfigType type) {
        JSONObject config = new JSONObject();
        config.put("type", FormHandlerType.SELECT.toString());
        config.put("search", true);
        config.put("dynamicUrl", "/api/rest/task/search");
        config.put("rootName", "tbodyList");
        config.put("valueName", "id");
        config.put("textName", "name");
        config.put("multiple", true);
        config.put("value", "");
        config.put("defaultValue", "");
        /** 以下代码是为了兼容旧数据结构，前端有些地方还在用 **/
        config.put("isMultiple", true);
        JSONObject mappingObj = new JSONObject();
        mappingObj.put("value", "uuid");
        mappingObj.put("text", "name");
        config.put("mapping", mappingObj);
        return config;
    }

    @Override
    public Integer getSort() {
        return 19;
    }

    @Override
    public ParamType getParamType() {
        return ParamType.ARRAY;
    }

    @Override
    protected String getMyEsWhere(Integer index, List<ConditionVo> conditionList) {
        return null;
    }


    @Override
    public Object valueConversionText(Object value, JSONObject config) {

        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {

    }

    @Override
    public Object getConditionParamData(ProcessTaskVo processTaskVo){
        List<Long> dataList = new ArrayList<>();
        ProcessTaskStepVo currentTaskStepVo = processTaskVo.getCurrentProcessTaskStep();
        if(currentTaskStepVo != null) {
            ProcessTaskStepTaskVo stepTaskVo = currentTaskStepVo.getProcessTaskStepTaskVo();
            if (stepTaskVo != null) {
                dataList = Collections.singletonList(stepTaskVo.getTaskConfigId());
            }
        }
        return dataList;
    }
}