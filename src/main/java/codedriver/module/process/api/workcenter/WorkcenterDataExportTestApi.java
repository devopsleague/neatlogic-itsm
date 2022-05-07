/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.api.workcenter;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.form.attribute.core.FormAttributeHandlerFactory;
import codedriver.framework.form.attribute.core.IFormAttributeHandler;
import codedriver.framework.form.dao.mapper.FormMapper;
import codedriver.framework.form.dto.FormAttributeVo;
import codedriver.framework.form.dto.FormVersionVo;
import codedriver.framework.process.auth.PROCESS_BASE;
import codedriver.framework.process.column.core.IProcessTaskColumn;
import codedriver.framework.process.column.core.ProcessTaskColumnFactory;
import codedriver.framework.process.dao.mapper.ChannelMapper;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dao.mapper.workcenter.WorkcenterMapper;
import codedriver.framework.process.dto.ProcessFormVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.workcenter.dto.WorkcenterTheadVo;
import codedriver.framework.process.workcenter.dto.WorkcenterVo;
import codedriver.framework.process.workcenter.table.constvalue.ProcessSqlTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.module.framework.form.attribute.handler.DivideHandler;
import codedriver.module.process.dao.mapper.ProcessMapper;
import codedriver.module.process.service.NewWorkcenterService;
import codedriver.module.process.sql.decorator.SqlBuilder;
import codedriver.module.process.workcenter.column.handler.ProcessTaskCurrentStepColumn;
import codedriver.module.process.workcenter.column.handler.ProcessTaskCurrentStepNameColumn;
import codedriver.module.process.workcenter.column.handler.ProcessTaskCurrentStepWorkerColumn;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 导出工单中心工单数据接口
 * 同时支持按分类导出与实时查询结果导出
 */
@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class WorkcenterDataExportTestApi extends PrivateBinaryStreamApiComponentBase {
    @Resource
    WorkcenterMapper workcenterMapper;

    @Resource
    NewWorkcenterService newWorkcenterService;

    @Resource
    ChannelMapper channelMapper;

    @Resource
    ProcessMapper processMapper;

    @Resource
    FormMapper formMapper;

    @Resource
    ProcessTaskMapper processTaskMapper;

    @Override
    public String getToken() {
        return "workcenter/export";
    }

    @Override
    public String getName() {
        return "导出工单中心数据";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "uuid", type = ApiParamType.STRING, desc = "分类uuid", isRequired = true),
            @Param(name = "conditionConfig", type = ApiParamType.JSONOBJECT, desc = "条件设置，为空则使用数据库中保存的条件")
    })
    @Output({})
    @Description(desc = "导出工单中心数据")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uuid = jsonObj.getString("uuid");
        WorkcenterVo workcenterVo = JSONObject.toJavaObject(jsonObj, WorkcenterVo.class);
        Map<String, IProcessTaskColumn> columnComponentMap = ProcessTaskColumnFactory.columnComponentMap;
        /* 获取表头 */
        List<WorkcenterTheadVo> theadList = newWorkcenterService.getWorkcenterTheadList(workcenterVo, columnComponentMap);
        if (CollectionUtils.isNotEmpty(theadList)) {
            /* 如果勾选了当前步骤，却没有勾选当前步骤名与当前步骤处理人，自动加上 */
            if (theadList.stream().anyMatch(o -> o.getName().equals(new ProcessTaskCurrentStepColumn().getName()) && o.getIsShow() == 1)) {
                IProcessTaskColumn stepNameColumn = new ProcessTaskCurrentStepNameColumn();
                IProcessTaskColumn stepWorkerColumn = new ProcessTaskCurrentStepWorkerColumn();
                if (theadList.stream().noneMatch(o -> o.getName().equals(stepNameColumn.getName()) && o.getIsShow() == 1)) {
                    theadList.add(new WorkcenterTheadVo(stepNameColumn));
                }
                if (theadList.stream().noneMatch(o -> o.getName().equals(stepWorkerColumn.getName()) && o.getIsShow() == 1)) {
                    theadList.add(new WorkcenterTheadVo(stepWorkerColumn));
                }
            }
            theadList = theadList.stream().filter(o -> o.getDisabled() == 0 && o.getIsExport() == 1 && o.getIsShow() == 1)
                    .sorted(Comparator.comparing(WorkcenterTheadVo::getSort)).collect(Collectors.toList());
            workcenterVo.setTheadVoList(theadList);
        }
        // todo 服务不同，表单不同，表头也不同，循环每一批工单，判断是否存在该服务的sheet，不存在则创建，存在则追加数据
        List<String> publicHeadList = theadList.stream().map(WorkcenterTheadVo::getDisplayName).collect(Collectors.toList());
        Workbook workbook = new SXSSFWorkbook();
        Map<String, Sheet> sheetMap = new HashMap<>();
        Map<String, List<String>> channelFormLabelListMap = new HashMap<>();

        SqlBuilder sb = new SqlBuilder(workcenterVo, ProcessSqlTypeEnum.TOTAL_COUNT);
        int total = processTaskMapper.getProcessTaskCountBySql(sb.build());
        if (total > 0) {
            workcenterVo.setRowNum(total);
            workcenterVo.setPageSize(100);
            Integer pageCount = workcenterVo.getPageCount();
            for (int i = 1; i <= pageCount; i++) {
                workcenterVo.setCurrentPage(i);
                sb = new SqlBuilder(workcenterVo, ProcessSqlTypeEnum.DISTINCT_ID);
                List<ProcessTaskVo> processTaskList = processTaskMapper.getProcessTaskBySql(sb.build());
                workcenterVo.setProcessTaskIdList(processTaskList.stream().map(ProcessTaskVo::getId).collect(Collectors.toList()));
                sb = new SqlBuilder(workcenterVo, ProcessSqlTypeEnum.FIELD);
                List<ProcessTaskVo> processTaskVoList = processTaskMapper.getProcessTaskBySql(sb.build());
                for (ProcessTaskVo taskVo : processTaskVoList) {
                    String channelUuid = taskVo.getChannelVo().getUuid();
                    Sheet sheet = sheetMap.get(channelUuid);
                    if (sheet == null) {
                        sheet = workbook.createSheet(taskVo.getChannelVo().getName());
                        List<String> headList = new ArrayList<>(publicHeadList);
                        List<String> formLabelList = null;
                        Map<String, Integer> formLabelCellRangeMap = null;
                        String processUuid = channelMapper.getProcessUuidByChannelUuid(channelUuid);
                        if (StringUtils.isNotBlank(processUuid)) {
                            ProcessFormVo processForm = processMapper.getProcessFormByProcessUuid(processUuid);
                            if (processForm != null) {
                                FormVersionVo formVersionVo = formMapper.getActionFormVersionByFormUuid(processForm.getFormUuid());
                                if (formVersionVo != null) {
                                    List<FormAttributeVo> formAttributeList = formVersionVo.getFormAttributeList();
                                    if (CollectionUtils.isNotEmpty(formAttributeList)) {
                                        // todo 如果存在表格类字段，则需要根据表头字段数量计算出sheet表头需要合并多少列
                                        // todo DynamicListHandler扩展属性
                                        /**
                                         * 表头数量获取途径：
                                         * 账号选择组件-AccountsHandler：theadList
                                         * 表格选择组件-DynamicListHandler：dataConfig(扩展属性从attributeList拿)
                                         * 表格输入组件-StaticListHandler：attributeList
                                         * 配置项修改组件-CiEntitySyncHandler：dataConfig(注意isShow)
                                         */
                                        formLabelList = new ArrayList<>();
                                        formLabelCellRangeMap = new HashMap<>();
                                        for (FormAttributeVo formAttributeVo : formAttributeList) {
                                            IFormAttributeHandler handler = FormAttributeHandlerFactory.getHandler(formAttributeVo.getHandler());
                                            if ((handler instanceof DivideHandler)) {
                                                continue;
                                            }
                                            formLabelCellRangeMap.put(formAttributeVo.getLabel(), handler.getExcelHeadLength(formAttributeVo.getConfigObj()));
                                            formLabelList.add(formAttributeVo.getLabel());
                                        }
                                        channelFormLabelListMap.put(channelUuid, formLabelList);
                                    }
                                }
                            }
                        }
                        List<String> formCellValueList = new ArrayList<>();
                        List<CellRangeAddress> cellRangeAddressList = new ArrayList<>();
                        // 记录表单字段值&计算表单字段单元格合并
                        if (CollectionUtils.isNotEmpty(formLabelList)) {
                            int start = headList.size();
                            int end = headList.size();
                            for (int k = 0; k < formLabelList.size(); k++) {
                                Integer cellRange = formLabelCellRangeMap.get(formLabelList.get(k));
                                if (cellRange != null && cellRange > 1) {
                                    for (int m = 0; m < cellRange; m++) {
                                        formCellValueList.add(formLabelList.get(k));
                                    }
                                    end = start + cellRange - 1;
                                    cellRangeAddressList.add(new CellRangeAddress(0, 0, start, end));
                                    start = end + 1;
                                } else {
                                    formCellValueList.add(formLabelList.get(k));
                                    start++;
                                    end++;
                                }
                            }
                        }
                        // 创建工单字段
                        Row headerRow = sheet.createRow(0);
                        for (int j = 0; j < headList.size(); j++) {
                            Cell cell = headerRow.createCell(j);
                            cell.setCellValue(headList.get(j));
                        }
                        // 创建表单字段
                        if (CollectionUtils.isNotEmpty(formCellValueList)) {
                            for (int j = 0; j < formCellValueList.size(); j++) {
                                Cell cell = headerRow.createCell(headList.size() + j);
                                cell.setCellValue(formCellValueList.get(j));
                            }
                        }
                        if (cellRangeAddressList.size() > 0) {
                            for (CellRangeAddress cellAddresses : cellRangeAddressList) {
                                sheet.addMergedRegion(cellAddresses);
                            }
                        }
                        sheetMap.put(channelUuid, sheet);
                    }
                    int lastRowNum = sheet.getLastRowNum();
                    sheet.createRow(lastRowNum + 1);

//                    if (Objects.equals(taskVo.getStatus(), ProcessTaskStatus.RUNNING.getValue())) {
//                        taskVo.setStepList(processTaskMapper.getProcessTaskCurrentStepByProcessTaskId(taskVo.getId()));
//                    }
//                    Map<String, Object> map = new LinkedHashMap<>();
//                    //重新渲染工单字段
//                    for (Map.Entry<String, IProcessTaskColumn> entry : columnComponentMap.entrySet()) {
//                        IProcessTaskColumn column = entry.getValue();
//                        if (column.getIsShow() && column.getIsExport() && !column.getDisabled()) {
//                            map.put(column.getDisplayName(), column.getSimpleValue(taskVo));
//                        }
//                    }

                }

            }
        }
        String fileNameEncode = FileUtil.getEncodedFileName(request.getHeader("User-Agent"), "工单数据" + ".xlsx");
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileNameEncode + "\"");
        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
        } catch (IOException e) {
            throw e;
        }

        return null;
    }

}
