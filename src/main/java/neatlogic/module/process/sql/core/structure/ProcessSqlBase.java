/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.process.sql.core.structure;

import neatlogic.framework.dto.condition.*;
import neatlogic.framework.exception.condition.ConditionNotFoundException;
import neatlogic.framework.process.column.core.IProcessTaskColumn;
import neatlogic.framework.process.condition.core.IProcessTaskCondition;
import neatlogic.framework.process.condition.core.ProcessTaskConditionFactory;
import neatlogic.framework.process.constvalue.ProcessFieldType;
import neatlogic.framework.process.dto.SqlDecoratorVo;
import neatlogic.framework.process.workcenter.dto.JoinTableColumnVo;
import neatlogic.framework.process.workcenter.dto.SelectColumnVo;
import neatlogic.framework.process.workcenter.dto.TableSelectColumnVo;
import neatlogic.module.process.sql.IProcessSqlStructure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static neatlogic.framework.common.util.CommonUtil.distinctByKey;

public abstract class ProcessSqlBase<T extends SqlDecoratorVo> implements IProcessSqlStructure<T> {
    /**
     * @Description: 如果是distinct id 则 只需要 根据条件获取需要的表
     * @Author: 89770
     * @Date: 2021/1/20 16:36
     * @Params: []
     * @Returns: void
     **/
    protected List<JoinTableColumnVo> getJoinTableOfCondition(StringBuilder sb, SqlDecoratorVo sqlDecoratorVo) {
        List<JoinTableColumnVo> joinTableColumnList = new ArrayList<>();
        //根据接口入参的返回需要的conditionList,然后获取需要关联的tableList
        List<ConditionGroupVo> groupList = sqlDecoratorVo.getConditionGroupList();
        for (ConditionGroupVo groupVo : groupList) {
            List<ConditionVo> conditionVoList = groupVo.getConditionList();
            for (ConditionVo conditionVo : conditionVoList) {
                IProcessTaskCondition conditionHandler = ProcessTaskConditionFactory.getHandler(conditionVo.getName());
                if (conditionHandler != null) {
                    List<JoinTableColumnVo> handlerJoinTableColumnList = conditionHandler.getJoinTableColumnList(sqlDecoratorVo);
                    joinTableColumnList.addAll(handlerJoinTableColumnList);
                }
            }
        }
        return joinTableColumnList;
    }

    protected List<JoinTableColumnVo> getJoinTableColumnList(Map<String, IProcessTaskColumn> columnComponentMap, String columnName) {
        IProcessTaskColumn column = columnComponentMap.get(columnName);
        List<JoinTableColumnVo> handlerJoinTableColumnList = column.getJoinTableColumnList();
        return handlerJoinTableColumnList.stream().filter(distinctByKey(JoinTableColumnVo::getHash)).collect(Collectors.toList());
    }


    /**
     * @Description: count , distinct id 则 需要 根据条件获取需要的表。
     * 已过期 应该使用ConditionConfigVo.buildConditionWhereSql
     * @Author: 89770
     * @Date: 2021/2/8 16:33
     * @Params: [sqlSb, sqlDecoratorVo]
     * @Returns: void
     **/
    @Deprecated
    protected void buildOtherConditionWhereSql(StringBuilder sqlSb, ConditionConfigVo conditionConfigVo) {
        // 将group 以连接表达式 存 Map<fromUuid_toUuid,joinType>
        Map<String, String> groupRelMap = new HashMap<>();
        List<ConditionGroupRelVo> groupRelList = conditionConfigVo.getConditionGroupRelList();
        if (CollectionUtils.isNotEmpty(groupRelList)) {
            for (ConditionGroupRelVo groupRel : groupRelList) {
                groupRelMap.put(groupRel.getFrom() + "_" + groupRel.getTo(), groupRel.getJoinType());
            }
        }
        List<ConditionGroupVo> groupList = conditionConfigVo.getConditionGroupList();
        if (CollectionUtils.isNotEmpty(groupList)) {
            String fromGroupUuid = null;
            String toGroupUuid = groupList.get(0).getUuid();
            boolean isAddedAnd = false;
            for (ConditionGroupVo groupVo : groupList) {
                // 将condition 以连接表达式 存 Map<fromUuid_toUuid,joinType>
                Map<String, String> conditionRelMap = new HashMap<>();
                List<ConditionRelVo> conditionRelList = groupVo.getConditionRelList();
                if (CollectionUtils.isNotEmpty(conditionRelList)) {
                    for (ConditionRelVo conditionRel : conditionRelList) {
                        conditionRelMap.put(conditionRel.getFrom() + "_" + conditionRel.getTo(),
                                conditionRel.getJoinType());
                    }
                }
                //append joinType
                if (fromGroupUuid != null) {
                    toGroupUuid = groupVo.getUuid();
                    sqlSb.append(groupRelMap.get(fromGroupUuid + "_" + toGroupUuid));
                }
                List<ConditionVo> conditionVoList = groupVo.getConditionList();
                if (!isAddedAnd && CollectionUtils.isNotEmpty((conditionVoList))) {
                    //补充整体and 结束左括号
                    sqlSb.append(" and (");
                    isAddedAnd = true;
                }
                sqlSb.append(" ( ");
                String fromConditionUuid = null;
                String toConditionUuid;
                for (int i = 0; i < conditionVoList.size(); i++) {
                    ConditionVo conditionVo = conditionVoList.get(i);
                    //append joinType
                    toConditionUuid = conditionVo.getUuid();
                    if (MapUtils.isNotEmpty(conditionRelMap) && StringUtils.isNotBlank(fromConditionUuid)) {
                        sqlSb.append(conditionRelMap.get(fromConditionUuid + "_" + toConditionUuid));
                    }
                    //append condition
                    String handler = conditionVo.getName();
                    //如果是form
                    if (conditionVo.getType().equals(ProcessFieldType.FORM.getValue())) {
                        handler = ProcessFieldType.FORM.getValue();
                    }
                    IProcessTaskCondition sqlCondition = ProcessTaskConditionFactory.getHandler(handler);
                    if(sqlCondition == null){
                        throw new ConditionNotFoundException(handler);
                    }
                    sqlCondition.getSqlConditionWhere(groupVo, i, sqlSb);
                    fromConditionUuid = toConditionUuid;
                }
                sqlSb.append(" ) ");
                fromGroupUuid = toGroupUuid;

            }
            //补充整体and 结束右括号
            if(isAddedAnd){
                sqlSb.append(" ) ");
            }
        }
    }

    /**
     * 获取column sql list
     *
     * @param columnComponentMap 需要展示thead map
     * @param columnList         sql columnList 防止重复取column
     * @param theadName          具体的column
     * @param isGroup            是否group,目前用于dashboard统计
     */
    protected void getColumnSqlList(Map<String, IProcessTaskColumn> columnComponentMap, List<String> columnList, String theadName, Boolean isGroup) {
        if (columnComponentMap.containsKey(theadName)) {
            IProcessTaskColumn column = columnComponentMap.get(theadName);
            for (TableSelectColumnVo tableSelectColumnVo : column.getTableSelectColumn()) {
                if (!isGroup || tableSelectColumnVo.getColumnList().stream().anyMatch(SelectColumnVo::getIsPrimary)) {
                    for (SelectColumnVo selectColumnVo : tableSelectColumnVo.getColumnList()) {
                        String format = " %s.%s as %s ";
                        if (StringUtils.isNotBlank(selectColumnVo.getColumnFormat())) {
                            format = selectColumnVo.getColumnFormat();
                        }
                        String columnStr = String.format(format, tableSelectColumnVo.getTableShortName(), selectColumnVo.getColumnName(), selectColumnVo.getPropertyName());
                        if (!columnList.contains(columnStr)) {
                            columnList.add(columnStr);
                        }
                    }
                }
            }

        }
    }
}
