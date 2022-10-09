/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.condition.handler;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.ParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.condition.ConditionGroupVo;
import codedriver.framework.dto.condition.ConditionVo;
import codedriver.framework.form.constvalue.FormConditionModel;
import codedriver.framework.process.condition.core.IProcessTaskCondition;
import codedriver.framework.process.condition.core.ProcessTaskConditionBase;
import codedriver.framework.process.constvalue.ConditionConfigType;
import codedriver.framework.process.constvalue.ProcessFieldType;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.constvalue.ProcessTaskStepUserStatus;
import codedriver.framework.process.dto.SqlDecoratorVo;
import codedriver.framework.process.workcenter.dto.JoinOnVo;
import codedriver.framework.process.workcenter.dto.JoinTableColumnVo;
import codedriver.framework.process.workcenter.table.ProcessTaskFocusSqlTable;
import codedriver.framework.process.workcenter.table.ProcessTaskSqlTable;
import codedriver.framework.process.workcenter.table.ProcessTaskStepSqlTable;
import codedriver.framework.process.workcenter.table.ProcessTaskStepUserSqlTable;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class ProcessTaskAboutMeCondition extends ProcessTaskConditionBase implements IProcessTaskCondition {

    @Autowired
    UserMapper userMapper;

    private String formHandlerType = FormHandlerType.SELECT.toString();

    private final Map<String, Function<String, String>> map = new HashMap<>();

    private final Map<String, MyProcessTask<StringBuilder>> mapSql = new HashMap<>();

    private final Map<String, MyProcessTask<List<JoinTableColumnVo>>> joinTableSqlMap = new HashMap<>();

    @FunctionalInterface
    public interface MyProcessTask<T> {
        void build(T t);
    }

    {

        mapSql.put("doneOfMine", (sqlSb) -> {
            sqlSb.append(" ( ");
            sqlSb.append(Expression.getExpressionSql(Expression.EQUAL.getExpression(), new ProcessTaskStepUserSqlTable().getShortName(), ProcessTaskStepUserSqlTable.FieldEnum.STATUS.getValue(), ProcessTaskStepUserStatus.DONE.getValue()));
            sqlSb.append(" ) and ( ");
            sqlSb.append(Expression.getExpressionSql(Expression.EQUAL.getExpression(), new ProcessTaskStepUserSqlTable().getShortName(), ProcessTaskStepUserSqlTable.FieldEnum.USER_UUID.getValue(), UserContext.get().getUserUuid(true)));
            sqlSb.append(" ) ");
        });
        mapSql.put("focusOfMine", (sqlSb) -> {
            sqlSb.append(" ( ");
            sqlSb.append(Expression.getExpressionSql(Expression.EQUAL.getExpression(), new ProcessTaskFocusSqlTable().getShortName(), ProcessTaskFocusSqlTable.FieldEnum.USER_UUID.getValue(), UserContext.get().getUserUuid(true)));
            sqlSb.append(" ) ");
        });
        mapSql.put("needScoreOfMine", (sqlSb) -> {
            sqlSb.append(" ( ");
            sqlSb.append(Expression.getExpressionSql(Expression.EQUAL.getExpression(), new ProcessTaskSqlTable().getShortName(), ProcessTaskSqlTable.FieldEnum.OWNER.getValue(), UserContext.get().getUserUuid(true)));
            sqlSb.append(" ) and ( ");
            sqlSb.append(Expression.getExpressionSql(Expression.EQUAL.getExpression(), new ProcessTaskSqlTable().getShortName(), ProcessTaskSqlTable.FieldEnum.NEED_SCORE.getValue(), "1"));
            sqlSb.append(" ) and ( ");
            sqlSb.append(Expression.getExpressionSql(Expression.EQUAL.getExpression(), new ProcessTaskSqlTable().getShortName(), ProcessTaskSqlTable.FieldEnum.STATUS.getValue(), ProcessTaskStatus.SUCCEED.getValue()));
            sqlSb.append(" ) ");
        });

        mapSql.put("scoredOfMine", (sqlSb) -> {
            sqlSb.append(" ( ");
            sqlSb.append(Expression.getExpressionSql(Expression.EQUAL.getExpression(), new ProcessTaskSqlTable().getShortName(), ProcessTaskSqlTable.FieldEnum.OWNER.getValue(), UserContext.get().getUserUuid(true)));
            sqlSb.append(" ) and ( ");
            sqlSb.append(Expression.getExpressionSql(Expression.EQUAL.getExpression(), new ProcessTaskSqlTable().getShortName(), ProcessTaskSqlTable.FieldEnum.STATUS.getValue(), ProcessTaskStatus.SCORED.getValue()));
            sqlSb.append(" ) ");
        });

        joinTableSqlMap.put("doneOfMine", (list) -> {
            list.add(new JoinTableColumnVo(new ProcessTaskSqlTable(), new ProcessTaskStepSqlTable(), new ArrayList<JoinOnVo>() {{
                add(new JoinOnVo(ProcessTaskSqlTable.FieldEnum.ID.getValue(), ProcessTaskStepSqlTable.FieldEnum.PROCESSTASK_ID.getValue()));
            }}));
            list.add(new JoinTableColumnVo(new ProcessTaskStepSqlTable(), new ProcessTaskStepUserSqlTable(), new ArrayList<JoinOnVo>() {{
                add(new JoinOnVo(ProcessTaskStepSqlTable.FieldEnum.PROCESSTASK_ID.getValue(), ProcessTaskStepUserSqlTable.FieldEnum.PROCESSTASK_ID.getValue()));
                add(new JoinOnVo(ProcessTaskStepSqlTable.FieldEnum.ID.getValue(), ProcessTaskStepUserSqlTable.FieldEnum.PROCESSTASK_STEP_ID.getValue()));
            }}));
        });
        joinTableSqlMap.put("focusOfMine", (list) -> {
            list.add(new JoinTableColumnVo(new ProcessTaskSqlTable(), new ProcessTaskFocusSqlTable(), new ArrayList<JoinOnVo>() {{
                add(new JoinOnVo(ProcessTaskSqlTable.FieldEnum.ID.getValue(), ProcessTaskFocusSqlTable.FieldEnum.PROCESSTASK_ID.getValue()));
            }}));
        });
    }

    @Override
    public String getName() {
        return "aboutme";
    }

    @Override
    public String getDisplayName() {
        return "与我相关";
    }

    @Override
    public String getHandler(FormConditionModel formConditionModel) {
        if (FormConditionModel.SIMPLE == formConditionModel) {
            formHandlerType = FormHandlerType.CHECKBOX.toString();
        } else {
            formHandlerType = FormHandlerType.SELECT.toString();
        }
        return formHandlerType;
    }

    @Override
    public String getType() {
        return ProcessFieldType.COMMON.getValue();
    }

    @Override
    public JSONObject getConfig(ConditionConfigType type) {
        JSONArray dataList = new JSONArray();
        dataList.add(new ValueTextVo("doneOfMine", "已办"));
        dataList.add(new ValueTextVo("focusOfMine", "已关注"));
        dataList.add(new ValueTextVo("needScoreOfMine", "待评分"));
        dataList.add(new ValueTextVo("scoredOfMine", "已评分"));

        JSONObject config = new JSONObject();
        config.put("type", formHandlerType);
        config.put("search", false);
        config.put("multiple", false);
        config.put("value", "");
        config.put("defaultValue", "");
        config.put("dataList", dataList);
        /** 以下代码是为了兼容旧数据结构，前端有些地方还在用 **/
        config.put("isMultiple", false);
        return config;
    }

    @Override
    public Integer getSort() {
        return 8;
    }

    @Override
    public ParamType getParamType() {
        return ParamType.ARRAY;
    }


    @Override
    public Object valueConversionText(Object value, JSONObject config) {
        if (value instanceof String) {
            String v = value.toString();
            if (v.equalsIgnoreCase("doneOfMine")) {
                return "已办";
            } else if (v.equalsIgnoreCase("focusOfMine")) {
                return "已关注";
            } else if (v.equalsIgnoreCase("needScoreOfMine")) {
                return "待评分";
            } else if (v.equalsIgnoreCase("scoredOfMine")) {
                return "已评分";
            }
        } else if (value instanceof JSONArray) {
            List<String> textList = new ArrayList<>();
            JSONArray vList = (JSONArray) value;
            for (int i = 0; i < vList.size(); i++) {
                String v = vList.getString(i);
                if (v.equalsIgnoreCase("doneOfMine")) {
                    textList.add("已办");
                } else if (v.equalsIgnoreCase("focusOfMine")) {
                    textList.add("已关注");
                } else if (v.equalsIgnoreCase("needScoreOfMine")) {
                    textList.add("待评分");
                } else if (v.equalsIgnoreCase("scoredOfMine")) {
                    textList.add("已评分");
                }
            }
            return String.join("、", textList);
        }
        return value;
    }

    @Override
    public void getSqlConditionWhere(List<ConditionVo> conditionList, Integer index, StringBuilder sqlSb) {
        sqlSb.append(" ( ");
        ConditionVo condition = conditionList.get(index);
        List<String> valueList = JSON.parseArray(JSON.toJSONString(condition.getValueList()), String.class);
        int i = 0;
        for (String value : valueList) {
            MyProcessTask<StringBuilder> result = mapSql.get(value);
            if (result != null) {
                // 拼接条件
                if (i != 0) {
                    sqlSb.append(" or ");
                }
                result.build(sqlSb);

            }
            i++;
        }
        sqlSb.append(" ) ");
    }

    @Override
    public List<JoinTableColumnVo> getMyJoinTableColumnList(SqlDecoratorVo sqlDecoratorVo) {
        List<JoinTableColumnVo> joinTableColumnVoList = new ArrayList<>();
        for (ConditionGroupVo conditionGroupVo : sqlDecoratorVo.getConditionGroupList()) {
            for (ConditionVo conditionVo : conditionGroupVo.getConditionList()) {
                List<String> valueList = JSON.parseArray(JSON.toJSONString(conditionVo.getValueList()), String.class);
                for (String value : valueList) {
                    MyProcessTask<List<JoinTableColumnVo>> result = joinTableSqlMap.get(value);
                    if (result != null) {
                        result.build(joinTableColumnVoList);
                    }
                }
            }
        }
        return joinTableColumnVoList;
    }
}
