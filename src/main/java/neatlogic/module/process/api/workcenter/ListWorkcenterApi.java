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

package neatlogic.module.process.api.workcenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.batch.BatchRunner;
import neatlogic.framework.common.config.Config;
import neatlogic.framework.common.util.CommonUtil;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.process.auth.PROCESS_BASE;
import neatlogic.framework.process.auth.WORKCENTER_MODIFY;
import neatlogic.framework.process.auth.WORKCENTER_NEW_TYPE;
import neatlogic.framework.process.constvalue.ProcessWorkcenterInitType;
import neatlogic.framework.process.constvalue.ProcessWorkcenterType;
import neatlogic.framework.process.workcenter.dto.WorkcenterUserProfileVo;
import neatlogic.framework.process.workcenter.dto.WorkcenterVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.process.dao.mapper.workcenter.WorkcenterMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListWorkcenterApi extends PrivateApiComponentBase {
    @Resource
    WorkcenterMapper workcenterMapper;

    @Override
    public String getToken() {
        return "workcenter/list";
    }

    @Override
    public String getName() {
        return "获取工单中心分类列表接口";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Output({@Param(name = "workcenter", explode = WorkcenterVo.class, desc = "分类信息")})
    @Description(desc = "获取工单中心分类列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject workcenterJson = new JSONObject();
        String userUuid = UserContext.get().getUserUuid(true);
        AuthenticationInfoVo authenticationInfoVo = UserContext.get().getAuthenticationInfoVo();
        int isHasModifiedAuth = Boolean.TRUE.equals(AuthActionChecker.check(WORKCENTER_MODIFY.class)) ? 1 : 0;
        int isHasNewTypeAuth = Boolean.TRUE.equals(AuthActionChecker.check(WORKCENTER_NEW_TYPE.class)) ? 1 : 0;
        List<WorkcenterVo> workcenterList = new ArrayList<>();
        String viewType = "table";//默认table展示
        WorkcenterVo workcenterVo = new WorkcenterVo();
        workcenterVo.setDevice(CommonUtil.getDevice());
        List<String> authList = new ArrayList<>();
        authList.add(userUuid);
        authList.addAll(authenticationInfoVo.getTeamUuidList());
        authList.addAll(authenticationInfoVo.getRoleUuidList());
        workcenterVo.setAuthList(authList);
        workcenterVo.setOwner(userUuid);
        //根据用户（用户、组、角色）授权、支持设备和是否拥有工单中心管理权限，查出工单分类列表
        List<String> workcenterUuidList = workcenterMapper.getAuthorizedWorkcenterUuidList(workcenterVo, isHasModifiedAuth, isHasNewTypeAuth);
        if (CollectionUtils.isNotEmpty(workcenterUuidList)) {
            workcenterList = workcenterMapper.getAuthorizedWorkcenterListByUuidList(workcenterUuidList);
            WorkcenterUserProfileVo userProfile = workcenterMapper.getWorkcenterUserProfileByUserUuid(userUuid);
            Map<String, Integer> workcenterUserSortMap = new HashMap<>();
            boolean isWorkcenterManager = AuthActionChecker.check(WORKCENTER_MODIFY.class.getSimpleName());
            if (userProfile != null) {
                JSONObject userConfig = JSON.parseObject(userProfile.getConfig());
                if (userConfig.containsKey("viewType")) {
                    viewType = userConfig.getString("viewType");
                }
                if (userConfig.containsKey("workcenterList")) {
                    JSONArray workcenterSortList = userConfig.getJSONArray("workcenterList");
                    for (Object workcenterSort : workcenterSortList) {
                        JSONObject workcenterSortJson = (JSONObject) workcenterSort;
                        workcenterUserSortMap.put(workcenterSortJson.getString("uuid"), workcenterSortJson.getInteger("sort"));
                    }
                }
            }
            BatchRunner<WorkcenterVo> runner = new BatchRunner<>();
//            runner.execute(workcenterList, 3, (threadIndex, dataIndex, workcenter) -> {
            for (WorkcenterVo workcenter : workcenterList) {
                if (workcenter.getType().equals(ProcessWorkcenterType.FACTORY.getValue())) {
                    workcenter.setIsCanEdit(isWorkcenterManager ? 1 : 0);
                    if (Arrays.asList(ProcessWorkcenterInitType.ALL_PROCESSTASK.getValue(), ProcessWorkcenterInitType.DRAFT_PROCESSTASK.getValue(), ProcessWorkcenterInitType.DONE_OF_MINE_PROCESSTASK.getValue(), ProcessWorkcenterInitType.PROCESSING_OF_MINE_PROCESSTASK.getValue()).contains(workcenter.getUuid()) && isWorkcenterManager) {
                        workcenter.setIsCanRole(1);
                    }
                }
                if (workcenter.getType().equals(ProcessWorkcenterType.SYSTEM.getValue()) && isWorkcenterManager) {
                    workcenter.setIsCanEdit(1);
                    workcenter.setIsCanRole(1);
                } else if (workcenter.getType().equals(ProcessWorkcenterType.CUSTOM.getValue())) {
                    if (UserContext.get().getUserUuid(true).equalsIgnoreCase(workcenter.getOwner())) {
                        workcenter.setIsCanEdit(1);
                        if (AuthActionChecker.check(WORKCENTER_MODIFY.class.getSimpleName())) {
                            workcenter.setIsCanRole(1);
                        } else {
                            workcenter.setIsCanRole(0);
                        }
                    } else {
                        workcenter.setIsCanEdit(0);
                        workcenter.setIsCanRole(0);
                    }
                }

                workcenter.getHandlerType();
                workcenter.setConditionConfig(null);
                workcenter.setConditionConfigStr(null);
                //排序 用户设置的排序优先
                if (workcenterUserSortMap.containsKey(workcenter.getUuid())) {
                    workcenter.setSort(workcenterUserSortMap.get(workcenter.getUuid()));
                }
                //去除返回前端的多余字段
                workcenter.setConditionGroupList(null);
                workcenter.setConditionGroupRelList(null);
//            }, "WORKCENTER-LIST-SEARCHER");
            }
        }
        workcenterJson.put("mobileIsOnline", Config.MOBILE_IS_ONLINE());
        workcenterJson.put("viewType", viewType);
        workcenterJson.put("workcenterList", workcenterList.stream().sorted(Comparator.comparing(WorkcenterVo::getSort)).collect(Collectors.toList()));
        return workcenterJson;
    }
}
