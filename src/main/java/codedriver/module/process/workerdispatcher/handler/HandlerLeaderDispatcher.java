package codedriver.module.process.workerdispatcher.handler;

import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.TeamUserTitleVo;
import codedriver.framework.dto.TeamVo;
import codedriver.framework.dto.UserTitleVo;
import codedriver.framework.exception.team.TeamUserTitleNotFoundException;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.workerdispatcher.core.WorkerDispatcherBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class HandlerLeaderDispatcher extends WorkerDispatcherBase {

    @Resource
    private ProcessTaskMapper processTaskMapper;

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public String getName() {
        return "处理人领导分派器";
    }

    @Override
    public JSONArray getConfig() {
        JSONArray resultArray = new JSONArray();
        /** 前置步骤 **/
        JSONObject preStepJsonObj = new JSONObject();
        preStepJsonObj.put("type", "select");
        preStepJsonObj.put("name", "preStepList");
        preStepJsonObj.put("label", "前置步骤");
        preStepJsonObj.put("validateList", Collections.singletonList("required"));
        preStepJsonObj.put("multiple", true);
        preStepJsonObj.put("policy", "preStepList");
        resultArray.add(preStepJsonObj);
        /** 选择头衔 **/
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("type", "select");
        jsonObj.put("name", "teamUserTitle");
        jsonObj.put("search", true);
        jsonObj.put("dynamicUrl", "api/rest/user/title/search");
        jsonObj.put("label", "头衔");
        jsonObj.put("validateList", Collections.singletonList("required"));
        jsonObj.put("multiple", false);
        jsonObj.put("textName", "name");
        jsonObj.put("valueName", "name");
        jsonObj.put("rootName", "tbodyList");
        jsonObj.put("value", "");
        jsonObj.put("defaultValue", "");
        resultArray.add(jsonObj);
        return resultArray;
    }

    @Override
    public String getHelp() {
        return "在上报人所在的组及父级组中，找出与选择头衔相同的用户作为当前步骤的处理人";
    }

    @Override
    protected List<String> myGetWorker(ProcessTaskStepVo processTaskStepVo, JSONObject configObj) {
        List<String> resultList = new ArrayList<>();
        String teamUserTitle = configObj.getString("teamUserTitle");
        if (StringUtils.isNotBlank(teamUserTitle)) {
            UserTitleVo userTitleVo = userMapper.getUserTitleByName(teamUserTitle);
            if (userTitleVo == null) {
                throw new TeamUserTitleNotFoundException(teamUserTitle);
            }
            ProcessTaskVo processTask = processTaskMapper.getProcessTaskById(processTaskStepVo.getProcessTaskId());
            List<TeamVo> teamList = teamMapper.getTeamListByUserUuid(processTask.getOwner());
            if (CollectionUtils.isNotEmpty(teamList)) {
                //需要逐级分组往上找，找到第一个符合的头衔的用户s
                for (TeamVo teamVo : teamList) {
                    List<TeamUserTitleVo> teamUserTitleVoList = teamMapper.getTeamUserTitleListByTeamlrAndTitleId(teamVo.getLft(), teamVo.getRht(), userTitleVo.getId());
                    if (CollectionUtils.isNotEmpty(teamUserTitleVoList)) {
                        resultList.addAll(teamUserTitleVoList.get(0).getUserList());
                    }
                }
            }
        }
        return resultList;
    }

}