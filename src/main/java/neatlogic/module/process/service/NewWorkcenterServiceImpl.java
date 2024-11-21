/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.process.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.batch.BatchRunner;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.config.ConfigManager;
import neatlogic.framework.dao.mapper.RoleMapper;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.RoleVo;
import neatlogic.framework.dto.TeamVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.form.dao.mapper.FormMapper;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.process.auth.PROCESSTASK_MODIFY;
import neatlogic.framework.process.column.core.IProcessTaskColumn;
import neatlogic.framework.process.column.core.ProcessTaskColumnFactory;
import neatlogic.framework.process.constvalue.*;
import neatlogic.framework.process.dto.*;
import neatlogic.framework.process.operationauth.core.IOperationType;
import neatlogic.framework.process.operationauth.core.ProcessAuthManager;
import neatlogic.framework.process.stephandler.core.IProcessStepHandler;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerFactory;
import neatlogic.framework.process.workcenter.dto.WorkcenterTheadVo;
import neatlogic.framework.process.workcenter.dto.WorkcenterVo;
import neatlogic.framework.process.workcenter.table.ProcessTaskSqlTable;
import neatlogic.framework.process.workcenter.table.constvalue.ProcessSqlTypeEnum;
import neatlogic.framework.util.$;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.process.dao.mapper.catalog.ChannelMapper;
import neatlogic.module.process.dao.mapper.processtask.ProcessTaskMapper;
import neatlogic.module.process.dao.mapper.processtask.ProcessTaskStepTaskMapper;
import neatlogic.module.process.dao.mapper.workcenter.WorkcenterMapper;
import neatlogic.module.process.sql.decorator.SqlBuilder;
import neatlogic.module.process.workcenter.operate.WorkcenterOperateBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NewWorkcenterServiceImpl implements NewWorkcenterService {

    //Logger logger = LoggerFactory.getLogger(NewWorkcenterServiceImpl.class);

    @Resource
    WorkcenterMapper workcenterMapper;

    @Resource
    FormMapper formMapper;

    @Resource
    ChannelMapper channelMapper;

    @Resource
    ProcessTaskMapper processTaskMapper;

    @Resource
    UserMapper userMapper;
    @Resource
    RoleMapper roleMapper;
    @Resource
    TeamMapper teamMapper;
    @Resource
    ProcessTaskStepTaskMapper processTaskStepTaskMapper;

    @Override
    public JSONObject doSearch(WorkcenterVo workcenterVo) {
        //long theadStartTime = System.currentTimeMillis();
        List<JSONObject> dataList = Collections.synchronizedList(new ArrayList<>());//线程安全
        //JSONArray sortColumnList = new JSONArray();
        Map<String, IProcessTaskColumn> columnComponentMap = ProcessTaskColumnFactory.columnComponentMap;
        //补充工单字段信息
        List<WorkcenterTheadVo> theadList = getWorkcenterTheadList(workcenterVo, columnComponentMap);
        theadList = theadList.stream().sorted(Comparator.comparing(WorkcenterTheadVo::getSort)).collect(Collectors.toList());
        workcenterVo.setTheadVoList(theadList);
        //System.out.println((System.currentTimeMillis() - theadStartTime) + " ##end workcenter-thead:------------------------------------------------------------------------------- ");
        //统计符合条件工单数量
        //long countStartTime = System.currentTimeMillis();
        SqlBuilder sb = new SqlBuilder(workcenterVo, ProcessSqlTypeEnum.LIMIT_COUNT);
        //System.out.println(sb.build());
        int offsetRowNum = processTaskMapper.getProcessTaskCountBySql(sb.build());
        workcenterVo.setOffsetRowNum(offsetRowNum);
        //int total = processTaskMapper.getProcessTaskCountBySql(sb.build());
        //System.out.println((System.currentTimeMillis() - countStartTime) + " ##end workcenter-count:------------------------------------------------------------------------------- ");
        //if (total > 0) {
        if (offsetRowNum > 0) {
            //找出符合条件分页后的工单ID List
            //long idStartTime = System.currentTimeMillis();
            sb = new SqlBuilder(workcenterVo, ProcessSqlTypeEnum.DISTINCT_ID);
            // System.out.println(sb.build());
            List<ProcessTaskVo> processTaskList = processTaskMapper.getProcessTaskBySql(sb.build());
            workcenterVo.setProcessTaskIdList(processTaskList.stream().map(ProcessTaskVo::getId).collect(Collectors.toList()));
            //System.out.println((System.currentTimeMillis() - idStartTime) + " ##end workcenter-id:-------------------------------------------------------------------------------");

            //long detailStartTime = System.currentTimeMillis();
            sb = new SqlBuilder(workcenterVo, ProcessSqlTypeEnum.FIELD);
            // System.out.println(sb.build());
            List<ProcessTaskVo> processTaskVoList = processTaskMapper.getProcessTaskBySql(sb.build());
            //System.out.println((System.currentTimeMillis() - detailStartTime) + " ##end workcenter-detail:-------------------------------------------------------------------------------");
            //long columnTime = System.currentTimeMillis();
            //纠正顺序
            for (int i = 0; i < processTaskVoList.size(); i++) {
                ProcessTaskVo processTaskVo = processTaskVoList.get(i);
                processTaskVo.setIndex(i);
            }
            BatchRunner<ProcessTaskVo> runner = new BatchRunner<>();
            List<JSONObject> finalDataList = dataList;
            runner.execute(processTaskVoList, 3, (threadIndex, dataIndex, processTaskVo) -> {
                JSONObject taskJson = new JSONObject();
                if (Arrays.asList(ProcessTaskStatus.RUNNING.getValue(), ProcessTaskStatus.HANG.getValue()).contains(processTaskVo.getStatus())) {
                    processTaskVo.setStepList(processTaskMapper.getProcessTaskCurrentStepByProcessTaskId(processTaskVo.getId()));
                }
                //重新渲染工单字段
                for (Map.Entry<String, IProcessTaskColumn> entry : columnComponentMap.entrySet()) {
                    //long tmp = System.currentTimeMillis();
                    IProcessTaskColumn column = entry.getValue();
                    taskJson.put(column.getName(), column.getValue(processTaskVo));
                    /*if (Objects.equals("currentstep", column.getName())) {
                        System.out.println(System.currentTimeMillis() - tmp + " ##end workcenter-column " + column.getName() + ":-------------------------------------------------------------------------------");
                    }*/
                }
                // route 供前端跳转路由信息
                JSONObject routeJson = new JSONObject();
                routeJson.put("taskid", processTaskVo.getId());
                taskJson.put("route", routeJson);
                taskJson.put("taskid", processTaskVo.getId());
                taskJson.put("isShow", processTaskVo.getIsShow());
                taskJson.put("index", processTaskVo.getIndex());
                finalDataList.add(taskJson);
            }, "WORKCENTER-COLUMN-SEARCHER");
            //System.out.println((System.currentTimeMillis() - columnTime) + " ##end workcenter-column:-------------------------------------------------------------------------------");
        }
        dataList = dataList.stream().sorted(Comparator.comparing(o -> JSON.parseObject(o.toString()).getInteger("index"))).collect(Collectors.toList());
        // 字段排序
        /*JSONArray sortList = workcenterVo.getSortList();
        if (CollectionUtils.isEmpty(sortList)) {
            sortList = sortColumnList;
        }*/


        JSONObject returnObj = TableResultUtil.getOffsetResult(theadList, dataList, workcenterVo);
        //returnObj.put("sortList", sortList);

        //补充总数
        workcenterVo.setExpectOffsetRowNum(1000);
        workcenterVo.setCurrentPage(1);
        sb = new SqlBuilder(workcenterVo, ProcessSqlTypeEnum.LIMIT_COUNT);
        Integer total = processTaskMapper.getProcessTaskCountBySql(sb.build());
        returnObj.put("rowNum", total > 999 ? "999+" : total.toString());

        //补充待办数
        Integer count = 0;
        //long ofMineStartTime = System.currentTimeMillis();
        if (offsetRowNum > 0) {
            /*
            由于需要一直显示我的待办数量，因此无论输入条件有没有设置我的待办，都需要把我的待办设为1来查询一次数量
             */
            workcenterVo.getConditionConfig().put("isProcessingOfMine", 1);
            workcenterVo.setExpectOffsetRowNum(100);
            sb = new SqlBuilder(workcenterVo, ProcessSqlTypeEnum.LIMIT_COUNT);
            // System.out.println(sb.build());
            count = processTaskMapper.getProcessTaskCountBySql(sb.build());
        }
        returnObj.put("processingOfMineCount", count > 99 ? "99+" : count.toString());
        //System.out.println((System.currentTimeMillis() - ofMineStartTime) + " ##end workcenter-ofMine:-------------------------------------------------------------------------------");
        //System.out.println((System.currentTimeMillis() - theadStartTime) + " ##end workcenter:-------------------------------------------------------------------------------");
        returnObj.put("isAutoRefresh", Objects.equals(ConfigManager.getConfig(ItsmTenantConfig.WORKCENTER_AUTO_REFRESH), "1"));
        return returnObj;
    }

    @Override
    public JSONObject doSearch(List<Long> processTaskIdList) throws ParseException {
        JSONObject operationJson = new JSONObject();
        Boolean isHasProcessTaskAuth = AuthActionChecker.check(PROCESSTASK_MODIFY.class.getSimpleName());
        BatchRunner<Long> runner = new BatchRunner<>();
        runner.execute(processTaskIdList, 3, (threadIndex, dataIndex, processtaskId) -> {
            ProcessTaskVo processTaskVo = processTaskMapper.getProcessTaskAndStepById(processtaskId);
            JSONObject taskJson = null;
            if (processTaskVo != null) {
                Map<String, IProcessTaskColumn> columnComponentMap = ProcessTaskColumnFactory.columnComponentMap;
                //获取工单&&步骤操作
                ProcessAuthManager.Builder builder = new ProcessAuthManager.Builder();
                builder.addProcessTaskId(processTaskVo.getId());
                for (ProcessTaskStepVo processStep : processTaskVo.getStepList()) {
                    builder.addProcessTaskStepId(processStep.getId());
                }
                Map<Long, Set<IOperationType>> operateTypeSetMap =
                        builder.addOperationType(ProcessTaskOperationType.PROCESSTASK_ABORT)
                                .addOperationType(ProcessTaskOperationType.PROCESSTASK_RECOVER)
                                .addOperationType(ProcessTaskOperationType.PROCESSTASK_URGE)
                                .addOperationType(ProcessTaskStepOperationType.STEP_WORK).build().getOperateMap();

                processTaskVo.getParamObj().put("isHasProcessTaskAuth", isHasProcessTaskAuth);
                taskJson = new JSONObject();
                //重新渲染工单字段
                for (Map.Entry<String, IProcessTaskColumn> entry : columnComponentMap.entrySet()) {
                    IProcessTaskColumn column = entry.getValue();
                    taskJson.put(column.getName(), column.getValue(processTaskVo));
                }
                // route 供前端跳转路由信息
                //JSONObject routeJson = new JSONObject();
                // operate 获取对应工单的操作
                operationJson.put(processTaskVo.getId().toString(), getTaskOperate(processTaskVo, operateTypeSetMap));
            }
        }, "WORKCENTER-OPERATION-SEARCHER");
        return operationJson;
    }

    @Override
    public List<ProcessTaskVo> doSearchKeyword(WorkcenterVo workcenterVo) {
        //找出符合条件分页后的工单ID List
        //SqlBuilder sb = new SqlBuilder(workcenterVo, FieldTypeEnum.FULL_TEXT);
        //logger.info("fullTextSql:-------------------------------------------------------------------------------");
//        logger.info(sb.build());
        //return processTaskMapper.getProcessTaskBySql(sb.build());
        List<String> keywordList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(workcenterVo.getKeywordList())) {
            keywordList = new ArrayList<>(workcenterVo.getKeywordList());
        }
        return processTaskMapper.getProcessTaskColumnByIndexKeyword(keywordList, workcenterVo.getPageSize(), workcenterVo.getKeywordColumn(), workcenterVo.getKeywordPro());
    }

    @Override
    public Integer doSearchLimitCount(WorkcenterVo workcenterVo) {
        SqlBuilder sb = new SqlBuilder(workcenterVo, ProcessSqlTypeEnum.LIMIT_COUNT);
        //logger.info("countSql:-------------------------------------------------------------------------------");
        //logger.info(sb.build());
        return processTaskMapper.getProcessTaskCountBySql(sb.build());
    }

    @Override
    public List<WorkcenterTheadVo> getWorkcenterTheadList(WorkcenterVo workcenterVo, Map<String, IProcessTaskColumn> columnComponentMap) {
        String theadConfigHash = workcenterVo.getTheadConfigHash();
        WorkcenterVo workcenterThead = workcenterMapper.getWorkcenterThead(new WorkcenterTheadVo(workcenterVo.getUuid(), UserContext.get().getUserUuid()));
        if (workcenterThead != null && StringUtils.isNotBlank(workcenterThead.getTheadConfigHash())) {
            theadConfigHash = workcenterThead.getTheadConfigHash(); //优先使用用户自己定义的thead
        }
        String theadConfigStr = workcenterMapper.getWorkcenterTheadConfigByHash(theadConfigHash);
        workcenterVo.setTheadConfigStr(theadConfigStr);
        // 矫正theadList 或存在表单属性或固定字段增删
        // 多删
        List<WorkcenterTheadVo> theadList = workcenterVo.getTheadList();
        ListIterator<WorkcenterTheadVo> it = theadList.listIterator();
        while (it.hasNext()) {
            WorkcenterTheadVo thead = it.next();
            if (thead.getType().equals(ProcessFieldType.COMMON.getValue())) {
                if (!columnComponentMap.containsKey(thead.getName())) {
                    it.remove();
                } else {
                    thead.setDisabled(Boolean.TRUE.equals(columnComponentMap.get(thead.getName()).getDisabled()) ? 1 : 0);
                    thead.setDisplayName($.t(columnComponentMap.get(thead.getName()).getDisplayName()));
                    thead.setClassName(columnComponentMap.get(thead.getName()).getClassName());
                    thead.setIsExport(Boolean.TRUE.equals(columnComponentMap.get(thead.getName()).getIsExport()) ? 1 : 0);
                }
            } else {
                List<String> channelUuidList = workcenterVo.getChannelUuidList();
                if (CollectionUtils.isNotEmpty(channelUuidList)) {
                    List<String> formUuidList = channelMapper.getFormUuidListByChannelUuidList(channelUuidList);
                    if (CollectionUtils.isNotEmpty(formUuidList)) {
                        List<FormAttributeVo> formAttrList =
                                formMapper.getFormAttributeListByFormUuidList(formUuidList);
                        List<FormAttributeVo> theadFormList = formAttrList.stream()
                                .filter(attr -> attr.getUuid().equals(thead.getName())).collect(Collectors.toList());
                        if (CollectionUtils.isEmpty(theadFormList)) {
                            it.remove();
                        } else {
                            thead.setDisplayName(theadFormList.get(0).getLabel());
                        }
                    }
                }
            }
        }
        // 少补
        for (Map.Entry<String, IProcessTaskColumn> entry : columnComponentMap.entrySet()) {
            IProcessTaskColumn column = entry.getValue();
            if (Objects.equals(column.getType(), ProcessFieldType.COMMON.getValue()) && CollectionUtils.isEmpty(theadList.stream()
                    .filter(data -> column.getName().endsWith(data.getName())).collect(Collectors.toList()))) {
                theadList.add(new WorkcenterTheadVo(column));
            }
            // 如果需要排序
            //if (sortColumnList != null && column.get //if (sortColumnList != null && column.getIsSort()) {
            //            //     sortColumnList.add(column.getName());
            //            //}IsSort()) {
            //     sortColumnList.add(column.getName());
            //}
        }
        return theadList;
    }


    /**
     * @Description:
     * @Author: 89770
     * @Date: 2021/1/29 11:44
     * @Params: [processTaskVo, operateTypeSetMap]
     * @Returns: com.alibaba.fastjson.JSONObject
     **/
    private JSONObject getTaskOperate(ProcessTaskVo processTaskVo, Map<Long, Set<IOperationType>> operateTypeSetMap) {
        JSONObject action = new JSONObject();
        String processTaskStatus = processTaskVo.getStatus();
        boolean isHasAbort = false;
        boolean isHasRecover = false;
        boolean isHasUrge = false;
        JSONArray handleArray = new JSONArray();
        if ((ProcessTaskStatus.RUNNING.getValue().equals(processTaskStatus)
                || ProcessTaskStatus.DRAFT.getValue().equals(processTaskStatus)
                || ProcessTaskStatus.ABORTED.getValue().equals(processTaskStatus))) {
            Set<IOperationType> operationTypeSet = operateTypeSetMap.get(processTaskVo.getId());

            if (CollectionUtils.isNotEmpty(operationTypeSet)) {
                if (operationTypeSet.contains(ProcessTaskOperationType.PROCESSTASK_ABORT)) {
                    isHasAbort = true;
                }
                if (operationTypeSet.contains(ProcessTaskOperationType.PROCESSTASK_RECOVER)) {
                    isHasRecover = true;
                }
                if (operationTypeSet.contains(ProcessTaskOperationType.PROCESSTASK_URGE)) {
                    isHasUrge = true;
                }
            }
            for (ProcessTaskStepVo step : processTaskVo.getStepList()) {
                Set<IOperationType> set = operateTypeSetMap.get(step.getId());
                if (set != null && set.contains(ProcessTaskStepOperationType.STEP_WORK)) {
                    JSONObject configJson = new JSONObject();
                    configJson.put("taskid", processTaskVo.getId());
                    configJson.put("stepid", step.getId());
                    configJson.put("stepName", step.getName());
                    JSONObject actionJson = new JSONObject();
                    actionJson.put("name", "handle");
                    actionJson.put("text", step.getName());
                    actionJson.put("config", configJson);
                    handleArray.add(actionJson);
                }
            }
        }
        // 返回实际操作按钮
        /*
          实质性操作按钮：如“处理”、“取消”、“催办”，根据用户权限展示 ;次要的操作按钮：如“隐藏”、“删除”，只有管理员可见 移动端按钮展示规则：
          1、工单显示时，优先展示实质性的按钮，次要的操作按钮收起到“更多”中；如果没有任何实质性的操作按钮，则将次要按钮放出来（管理员可见）；
          2、工单隐藏时，仅“显示”、“删除”按钮放出来，其他实质性按钮需要等工单显示后才会展示；
         */

        WorkcenterOperateBuilder workcenterFirstOperateBuilder = new WorkcenterOperateBuilder();
        JSONArray workcenterFirstOperateArray = workcenterFirstOperateBuilder.setHandleOperate(handleArray)
                .setAbortRecoverOperate(isHasAbort, isHasRecover, processTaskVo).setUrgeOperate(isHasUrge, processTaskVo)
                .build();
        boolean isNeedFirstOperate = false;
        for (Object firstOperate : workcenterFirstOperateArray) {
            JSONObject firstOperateJson = JSON.parseObject(firstOperate.toString());
            if (firstOperateJson.getInteger("isEnable") == 1) {
                isNeedFirstOperate = true;
            }
        }
        WorkcenterOperateBuilder workcenterSecondOperateBuilder = new WorkcenterOperateBuilder();
        JSONArray workcenterSecondOperateJsonArray =
                workcenterSecondOperateBuilder.setShowHideOperate(processTaskVo).setDeleteOperate(processTaskVo).build();
        for (Object workcenterSecondOperateObj : workcenterSecondOperateJsonArray) {
            JSONObject workcenterSecondOperateJson = JSON.parseObject(workcenterSecondOperateObj.toString());
            if (ProcessTaskOperationType.PROCESSTASK_SHOW.getValue().equals(workcenterSecondOperateJson.getString("name"))) {
                isNeedFirstOperate = false;
            }
        }
        if (isNeedFirstOperate) {
            action.put("firstActionList", workcenterFirstOperateArray);
            action.put("secondActionList", workcenterSecondOperateJsonArray);
        } else {
            action.put("firstActionList", workcenterSecondOperateJsonArray);
            action.put("secondActionList", new JSONArray());
        }
        return action;
    }

    /**
     * @Description: 获取工单idList by  关键字搜索条件 keywordConditionList
     * @Author: 89770
     * @Date: 2021/2/9 17:08
     * @Params: [workcenterVo]
     * @Returns: java.util.List<java.lang.Long>
     **/
    @Deprecated
    private List<Long> getProcessTaskIdListByKeywordConditionList(WorkcenterVo workcenterVo) {
        if (CollectionUtils.isNotEmpty(workcenterVo.getKeywordConditionList())) {
            SqlBuilder sb = new SqlBuilder(workcenterVo, ProcessSqlTypeEnum.FULL_TEXT);
//        logger.info("fullTextGetIdListSql:-------------------------------------------------------------------------------");
//        logger.info(sb.build());
            List<ProcessTaskVo> processTaskVoList = processTaskMapper.getProcessTaskBySql(sb.build());
            return processTaskVoList.stream().map(ProcessTaskVo::getId).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public JSONArray getKeywordOptionsPCNew(WorkcenterVo workcenterVo) {
        JSONArray returnArray = new JSONArray();
//        workcenterVo.setSqlFieldType(FieldTypeEnum.FULL_TEXT.getValue());
        // 搜索标题
//        workcenterVo.setKeywordHandler(ProcessTaskSqlTable.FieldEnum.TITLE.getHandlerName());
//        workcenterVo.setKeywordText(ProcessTaskSqlTable.FieldEnum.TITLE.getText());
//        workcenterVo.setKeywordPro(ProcessTaskSqlTable.FieldEnum.TITLE.getProName());
//        workcenterVo.setKeywordColumn(ProcessTaskSqlTable.FieldEnum.TITLE.getValue());
//        returnArray.addAll(getKeywordOptionPCNew(workcenterVo));

        // 搜索SerialNumber
        JSONObject titleObj = new JSONObject();
        List<ProcessTaskVo> processTaskVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(workcenterVo.getKeywordList())) {
            processTaskVoList = processTaskMapper.getProcessTaskBySerialNumberList(workcenterVo.getKeywordList());
        }
        titleObj.put("dataList", processTaskVoList.stream().map(ProcessTaskVo::getSerialNumber).collect(Collectors.toList()));
        titleObj.put("value", ProcessTaskSqlTable.FieldEnum.SERIAL_NUMBER.getValue());
        titleObj.put("text", ProcessTaskSqlTable.FieldEnum.SERIAL_NUMBER.getText());
        returnArray.add(titleObj);

        // 搜索ID
        titleObj = new JSONObject();
        if (CollectionUtils.isNotEmpty(workcenterVo.getKeywordList())) {
            processTaskVoList = processTaskMapper.getProcessTaskByIdStrList(workcenterVo.getKeywordList());
        }
        titleObj.put("dataList", processTaskVoList.stream().map(ProcessTaskVo::getId).collect(Collectors.toList()));
        titleObj.put("value", ProcessTaskSqlTable.FieldEnum.ID.getValue());
        titleObj.put("text", ProcessTaskSqlTable.FieldEnum.ID.getText());
        returnArray.add(titleObj);
        return returnArray;
    }

    /**
     * @Description: 根据关键字获取所有过滤选项 pc端
     * @Author: 89770
     * @Date: 2021/2/5 9:59
     * @Params: [condition, keyword, pageSize, columnName]
     * @Returns: com.alibaba.fastjson.JSONArray
     **/
    private JSONArray getKeywordOptionPCNew(WorkcenterVo workcenterVo) {
        JSONArray returnArray = new JSONArray();
        List<ProcessTaskVo> processTaskVoList = doSearchKeyword(workcenterVo);
        if (CollectionUtils.isNotEmpty(processTaskVoList)) {
            JSONObject titleObj = new JSONObject();
            JSONArray dataList = new JSONArray();
            for (ProcessTaskVo processTaskVo : processTaskVoList) {
                dataList.add(JSON.parseObject(JSON.toJSONString(processTaskVo)).getString(workcenterVo.getKeywordPro()));
            }
            titleObj.put("dataList", dataList);
            titleObj.put("value", workcenterVo.getKeywordColumn());
            titleObj.put("text", workcenterVo.getKeywordText());
            returnArray.add(titleObj);
        }
        return returnArray;
    }

    @Override
    public void getStepTaskWorkerList(JSONArray workerArray, ProcessTaskStepVo stepVo) {
        if (ProcessTaskStepStatus.DRAFT.getValue().equals(stepVo.getStatus()) ||
                ProcessTaskStepStatus.RUNNING.getValue().equals(stepVo.getStatus()) ||
                (ProcessTaskStepStatus.HANG.getValue().equals(stepVo.getStatus()) && stepVo.getUserList().stream().anyMatch(o -> Objects.equals(o.getStatus(), ProcessTaskStepUserStatus.DOING.getValue()))) ||
                ProcessTaskStepStatus.PENDING.getValue().equals(stepVo.getStatus()) && stepVo.getIsActive() == 1
        ) {
            List<ProcessTaskStepWorkerVo> majorWorkerList = stepVo.getWorkerList().stream().filter(o -> Objects.equals(o.getUserType(), ProcessUserType.MAJOR.getValue())).collect(Collectors.toList());
            for (ProcessTaskStepWorkerVo majorWorker : majorWorkerList) {
                JSONObject workerJson = new JSONObject();
                getWorkerInfo(majorWorker, workerJson, workerArray);
            }
            List<String> workerUuidTypeList = new ArrayList<>();
            for (ProcessTaskStepWorkerVo workerVo : stepVo.getWorkerList()) {
                if (Objects.equals(workerVo.getUserType(), ProcessUserType.MINOR.getValue())) {
                    //子任务minor
                    //long stepStartTime = System.currentTimeMillis();
                    stepTaskWorker(workerVo, stepVo, workerArray, workerUuidTypeList);
                    //System.out.println((System.currentTimeMillis()-stepStartTime)+" ##end stepTaskWorker:-------------------------------------------------------------------------------");
                    //其他minor
                    //long otherStartTime = System.currentTimeMillis();
                    otherWorker(workerVo, stepVo, workerArray, workerUuidTypeList);
                    //System.out.println((System.currentTimeMillis()-otherStartTime)+" ##end otherWorker:-------------------------------------------------------------------------------");
                }
            }
        }
    }


    /**
     * 子任务处理人
     *
     * @param workerVo           处理人
     * @param stepVo             工单步骤
     * @param workerArray        处理人数组
     * @param workerUuidTypeList 用于去重
     */
    @Override
    public void stepTaskWorker(ProcessTaskStepWorkerVo workerVo, ProcessTaskStepVo stepVo, JSONArray workerArray, List<String> workerUuidTypeList) {
        if (Objects.equals(workerVo.getType(), GroupSearch.USER.getValue())) {
            List<ProcessTaskStepTaskVo> stepTaskVoList = processTaskStepTaskMapper.getStepTaskWithUserByProcessTaskStepId(stepVo.getId());
            for (ProcessTaskStepTaskVo stepTaskVo : stepTaskVoList) {
                for (ProcessTaskStepTaskUserVo userVo : stepTaskVo.getStepTaskUserVoList()) {
                    if (Objects.equals(userVo.getUserUuid(), workerVo.getUuid()) && !Objects.equals(ProcessTaskStepTaskUserStatus.SUCCEED.getValue(), userVo.getStatus())) {
                        String workerUuidType = workerVo.getUuid() + stepTaskVo.getTaskConfigName();
                        if (!workerUuidTypeList.contains(workerUuidType)) {
                            JSONObject workerJson = new JSONObject();
                            workerJson.put("workTypename", stepTaskVo.getTaskConfigName());
                            getWorkerInfo(workerVo, workerJson, workerArray);
                            workerUuidTypeList.add(workerUuidType);
                        }
                    }
                }
            }
        }
    }

    /**
     * 其它模块协助处理人
     *
     * @param workerVo           处理人
     * @param stepVo             工单步骤
     * @param workerArray        处理人数组
     * @param workerUuidTypeList 用于去重
     */
    @Override
    public void otherWorker(ProcessTaskStepWorkerVo workerVo, ProcessTaskStepVo stepVo, JSONArray workerArray, List<String> workerUuidTypeList) {
        IProcessStepHandler stepHandler = ProcessStepHandlerFactory.getHandler(stepVo.getHandler());
        List<ProcessTaskStepWorkerVo> stepMinorWorkerList = stepHandler.getMinorWorkerList(stepVo);
        if (CollectionUtils.isNotEmpty(stepMinorWorkerList)) {
            if (stepMinorWorkerList.stream().anyMatch(w -> Objects.equals(workerVo.getUuid(), w.getUuid()))) {
                String workerUuidType = workerVo.getUuid() + stepHandler.getMinorName();
                if (!workerUuidTypeList.contains(workerUuidType)) {
                    JSONObject workerJson = new JSONObject();
                    workerJson.put("workTypename", stepHandler.getMinorName());
                    getWorkerInfo(workerVo, workerJson, workerArray);
                    workerUuidTypeList.add(workerUuidType);
                }
            }
        }
    }

    @Override
    public void getWorkerInfo(ProcessTaskStepWorkerVo workerVo, JSONObject workerJson, JSONArray workerArray) {
        if (GroupSearch.USER.getValue().equals(workerVo.getType())) {
            UserVo userVo = userMapper.getUserBaseInfoByUuid(workerVo.getUuid());
            if (userVo != null) {
                workerJson.put("workerVo", JSON.parseObject(JSON.toJSONString(userVo)));
                workerArray.add(workerJson);
            }
        } else if (GroupSearch.TEAM.getValue().equals(workerVo.getType())) {
            TeamVo teamVo = teamMapper.getTeamByUuid(workerVo.getUuid());
            if (teamVo != null) {
                JSONObject teamTmp = new JSONObject();
                teamTmp.put("initType", GroupSearch.TEAM.getValue());
                teamTmp.put("uuid", teamVo.getUuid());
                teamTmp.put("name", teamVo.getName());
                workerJson.put("workerVo", teamTmp);
                workerArray.add(workerJson);
            }
        } else {
            RoleVo roleVo = roleMapper.getRoleByUuid(workerVo.getUuid());
            if (roleVo != null) {
                JSONObject roleTmp = new JSONObject();
                roleTmp.put("initType", GroupSearch.ROLE.getValue());
                roleTmp.put("uuid", roleVo.getUuid());
                roleTmp.put("name", roleVo.getName());
                workerJson.put("workerVo", roleTmp);
                workerArray.add(workerJson);
            }
        }
    }
}
