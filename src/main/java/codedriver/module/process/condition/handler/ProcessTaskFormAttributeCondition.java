package codedriver.module.process.condition.handler;

import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.framework.process.condition.core.IProcessTaskCondition;
import codedriver.framework.process.condition.core.ProcessTaskConditionBase;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.framework.process.dto.AttributeDataVo;
import codedriver.framework.process.dto.FormAttributeVo;
import codedriver.framework.process.dto.FormVersionVo;
import codedriver.framework.process.formattribute.core.FormAttributeHandlerFactory;
import codedriver.framework.process.formattribute.core.IFormAttributeHandler;
import codedriver.framework.util.Md5Util;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ProcessTaskFormAttributeCondition extends ProcessTaskConditionBase implements IProcessTaskCondition {

    @Override
    public String getName() {
        return ProcessFieldType.FORM.getValue();
    }

    @Override
    public String getDisplayName() {
        return ProcessFieldType.FORM.getName();
    }

    @Override
    public String getHandler(String processWorkcenterConditionType) {
        return null;
    }

    @Override
    public String getType() {
        return ProcessFieldType.FORM.getValue();
    }

    @Override
    public JSONObject getConfig() {
        return null;
    }

    @Override
    public Integer getSort() {
        return null;
    }

    @Override
    public ParamType getParamType() {
        return null;
    }

    @Override
    protected String getMyEsWhere(Integer index, List<ConditionVo> conditionList) {
        ConditionVo condition = conditionList.get(index);
        if (condition != null && StringUtils.isNotBlank(condition.getName())) {
            IFormAttributeHandler formHandler = FormAttributeHandlerFactory.getHandler(condition.getHandler());
            if (condition.getHandler().equals("formdate")) {
                return getDateEsWhere(condition, conditionList);
            } else {
                String where = "(";
                String formKey = condition.getName();
                String formValueKey = "form.value_" + formHandler.getDataType().toLowerCase();
                Object value = StringUtils.EMPTY;
                if (condition.getValueList() instanceof String) {
                    value = condition.getValueList();
                } else if (condition.getValueList() instanceof List) {
                    List<String> values = JSON.parseArray(JSON.toJSONString(condition.getValueList()), String.class);
                    value = String.join("','", values);
                }
                if (StringUtils.isNotBlank(value.toString())) {
                    value = String.format("'%s'", value);
                }
                where += String.format(
                        " [ form.key = '%s' and " + Expression.getExpressionEs(condition.getExpression()) + " ] ", formKey,
                        formValueKey, value);
                return where + ")";
            }
        }
        return null;
    }

    @Override
    public Object valueConversionText(Object value, JSONObject config) {
        if (MapUtils.isNotEmpty(config)) {
            String attributeUuid = config.getString("attributeUuid");
            String formConfig = config.getString("formConfig");
            FormVersionVo formVersionVo = new FormVersionVo();
            formVersionVo.setFormConfig(formConfig);
            List<FormAttributeVo> formAttributeList = formVersionVo.getFormAttributeList();
            if (CollectionUtils.isNotEmpty(formAttributeList)) {
                for (FormAttributeVo formAttribute : formAttributeList) {
                    if (Objects.equal(attributeUuid, formAttribute.getUuid())) {
                        config.put("name", formAttribute.getLabel());
                        if (value != null) {
                            IFormAttributeHandler formAttributeHandler =
                                    FormAttributeHandlerFactory.getHandler(formAttribute.getHandler());
                            if (formAttributeHandler != null) {
                                AttributeDataVo attributeDataVo = new AttributeDataVo();
                                attributeDataVo.setAttributeUuid(attributeUuid);
                                if (value instanceof String) {
                                    attributeDataVo.setData((String) value);
                                } else if (value instanceof JSONArray) {
                                    attributeDataVo.setData(JSON.toJSONString(value));
                                }
                                Object text = formAttributeHandler.valueConversionText(attributeDataVo,
                                        JSON.parseObject(formAttribute.getConfig()));
                                if (text instanceof String) {
                                    return text;
                                } else if (text instanceof List) {
                                    List<String> textList = JSON.parseArray(JSON.toJSONString(text), String.class);
                                    if ("formdate".equals(formAttribute.getHandler())
                                            || "formtime".equals(formAttribute.getHandler())) {
                                        return String.join("-", textList);
                                    } else if ("formcascadelist".equals(formAttribute.getHandler())) {
                                        return String.join("/", textList);
                                    } else {
                                        return String.join("、", textList);
                                    }
                                }
                                return text;
                            }
                        }
                    }
                }
            }
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        ConditionVo conditionVo = conditionList.get(index);
        IFormAttributeHandler formAttributeHandler = FormAttributeHandlerFactory.getHandler(conditionVo.getHandler());
        JSONArray valueArray = JSONArray.parseArray(conditionVo.getValueList().toString());
        List<String> valueList = new ArrayList<>();
        for(Object valueObj : valueArray){
            valueList.addAll(Arrays.stream(valueObj.toString().split("&=&")).map(o -> formAttributeHandler.isNeedSliceWord() ? o : Md5Util.encryptBASE64(o).toLowerCase(Locale.ROOT)).collect(Collectors.toList()));
        }
        sqlSb.append(String.format(" EXISTS (SELECT 1 FROM `fulltextindex_word` fw JOIN fulltextindex_field_process ff ON fw.id = ff.`word_id` WHERE ff.`target_id` = pt.id  AND ff.`target_type` = 'processtask_form' AND ff.`target_field` = '%s' AND fw.word IN ('%s')) ",
                conditionVo.getName(), String.join("','",valueList)));
    }
}
