/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.api.processtask;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.service.ProcessTaskService;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/9/14 11:44
 **/
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
        JSONObject resultObj = new JSONObject();
        Long processTaskId = paramObj.getLong("processTaskId");
        ProcessTaskVo processTaskVo = processTaskService.checkProcessTaskParamsIsLegal(processTaskId);
        List<Long> processTaskIdList = new ArrayList<>();
        Long repeatGroupId = processTaskMapper.getRepeatGroupIdByProcessTaskId(processTaskId);
        if (repeatGroupId != null) {
            processTaskIdList = processTaskMapper.getProcessTaskIdListByRepeatGroupId(repeatGroupId);
        }
        processTaskIdList.add(processTaskId);
        List<String> channelUuidList = new ArrayList<>();
        channelUuidList.add(processTaskVo.getChannelUuid());
        BasePageVo basePageVo = JSON.toJavaObject(paramObj, BasePageVo.class);
        int rowNum = processTaskMapper.getProcessTaskCountByKeywordAndChannelUuidList(basePageVo, processTaskIdList, channelUuidList);
        if (rowNum > 0) {
            basePageVo.setRowNum(rowNum);
            if (basePageVo.getCurrentPage() <= basePageVo.getPageCount()) {
                List<ProcessTaskVo> processTaskList = processTaskMapper.getProcessTaskListByKeywordAndChannelUuidList(basePageVo, processTaskIdList, channelUuidList);
                resultObj.put("tbodyList", processTaskList);
            }
        } else {
            resultObj.put("tbodyList", new ArrayList<>());
        }
        resultObj.put("currentPage", basePageVo.getCurrentPage());
        resultObj.put("pageSize", basePageVo.getPageSize());
        resultObj.put("pageCount", basePageVo.getPageCount());
        resultObj.put("rowNum", rowNum);
        return resultObj;
    }
}
