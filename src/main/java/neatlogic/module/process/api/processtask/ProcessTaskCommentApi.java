package neatlogic.module.process.api.processtask;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.file.FileNotFoundException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.process.auth.PROCESS_BASE;
import neatlogic.framework.process.constvalue.ProcessTaskAuditType;
import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.constvalue.ProcessTaskStepDataType;
import neatlogic.framework.process.constvalue.ProcessTaskStepOperationType;
import neatlogic.framework.process.crossover.IProcessTaskCommentApiCrossoverService;
import neatlogic.framework.process.dto.*;
import neatlogic.framework.process.notify.constvalue.ProcessTaskStepNotifyTriggerType;
import neatlogic.framework.process.operationauth.core.ProcessAuthManager;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.process.dao.mapper.process.ProcessCommentTemplateMapper;
import neatlogic.module.process.dao.mapper.processtask.ProcessTaskMapper;
import neatlogic.module.process.dao.mapper.processtask.ProcessTaskStepDataMapper;
import neatlogic.module.process.service.IProcessStepHandlerUtil;
import neatlogic.module.process.service.ProcessTaskService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
@AuthAction(action = PROCESS_BASE.class)
public class ProcessTaskCommentApi extends PrivateApiComponentBase implements IProcessTaskCommentApiCrossoverService {

    @Autowired
    private ProcessTaskMapper processTaskMapper;

    @Autowired
    private ProcessTaskService processTaskService;

    @Autowired
    private ProcessTaskStepDataMapper processTaskStepDataMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private ProcessCommentTemplateMapper commentTemplateMapper;

    @Autowired
    private IProcessStepHandlerUtil processStepHandlerUtil;

    @Override
    public String getToken() {
        return "processtask/comment";
    }

    @Override
    public String getName() {
        return "工单回复接口";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "工单id"),
        @Param(name = "processTaskStepId", type = ApiParamType.LONG, isRequired = true, desc = "步骤id"),
        @Param(name = "content", type = ApiParamType.STRING, desc = "描述"),
        @Param(name = "source", type = ApiParamType.STRING, defaultValue = "pc", desc = "来源"),
        @Param(name = "fileIdList", type = ApiParamType.JSONARRAY, desc = "附件id列表"),
        @Param(name = "commentTemplateId", type = ApiParamType.LONG, desc = "回复模版ID")})
    @Output({@Param(name = "commentList", explode = ProcessTaskStepReplyVo[].class, desc = "当前步骤评论列表")})
    @Description(desc = "工单回复接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long processTaskId = jsonObj.getLong("processTaskId");
        Long processTaskStepId = jsonObj.getLong("processTaskStepId");
        Long commentTemplateId = jsonObj.getLong("commentTemplateId");
        ProcessTaskVo processTaskVo =
            processTaskService.checkProcessTaskParamsIsLegal(processTaskId, processTaskStepId);
        processTaskMapper.getProcessTaskLockById(processTaskId);
        ProcessTaskStepVo processTaskStepVo = processTaskVo.getCurrentProcessTaskStep();
        new ProcessAuthManager.StepOperationChecker(processTaskStepId, ProcessTaskStepOperationType.STEP_COMMENT)
                .build()
                .checkAndNoPermissionThrowException();
        // 删除暂存
        ProcessTaskStepDataVo processTaskStepDataVo = new ProcessTaskStepDataVo();
        processTaskStepDataVo.setProcessTaskId(processTaskId);
        processTaskStepDataVo.setProcessTaskStepId(processTaskStepId);
        processTaskStepDataVo.setFcu(UserContext.get().getUserUuid(true));
        processTaskStepDataVo.setType(ProcessTaskStepDataType.STEPDRAFTSAVE.getValue());
        ProcessTaskStepDataVo stepDraftSaveData =
            processTaskStepDataMapper.getProcessTaskStepData(processTaskStepDataVo);
        if (stepDraftSaveData != null) {
            JSONObject dataObj = stepDraftSaveData.getData();
            if (MapUtils.isNotEmpty(dataObj)) {
                dataObj.remove("content");
                dataObj.remove("fileIdList");
            }
            if (MapUtils.isNotEmpty(dataObj)) {
                processTaskStepDataMapper.replaceProcessTaskStepData(stepDraftSaveData);
            } else {
                processTaskStepDataMapper.deleteProcessTaskStepData(stepDraftSaveData);
            }
        }

        String content = jsonObj.getString("content");
        List<Long> fileIdList = JSON.parseArray(JSON.toJSONString(jsonObj.getJSONArray("fileIdList")), Long.class);
        if (StringUtils.isBlank(content) && CollectionUtils.isEmpty(fileIdList)) {
            return null;
        }

        ProcessTaskStepContentVo processTaskStepContentVo = new ProcessTaskStepContentVo();
        processTaskStepContentVo.setProcessTaskId(processTaskId);
        processTaskStepContentVo.setProcessTaskStepId(processTaskStepId);
        processTaskStepContentVo.setType(ProcessTaskStepOperationType.STEP_COMMENT.getValue());
        if (StringUtils.isNotBlank(content)) {
            ProcessTaskContentVo contentVo = new ProcessTaskContentVo(content);
            processTaskMapper.insertIgnoreProcessTaskContent(contentVo);
            processTaskStepContentVo.setContentHash(contentVo.getHash());
        }
        String source = jsonObj.getString("source");
        if (StringUtils.isNotBlank(source)) {
            processTaskStepContentVo.setSource(source);
        }
        processTaskMapper.insertProcessTaskStepContent(processTaskStepContentVo);

        /** 保存附件uuid **/
        if (CollectionUtils.isNotEmpty(fileIdList)) {
            ProcessTaskStepFileVo processTaskStepFileVo = new ProcessTaskStepFileVo();
            processTaskStepFileVo.setProcessTaskId(processTaskId);
            processTaskStepFileVo.setProcessTaskStepId(processTaskStepId);
            processTaskStepFileVo.setContentId(processTaskStepContentVo.getId());
            for (Long fileId : fileIdList) {
                if (fileMapper.getFileById(fileId) == null) {
                    throw new FileNotFoundException(fileId);
                }
                processTaskStepFileVo.setFileId(fileId);
                processTaskMapper.insertProcessTaskStepFile(processTaskStepFileVo);
            }
        }
        /** 记录回复模版使用次数 */
        if (commentTemplateId != null) {
            ProcessCommentTemplateUseCountVo templateUseCount =
                commentTemplateMapper.getTemplateUseCount(commentTemplateId, UserContext.get().getUserUuid());
            if (templateUseCount != null) {
                commentTemplateMapper.updateTemplateUseCount(commentTemplateId, UserContext.get().getUserUuid());
            } else {
                commentTemplateMapper.insertTemplateUseCount(commentTemplateId, UserContext.get().getUserUuid());
            }
        }

        // 生成活动
        processTaskStepVo.getParamObj().putAll(jsonObj);
        processStepHandlerUtil.audit(processTaskStepVo, ProcessTaskAuditType.COMMENT);
        processStepHandlerUtil.notify(processTaskStepVo, ProcessTaskStepNotifyTriggerType.COMMENT);
        processStepHandlerUtil.action(processTaskStepVo, ProcessTaskStepNotifyTriggerType.COMMENT);
        JSONObject resultObj = new JSONObject();
        List<String> typeList = new ArrayList<>();
        typeList.add(ProcessTaskStepOperationType.STEP_COMMENT.getValue());
        typeList.add(ProcessTaskStepOperationType.STEP_COMPLETE.getValue());
        typeList.add(ProcessTaskStepOperationType.STEP_BACK.getValue());
        typeList.add(ProcessTaskOperationType.PROCESSTASK_RETREAT.getValue());
        typeList.add(ProcessTaskOperationType.PROCESSTASK_TRANSFER.getValue());
        typeList.add(ProcessTaskStepOperationType.STEP_REAPPROVAL.getValue());
        typeList.add(ProcessTaskOperationType.PROCESSTASK_START.getValue());
        resultObj.put("commentList",
            processTaskService.getProcessTaskStepReplyListByProcessTaskStepId(processTaskStepId, typeList));
        return resultObj;
    }

}
