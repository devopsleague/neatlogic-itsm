package neatlogic.module.process.audithandler.handler;

import neatlogic.framework.form.attribute.core.FormAttributeDataConversionHandlerFactory;
import neatlogic.framework.form.attribute.core.FormAttributeHandlerFactory;
import neatlogic.framework.form.attribute.core.IFormAttributeDataConversionHandler;
import neatlogic.framework.form.attribute.core.IFormAttributeHandler;
import neatlogic.framework.form.dto.FormAttributeVo;
import neatlogic.framework.form.dto.FormVersionVo;
import neatlogic.framework.process.audithandler.core.IProcessTaskStepAuditDetailHandler;
import neatlogic.framework.process.constvalue.ProcessTaskAuditDetailType;
import neatlogic.framework.process.dao.mapper.ProcessTaskMapper;
import neatlogic.framework.process.dao.mapper.SelectContentByHashMapper;
import neatlogic.framework.process.dto.ProcessTaskFormAttributeDataVo;
import neatlogic.framework.process.dto.ProcessTaskFormVo;
import neatlogic.framework.process.dto.ProcessTaskStepAuditDetailVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FormAuditHandler implements IProcessTaskStepAuditDetailHandler {

    private final static Logger logger = LoggerFactory.getLogger(FormAuditHandler.class);

    @Autowired
    private ProcessTaskMapper processTaskMapper;
    @Autowired
    private SelectContentByHashMapper selectContentByHashMapper;

    @Override
    public String getType() {
        return ProcessTaskAuditDetailType.FORM.getValue();
    }

    @Override
    public int handle(ProcessTaskStepAuditDetailVo processTaskStepAuditDetailVo) {
        List<ProcessTaskFormAttributeDataVo> oldProcessTaskFormAttributeDataList =
            JSON.parseArray(processTaskStepAuditDetailVo.getOldContent(), ProcessTaskFormAttributeDataVo.class);
        if (oldProcessTaskFormAttributeDataList == null) {
            oldProcessTaskFormAttributeDataList = new ArrayList<>();
        }

        List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList =
            JSON.parseArray(processTaskStepAuditDetailVo.getNewContent(), ProcessTaskFormAttributeDataVo.class);
        if (processTaskFormAttributeDataList == null) {
            processTaskFormAttributeDataList = new ArrayList<>();
        }
        Iterator<ProcessTaskFormAttributeDataVo> iterator = processTaskFormAttributeDataList.iterator();
        // 循环删除相同的
        while (iterator.hasNext()) {
            ProcessTaskFormAttributeDataVo processTaskFormAttributeDataVo = iterator.next();
            int index = oldProcessTaskFormAttributeDataList.indexOf(processTaskFormAttributeDataVo);

            if (index != -1) {
                iterator.remove();
                oldProcessTaskFormAttributeDataList.remove(index);
            } else {// 删除不能audit的表单组件
                IFormAttributeHandler formHandler =
                    FormAttributeHandlerFactory.getHandler(processTaskFormAttributeDataVo.getType());
                if (formHandler != null && !formHandler.isAudit()) {
                    iterator.remove();
                }
            }
        }
        processTaskStepAuditDetailVo.setOldContent(null);
        processTaskStepAuditDetailVo.setNewContent(null);
        if (CollectionUtils.isNotEmpty(processTaskFormAttributeDataList)) {
            Map<String, JSONObject> attributeConfigMap = new HashMap<>();
            Map<String, String> attributeLabelMap = new HashMap<>();
            Long processTaskId = processTaskFormAttributeDataList.get(0).getProcessTaskId();
            ProcessTaskFormVo processTaskForm = processTaskMapper.getProcessTaskFormByProcessTaskId(processTaskId);
            if (processTaskForm != null && StringUtils.isNotBlank(processTaskForm.getFormContentHash())) {
                String formContent =
                    selectContentByHashMapper.getProcessTaskFromContentByHash(processTaskForm.getFormContentHash());
                if (StringUtils.isNotBlank(formContent)) {
                    try {
                        FormVersionVo formVersionVo = new FormVersionVo();
                        formVersionVo.setFormUuid(processTaskForm.getFormUuid());
                        formVersionVo.setFormName(processTaskForm.getFormName());
                        formVersionVo.setFormConfig(JSONObject.parseObject(formContent));
                        List<FormAttributeVo> formAttributeList = formVersionVo.getFormAttributeList();
                        if (CollectionUtils.isNotEmpty(formAttributeList)) {
                            for (FormAttributeVo formAttributeVo : formAttributeList) {
                                attributeLabelMap.put(formAttributeVo.getUuid(), formAttributeVo.getLabel());
                                attributeConfigMap.put(formAttributeVo.getUuid(), formAttributeVo.getConfigObj());
                            }
                        }
//                        JSONObject formConfig = JSON.parseObject(formContent);
//                        JSONArray controllerList = formConfig.getJSONArray("controllerList");
//                        if (CollectionUtils.isNotEmpty(controllerList)) {
//                            for (int i = 0; i < controllerList.size(); i++) {
//                                JSONObject attributeObj = controllerList.getJSONObject(i);
//                                attributeLabelMap.put(attributeObj.getString("uuid"), attributeObj.getString("label"));
//                                attributeConfigMap.put(attributeObj.getString("uuid"),
//                                    attributeObj.getJSONObject("config"));
//                            }
//                        }
                    } catch (Exception ex) {
                        logger.error("hash为" + processTaskForm.getFormContentHash() + "的processtask_form内容不是合法的JSON格式",
                            ex);
                    }
                }
            }

            Map<String, String> oldContentMap = new HashMap<>();
            for (ProcessTaskFormAttributeDataVo attributeDataVo : oldProcessTaskFormAttributeDataList) {
                IFormAttributeDataConversionHandler handler = FormAttributeDataConversionHandlerFactory.getHandler(attributeDataVo.getType());
                if (handler != null) {
                    String result = null;
                    Object value = handler.valueConversionText(attributeDataVo,
                        attributeConfigMap.get(attributeDataVo.getAttributeUuid()));
                    if (value != null) {
                        if (value instanceof String) {
                            result = (String) value;
                        } else if (value instanceof List) {
                            List<String> valueList = JSON.parseArray(JSON.toJSONString(value), String.class);
                            result = String.join("、", valueList);
                        } else {
                            result = value.toString();
                        }
                    }
                    oldContentMap.put(attributeDataVo.getAttributeUuid(), result);
                } else {
                    oldContentMap.put(attributeDataVo.getAttributeUuid(), attributeDataVo.getData());
                }
            }
            JSONArray contentList = new JSONArray();
            for (ProcessTaskFormAttributeDataVo attributeDataVo : processTaskFormAttributeDataList) {
                JSONObject content = new JSONObject();
                content.put("label", attributeLabelMap.get(attributeDataVo.getAttributeUuid()));
                /*if (ProcessFormHandlerType.FORMCASCADELIST.getHandler().equalsIgnoreCase(attributeDataVo.getType())
                    || ProcessFormHandlerType.FORMDYNAMICLIST.getHandler().equalsIgnoreCase(attributeDataVo.getType())
                    || ProcessFormHandlerType.FORMSTATICLIST.getHandler().equalsIgnoreCase(attributeDataVo.getType())) {
                    if (!attributeDataVo.dataIsEmpty()) {
                        content.put("newContent", "已更新");
                    }
                } else {*/
                String oldContent = oldContentMap.get(attributeDataVo.getAttributeUuid());
                if (oldContent != null) {
                    content.put("oldContent", oldContent);
                }
                String newContent = null;
                IFormAttributeDataConversionHandler handler = FormAttributeDataConversionHandlerFactory.getHandler(attributeDataVo.getType());
                if (handler != null) {
                    String result = null;
                    Object value = handler.valueConversionText(attributeDataVo,
                        attributeConfigMap.get(attributeDataVo.getAttributeUuid()));
                    if (value != null) {
                        if (value instanceof String) {
                            result = (String) value;
                        } else if (value instanceof List) {
                            List<String> valueList = JSON.parseArray(JSON.toJSONString(value), String.class);
                            result = String.join("、", valueList);
                        } else {
                            result = value.toString();
                        }
                    }
                    content.put("newContent", result);
                    newContent = result;
                } else {
                    content.put("newContent", attributeDataVo.getData());
                    newContent = attributeDataVo.getData();
                }
                // }
               if(oldContent != null && newContent != null){
                   if(oldContent.equals(newContent)){
                       content.remove("oldContent");
                       content.put("changeType", "new");
                   }else{
                       content.put("changeType", "update");
                   }
                }else if(oldContent != null){
                    content.put("changeType", "clear");
                }else if(newContent != null){
                    content.put("changeType", "new");
                }
                contentList.add(content);
            }
            processTaskStepAuditDetailVo.setNewContent(JSON.toJSONString(contentList));
            return 1;
        } else {
            return 0;
        }
    }

}