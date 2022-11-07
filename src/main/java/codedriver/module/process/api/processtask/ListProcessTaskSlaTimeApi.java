/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.api.processtask;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.auth.PROCESS_BASE;
import codedriver.framework.process.dto.ProcessTaskSlaTimeVo;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.process.service.ProcessTaskService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListProcessTaskSlaTimeApi extends PrivateApiComponentBase {

    @Resource
    private ProcessTaskService processTaskService;

    @Override
    public String getToken() {
        return "processtask/slatime/list";
    }

    @Override
    public String getName() {
        return "工单时效列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "slaIdList", type = ApiParamType.JSONARRAY, isRequired = true, minSize = 1, desc = "时效ID列表")
    })
    @Output({
            @Param(name = "tbodyList", explode = ProcessTaskSlaTimeVo[].class, desc = "时效列表")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray slaIdArray = paramObj.getJSONArray("slaIdList");
        List<Long> slaIdList = slaIdArray.toJavaList(Long.class);
        List<ProcessTaskSlaTimeVo> processTaskSlaTimeList = processTaskService.getSlaTimeListBySlaIdList(slaIdList);
        return TableResultUtil.getResult(processTaskSlaTimeList);
    }
}