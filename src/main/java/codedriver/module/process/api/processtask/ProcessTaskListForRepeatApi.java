/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.api.processtask;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.process.auth.PROCESS_BASE;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.dao.mapper.ChannelMapper;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.ChannelVo;
import codedriver.framework.process.dto.ProcessTaskSearchVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.service.ProcessTaskService;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/9/14 11:44
 **/
@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ProcessTaskListForRepeatApi extends PrivateApiComponentBase {

    @Resource
    private ProcessTaskMapper processTaskMapper;

    @Resource
    private ProcessTaskService processTaskService;

    @Override
    public String getToken() {
        return "processtask/list/forrepeat";
    }

    @Override
    public String getName() {
        return "查询工单列表（重复工单专用）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊查询"),
            @Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "工单id"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页条目"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")
    })
    @Output({
            @Param(name = "tbodyList", explode = ProcessTaskVo[].class, desc = "工单列表"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "查询工单列表（重复工单专用）")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long processTaskId = paramObj.getLong("processTaskId");
        ProcessTaskVo processTaskVo = processTaskService.checkProcessTaskParamsIsLegal(processTaskId);
        List<Long> processTaskIdList = new ArrayList<>();
        Long repeatGroupId = processTaskMapper.getRepeatGroupIdByProcessTaskId(processTaskId);
        if (repeatGroupId != null) {
            processTaskIdList = processTaskMapper.getProcessTaskIdListByRepeatGroupId(repeatGroupId);
        } else {
            processTaskIdList.add(processTaskId);
        }
        List<ProcessTaskVo> processTaskList = new ArrayList<>();
        ProcessTaskSearchVo processTaskSearchVo = JSON.toJavaObject(paramObj, ProcessTaskSearchVo.class);
        processTaskSearchVo.setExcludeIdList(processTaskIdList);
        processTaskSearchVo.setIncludeChannelUuid(processTaskVo.getChannelUuid());
        processTaskSearchVo.setExcludeStatus(ProcessTaskStatus.DRAFT.getValue());
        int rowNum = processTaskMapper.getProcessTaskCountByKeywordAndChannelUuidList(processTaskSearchVo);
        if (rowNum > 0) {
            processTaskSearchVo.setRowNum(rowNum);
            if (processTaskSearchVo.getCurrentPage() <= processTaskSearchVo.getPageCount()) {
                processTaskList = processTaskMapper.getProcessTaskListByKeywordAndChannelUuidList(processTaskSearchVo);
            }
        }
        return TableResultUtil.getResult(processTaskList, processTaskSearchVo);
    }
}