/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.process.groupsearch;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.process.constvalue.ProcessTaskGroupSearch;
import neatlogic.framework.process.constvalue.ProcessUserType;
import neatlogic.framework.process.dao.mapper.task.TaskMapper;
import neatlogic.framework.process.dto.TaskConfigVo;
import neatlogic.framework.restful.groupsearch.core.GroupSearchOptionVo;
import neatlogic.framework.restful.groupsearch.core.GroupSearchVo;
import neatlogic.framework.restful.groupsearch.core.IGroupSearchHandler;
import neatlogic.framework.util.$;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProcessUserTypeGroupHandler implements IGroupSearchHandler {
    @Override
    public String getName() {
        return ProcessTaskGroupSearch.PROCESSUSERTYPE.getValue();
    }

    @Override
    public String getLabel() {
        return ProcessTaskGroupSearch.PROCESSUSERTYPE.getText();
    }

    @Override
    public String getHeader() {
        return getName() + "#";
    }

    @Resource
    TaskMapper taskMapper;

    @Override
    public List<GroupSearchOptionVo> search(GroupSearchVo groupSearchVo) {
        List<String> includeStrList = groupSearchVo.getIncludeList();
        if (CollectionUtils.isEmpty(includeStrList)) {
            includeStrList = new ArrayList<>();
        }
        List<String> excludeList = groupSearchVo.getExcludeList();
        List<String> valuelist = new ArrayList<>();
        List<GroupSearchOptionVo> userTypeList = new ArrayList<>();
        for (ProcessUserType s : ProcessUserType.values()) {
            if (s.getIsShow() && s.getText().contains(groupSearchVo.getKeyword())) {
                String value = getHeader() + s.getValue();
                if (!valuelist.contains(value)) {
                    valuelist.add(value);
                    GroupSearchOptionVo groupSearchOptionVo = new GroupSearchOptionVo();
                    groupSearchOptionVo.setValue(value);
                    groupSearchOptionVo.setText(s.getText());
                    userTypeList.add(groupSearchOptionVo);
                }
            }
            if (includeStrList.contains(getHeader() + s.getValue())) {
                if (userTypeList.stream().noneMatch(o -> Objects.equals(o.getValue(), s.getValue()))) {
                    String value = getHeader() + s.getValue();
                    if (!valuelist.contains(value)) {
                        valuelist.add(value);
                        GroupSearchOptionVo groupSearchOptionVo = new GroupSearchOptionVo();
                        groupSearchOptionVo.setValue(value);
                        groupSearchOptionVo.setText(s.getText());
                        userTypeList.add(groupSearchOptionVo);
                    }
                }
            }
        }
        //任务
        if (CollectionUtils.isNotEmpty(excludeList) && !excludeList.contains("processUserType#" + ProcessUserType.MINOR.getValue())) {
            TaskConfigVo configParam = new TaskConfigVo();
            configParam.setKeyword(groupSearchVo.getKeyword());
            List<TaskConfigVo> taskConfigVoList = taskMapper.searchTaskConfig(configParam);
            if (CollectionUtils.isNotEmpty(taskConfigVoList)) {
                for (TaskConfigVo configVo : taskConfigVoList) {
                    String value = getHeader() + configVo.getId().toString();
                    if (!valuelist.contains(value)) {
                        valuelist.add(value);
                        GroupSearchOptionVo groupSearchOptionVo = new GroupSearchOptionVo();
                        groupSearchOptionVo.setValue(value);
                        groupSearchOptionVo.setText(configVo.getName() + $.t("common.worker"));
                        userTypeList.add(groupSearchOptionVo);
                    }
                }
            }
        }
        return userTypeList;
    }

    @Override
    public List<GroupSearchOptionVo> reload(GroupSearchVo groupSearchVo) {
        List<GroupSearchOptionVo> userTypeList = new ArrayList<>();
        List<String> valueList = groupSearchVo.getValueList();
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (String value : valueList) {
                if (value.startsWith(getHeader())) {
                    String realValue = value.replace(getHeader(), "");
                    String text = ProcessUserType.getText(realValue);
                    if (StringUtils.isNotBlank(text)) {
                        GroupSearchOptionVo groupSearchOptionVo = new GroupSearchOptionVo();
                        groupSearchOptionVo.setValue(value);
                        groupSearchOptionVo.setText(text);
                        userTypeList.add(groupSearchOptionVo);
                    }
                }
            }
            List<TaskConfigVo> configVoList = taskMapper.getTaskConfigByIdList(JSONArray.parseArray(JSONArray.toJSONString(valueList.stream().map(v -> v.replace(getHeader(), "")).collect(Collectors.toList()))));
            if (CollectionUtils.isNotEmpty(configVoList)) {
                configVoList.forEach(o -> {
                    GroupSearchOptionVo groupSearchOptionVo = new GroupSearchOptionVo();
                    groupSearchOptionVo.setValue(getHeader() + o.getId().toString());
                    groupSearchOptionVo.setText(o.getName() + $.t("common.worker"));
                    userTypeList.add(groupSearchOptionVo);
                });
            }
        }
        return userTypeList;
    }

    @Override
    public int getSort() {
        return 1;
    }

    @Override
    public Boolean isLimit() {
        return false;
    }
}
