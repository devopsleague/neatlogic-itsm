package neatlogic.module.process.api.processtask;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.form.attribute.core.FormAttributeDataConversionHandlerFactory;
import neatlogic.framework.form.attribute.core.IFormAttributeDataConversionHandler;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.process.auth.PROCESS_BASE;
import neatlogic.framework.process.constvalue.ItsmTenantConfig;
import neatlogic.framework.process.constvalue.ProcessTaskOperationType;
import neatlogic.framework.process.dto.ProcessTaskFormAttributeDataVo;
import neatlogic.framework.process.dto.ProcessTaskScoreTemplateVo;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskVo;
import neatlogic.framework.process.exception.operationauth.ProcessTaskPermissionDeniedException;
import neatlogic.framework.process.operationauth.core.ProcessAuthManager;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.process.common.config.ProcessConfig;
import neatlogic.module.process.dao.mapper.processtask.ProcessTaskMapper;
import neatlogic.module.process.dao.mapper.score.ScoreTemplateMapper;
import neatlogic.module.process.service.ProcessTaskService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Phaser;

@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ProcessTaskStepGetApi extends PrivateApiComponentBase {

    @Resource
    private ProcessTaskMapper processTaskMapper;

    @Resource
    private ProcessTaskService processTaskService;

    @Resource
    private ScoreTemplateMapper scoreTemplateMapper;

    @Override
    public String getToken() {
        return "processtask/step/get";
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Override
    public String getName() {
        return "nmpap.processtaskstepgetapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "term.itsm.processtaskid"),
            @Param(name = "processTaskStepId", type = ApiParamType.LONG, desc = "term.itsm.processtaskstepid")})
    @Output({@Param(name = "processTask", explode = ProcessTaskVo.class, desc = "term.itsm.processtaskinfo")})
    @Description(desc = "nmpap.processtaskstepgetapi.description.desc")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long processTaskId = jsonObj.getLong("processTaskId");
        Long processTaskStepId = jsonObj.getLong("processTaskStepId");
        ProcessTaskVo processTaskVo = processTaskService.checkProcessTaskParamsIsLegal(processTaskId, processTaskStepId);
        ProcessAuthManager.Builder builder = new ProcessAuthManager.Builder()
                .addProcessTaskId(processTaskId)
                .addOperationType(ProcessTaskOperationType.PROCESSTASK_VIEW)
                .addOperationType(ProcessTaskOperationType.PROCESSTASK_FOCUSUSER_UPDATE)
                .addOperationType(ProcessTaskOperationType.PROCESSTASK_SCORE);
        if (processTaskStepId != null) {
            builder.addProcessTaskStepId(processTaskStepId)
                    .addOperationType(ProcessTaskOperationType.STEP_VIEW)
                    .addOperationType(ProcessTaskOperationType.STEP_SAVE)
                    .addOperationType(ProcessTaskOperationType.STEP_COMPLETE);
        }
        ProcessAuthManager processAuthManager = builder.build();
        Map<Long, Set<ProcessTaskOperationType>> operationTypeSetMap = processAuthManager.getOperateMap();

        Set<ProcessTaskOperationType> taskOperationTypeSet = operationTypeSetMap.get(processTaskId);
        if (!taskOperationTypeSet.contains(ProcessTaskOperationType.PROCESSTASK_VIEW)) {
            ProcessTaskPermissionDeniedException exception = processAuthManager.getProcessTaskPermissionDeniedException(processTaskId, ProcessTaskOperationType.PROCESSTASK_VIEW);
            if (exception != null) {
                throw new PermissionDeniedException(exception.getMessage());
            }
//            if (ProcessTaskStatus.DRAFT.getValue().equals(processTaskVo.getStatus())) {
//                throw new ProcessTaskViewDeniedException();
//            } else {
//                ChannelVo channelVo = channelMapper.getChannelByUuid(processTaskVo.getChannelUuid());
//                if (channelVo == null) {
//                    throw new ChannelNotFoundException(processTaskVo.getChannelUuid());
//                }
//                throw new ProcessTaskViewDeniedException(channelVo.getName());
//            }
        }
        Phaser phaser = new Phaser();
        phaser.register();
        NeatLogicThread ProcessTaskDetailThread = new NeatLogicThread("PROCESSTASK_DETAIL_" + processTaskId, true) {
            @Override
            protected void execute() {
                try {
                    processTaskService.setProcessTaskDetail(processTaskVo);
                    JSONObject formConfig = processTaskVo.getFormConfig();
                    if (MapUtils.isNotEmpty(formConfig)) {
                        FormVersionVo formVersionVo = new FormVersionVo();
                        String mainSceneUuid = formConfig.getString("uuid");
                        formVersionVo.setSceneUuid(mainSceneUuid);
                        formVersionVo.setFormConfig(formConfig);
                        Map<String, FormAttributeVo> formAttributeVoMap = new HashMap<>();
                        List<FormAttributeVo> formAttributeList = formVersionVo.getFormAttributeList();
                        for (FormAttributeVo formAttributeVo : formAttributeList) {
                            formAttributeVoMap.put(formAttributeVo.getUuid(), formAttributeVo);
                        }
                        List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = processTaskVo.getProcessTaskFormAttributeDataList();
                        for (ProcessTaskFormAttributeDataVo processTaskFormAttributeDataVo : processTaskFormAttributeDataList) {
                            FormAttributeVo formAttributeVo = formAttributeVoMap.get(processTaskFormAttributeDataVo.getAttributeUuid());
                            if (formAttributeVo != null) {
                                IFormAttributeDataConversionHandler formAttributeDataConversionHandler = FormAttributeDataConversionHandlerFactory.getHandler(formAttributeVo.getHandler());
                                if (formAttributeDataConversionHandler != null) {
                                    Object dataObj = formAttributeDataConversionHandler.passwordMask(processTaskFormAttributeDataVo.getDataObj(), formAttributeVo.getConfig());
                                    processTaskFormAttributeDataVo.setDataObj(dataObj);
                                }
                            }
                            processTaskVo.getFormAttributeDataMap().put(processTaskFormAttributeDataVo.getAttributeUuid(), processTaskFormAttributeDataVo.getDataObj());
                        }
                    }
                } finally {
                    phaser.arrive();
                }
            }
        };
        CachedThreadPool.execute(ProcessTaskDetailThread);

        phaser.register();
        NeatLogicThread startProcessTaskStepThread = new NeatLogicThread("START_PROCESSTASKSTEP_" + processTaskId, true) {
            @Override
            protected void execute() {
                try {
                    processTaskVo.setStartProcessTaskStep(processTaskService.getStartProcessTaskStepByProcessTaskId(processTaskId));
                } finally {
                    phaser.arrive();
                }
            }
        };
        CachedThreadPool.execute(startProcessTaskStepThread);

        ProcessTaskStepVo currentProcessTaskStepVo = processTaskVo.getCurrentProcessTaskStep();
        if (currentProcessTaskStepVo != null) {
            Set<ProcessTaskOperationType> stepOperationTypeSet = operationTypeSetMap.get(processTaskStepId);
            if (stepOperationTypeSet.contains(ProcessTaskOperationType.STEP_VIEW)) {
                phaser.register();
                NeatLogicThread currentProcessTaskStepDetailThread = new NeatLogicThread("CURRENT_PROCESSTASKSTEP_DETAIL_" + processTaskStepId, true) {
                    @Override
                    protected void execute() {
                        try {
                            processTaskService.getCurrentProcessTaskStepDetail(currentProcessTaskStepVo, stepOperationTypeSet.contains(ProcessTaskOperationType.STEP_COMPLETE));
                        } finally {
                            phaser.arrive();
                        }
                    }
                };
                CachedThreadPool.execute(currentProcessTaskStepDetailThread);
            }
        }

        /* 查询当前用户是否有权限修改工单关注人 **/
        if (taskOperationTypeSet.contains(ProcessTaskOperationType.PROCESSTASK_FOCUSUSER_UPDATE)) {
            processTaskVo.setCanEditFocusUser(1);
        } else {
            processTaskVo.setCanEditFocusUser(0);
        }

        if (taskOperationTypeSet.contains(ProcessTaskOperationType.PROCESSTASK_SCORE)) {
            ProcessTaskScoreTemplateVo processTaskScoreTemplateVo = processTaskMapper.getProcessTaskScoreTemplateByProcessTaskId(processTaskId);
            if (processTaskScoreTemplateVo != null) {
                processTaskVo.setScoreTemplateVo(scoreTemplateMapper.getScoreTemplateById(processTaskScoreTemplateVo.getScoreTemplateId()));
                ProcessTaskStepVo endProcessTaskStepVo = processTaskMapper.getEndProcessTaskStepByProcessTaskId(processTaskId);
                List<ProcessTaskStepVo> processTaskStepVoList = processTaskService.getBackwardNextStepListByProcessTaskStepId(endProcessTaskStepVo);
                processTaskVo.setRedoStepList(processTaskStepVoList);
            }
        }

        // TODO 兼容老工单表单（判断是否存在旧表单）
        Map<String, String> oldFormPropMap = processTaskMapper.getProcessTaskOldFormAndPropByTaskId(processTaskId);
        if (oldFormPropMap != null && oldFormPropMap.size() > 0) {
            processTaskVo.setIsHasOldFormProp(1);
        }
        // 移动端默认展开表单
        processTaskVo.setMobileFormUIType(Integer.valueOf(ProcessConfig.MOBILE_FORM_UI_TYPE()));
        phaser.awaitAdvance(0);
        if (currentProcessTaskStepVo != null) {
            Set<ProcessTaskOperationType> stepOperationTypeSet = operationTypeSetMap.get(processTaskStepId);
            if (stepOperationTypeSet.contains(ProcessTaskOperationType.STEP_SAVE)) {
                // 回复框内容和附件暂存回显
                processTaskService.setTemporaryData(processTaskVo, currentProcessTaskStepVo);
            }
        }
        String processTaskBaseInfoIsShow = ConfigManager.getConfig(ItsmTenantConfig.PROCESS_TASK_BASE_INFO_IS_SHOW);
        if (StringUtils.isNotBlank(processTaskBaseInfoIsShow)) {
            processTaskVo.setIsShowBaseInfo(Integer.valueOf(processTaskBaseInfoIsShow));
        }
        String processTaskStepCommentEditorToolbarIsShow = ConfigManager.getConfig(ItsmTenantConfig.PROCESS_TASK_STEP_COMMENT_EDITOR_TOOLBAR_IS_SHOW);
        if (StringUtils.isNotBlank(processTaskStepCommentEditorToolbarIsShow)) {
            processTaskVo.setIsShowProcessTaskStepCommentEditorToolbar(Integer.valueOf(processTaskStepCommentEditorToolbarIsShow));
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("processTask", processTaskVo);
        resultObj.put("processTaskRelationCount", processTaskMapper.getProcessTaskRelationCountByProcessTaskId(processTaskVo.getId()));
        return resultObj;
    }

}
