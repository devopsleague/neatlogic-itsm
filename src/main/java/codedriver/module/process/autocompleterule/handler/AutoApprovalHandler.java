/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.autocompleterule.handler;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.asynchronization.threadpool.TransactionSynchronizationPool;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.process.autocompleterule.core.IAutoCompleteRuleHandler;
import codedriver.framework.process.constvalue.*;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.*;
import codedriver.framework.process.exception.process.ProcessStepUtilHandlerNotFoundException;
import codedriver.framework.process.service.ProcessTaskAgentService;
import codedriver.framework.process.stephandler.core.*;
import codedriver.framework.service.AuthenticationInfoService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author linbq
 * @since 2021/10/29 16:22
 **/
@Component
public class AutoApprovalHandler implements IAutoCompleteRuleHandler {

    @Resource
    private ProcessTaskMapper processTaskMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Resource
    private ProcessTaskAgentService processTaskAgentService;

    @Override
    public String getHandler() {
        return "autoApproval";
    }

    @Override
    public String getName() {
        return "自动审批";
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public boolean execute(ProcessTaskStepVo currentProcessTaskStepVo) {
        //如果当前步骤有多条可流转路径时，自动审批不生效
        List<Long> nextStepIdList = processTaskMapper.getToProcessTaskStepIdListByFromIdAndType(currentProcessTaskStepVo.getId(), ProcessFlowDirection.FORWARD.getValue());
        if (nextStepIdList.size() != 1) {
            return false;
        }
        List<Long> searchList = new ArrayList<>(2);
        searchList.add(currentProcessTaskStepVo.getId());
        /** 如果当前节点与并行激活兄弟节点中有相同标签的情况，就属于并行审批，当前节点不会自动审批 **/
        List<Long> parallelActivateStepIdList = currentProcessTaskStepVo.getParallelActivateStepIdList();
        if (parallelActivateStepIdList.size() > 0) {
            for (Long parallelActivateStepId : parallelActivateStepIdList) {
                if (Objects.equals(parallelActivateStepId, currentProcessTaskStepVo.getId())) {
                    continue;
                }
                searchList.add(parallelActivateStepId);
                List<Long> tagIdList = processTaskMapper.getSameTagIdListByProcessTaskStepIdList(searchList);
                if (CollectionUtils.isNotEmpty(tagIdList)) {
                    return false;
                }
                searchList.remove(parallelActivateStepId);
            }
        }
        /** 如果上游节点中有两个节点与当前节点存在相同标签的情况，就属于上游并行审批，当前节点不会自动审批 **/
        List<Long> preApprovalStepIdList = new ArrayList<>();
        List<Long> preStepIdList = getPreStepIdList(currentProcessTaskStepVo.getFromProcessTaskStepId());
        for (Long preStepId : preStepIdList) {
            searchList.add(preStepId);
            List<Long> tagIdList = processTaskMapper.getSameTagIdListByProcessTaskStepIdList(searchList);
            if (CollectionUtils.isNotEmpty(tagIdList)) {
                preApprovalStepIdList.add(preStepId);
            }
            searchList.remove(preStepId);
        }
        /** 上游并行审批或没有审批，当前节点都不会自动审批 **/
        if (preApprovalStepIdList.size() != 1) {
            return false;
        }
        String preStepMajorUserUuid = getPreStepMajorUserUuid(preApprovalStepIdList.get(0));
        if (preStepMajorUserUuid != null) {
            ProcessTaskVo processTaskVo = processTaskMapper.getProcessTaskById(currentProcessTaskStepVo.getProcessTaskId());
            List<String> fromUserUuidList = processTaskAgentService.getFromUserUuidListByToUserUuidAndChannelUuid(preStepMajorUserUuid, processTaskVo.getChannelUuid());
            AuthenticationInfoVo authenticationInfoVo = authenticationInfoService.getAuthenticationInfo(fromUserUuidList);
            if (processTaskMapper.checkIsWorker(currentProcessTaskStepVo.getProcessTaskId(), currentProcessTaskStepVo.getId(), ProcessUserType.MAJOR.getValue(), authenticationInfoVo) > 0) {
                UserVo currentUserVo = userMapper.getUserBaseInfoByUuid(preStepMajorUserUuid);
                IProcessStepHandler handler = ProcessStepHandlerFactory.getHandler(currentProcessTaskStepVo.getHandler());
                ProcessStepThread thread = new ProcessStepThread(currentProcessTaskStepVo, currentUserVo) {
                    @Override
                    public void myExecute() {
                        UserContext.init(currentUserVo, SystemUser.SYSTEM.getTimezone());
                        currentProcessTaskStepVo.getParamObj().put("action", "complete");
                        handler.autoComplete(currentProcessTaskStepVo);
                    }
                };
                ProcessTaskStepInOperationVo processTaskStepInOperationVo = new ProcessTaskStepInOperationVo(
                        currentProcessTaskStepVo.getProcessTaskId(),
                        currentProcessTaskStepVo.getId(),
                        ProcessTaskOperationType.STEP_COMPLETE.getValue()
                );
                IProcessStepInternalHandler processStepInternalHandler = ProcessStepInternalHandlerFactory.getHandler(currentProcessTaskStepVo.getHandler());
                if (processStepInternalHandler == null) {
                    throw new ProcessStepUtilHandlerNotFoundException(currentProcessTaskStepVo.getHandler());
                }
                processStepInternalHandler.insertProcessTaskStepInOperation(processTaskStepInOperationVo);
                thread.setSupplier(() -> processTaskMapper.deleteProcessTaskStepInOperationById(processTaskStepInOperationVo.getId()));
                TransactionSynchronizationPool.execute(thread);
                return true;
            }
        }
        return false;
    }

    /**
     * 获取上游节点步骤id列表
     * @param fromProcessTaskStepId
     * @return
     */
    private List<Long> getPreStepIdList(Long fromProcessTaskStepId) {
        List<Long> resultList = new ArrayList<>();
        ProcessTaskStepVo fromProcessTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(fromProcessTaskStepId);
        if (fromProcessTaskStepVo != null) {
            IProcessStepHandler processStepHandler = ProcessStepHandlerFactory.getHandler(fromProcessTaskStepVo.getHandler());
            if (processStepHandler != null) {
                if (processStepHandler.getMode() == ProcessStepMode.MT) {
                    resultList.add(fromProcessTaskStepVo.getId());
                } else if (processStepHandler.getMode() == ProcessStepMode.AT) {
                    List<ProcessTaskStepRelVo> processTaskStepRelList = processTaskMapper.getProcessTaskStepRelByToId(fromProcessTaskStepVo.getId());
                    for (ProcessTaskStepRelVo processTaskStepRelVo : processTaskStepRelList) {
                        if (Objects.equals(processTaskStepRelVo.getIsHit(), 1)) {
                            resultList.addAll(getPreStepIdList(processTaskStepRelVo.getFromProcessTaskStepId()));
                        }
                    }
                }
            }
        }
        return resultList;
    }
    /**
     * 如果上游节点是审批节点，则返回处理人
     * @param preApprovalStepId
     * @return
     */
    private String getPreStepMajorUserUuid(Long preApprovalStepId) {
        ProcessTaskStepVo startStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(preApprovalStepId);
        if (startStepVo != null) {
            if (Objects.equals(startStepVo.getIsActive(), 2)) {
                if (ProcessTaskStatus.SUCCEED.getValue().equals(startStepVo.getStatus())) {
//                    Long tagId = processTagMapper.getProcessTagIdByName("审批");
//                    if (tagId != null) {
//                        ProcessTaskStepTagVo processTaskStepTagVo = new ProcessTaskStepTagVo();
//                        processTaskStepTagVo.setProcessTaskStepId(startStepVo.getId());
//                        processTaskStepTagVo.setTagId(tagId);
//                        if (processTaskMapper.checkProcessTaskStepTagIsExists(processTaskStepTagVo) > 0) {
                            List<ProcessTaskStepUserVo> startStepUserVoList = processTaskMapper.getProcessTaskStepUserByStepId(preApprovalStepId, ProcessUserType.MAJOR.getValue());
                            if (startStepUserVoList.size() == 1) {
                                return startStepUserVoList.get(0).getUserUuid();
                            }
//                        }
//                    }
                }
            }
        }
        return null;
    }

}
