package codedriver.module.process.workerpolicy.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;

import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.process.constvalue.ProcessFormHandler;
import codedriver.framework.process.constvalue.WorkerPolicy;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.ProcessTaskFormAttributeDataVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskStepWorkerPolicyVo;
import codedriver.framework.process.dto.ProcessTaskStepWorkerVo;
import codedriver.framework.process.workerpolicy.core.IWorkerPolicyHandler;

@Service
public class FormWorkerPolicyHandler implements IWorkerPolicyHandler {

	@Override
	public String getType() {
		return WorkerPolicy.FORM.getValue();
	}

	@Override
	public String getName() {
		return WorkerPolicy.FORM.getText();
	}
	
	@Autowired
	private ProcessTaskMapper processTaskMapper;
	
	@Autowired
	private UserMapper userMapper;
	
	@Autowired
	private TeamMapper teamMapper;
	
	@Autowired
	private RoleMapper roleMapper;

	@Override
	public List<ProcessTaskStepWorkerVo> execute(ProcessTaskStepWorkerPolicyVo workerPolicyVo, ProcessTaskStepVo currentProcessTaskStepVo) {
		List<ProcessTaskStepWorkerVo> processTaskStepWorkerList = new ArrayList<>();
		if (MapUtils.isNotEmpty(workerPolicyVo.getConfigObj())) {
			/** 选择的表单属性uuid **/
			String attributeUuid = workerPolicyVo.getConfigObj().getString("attributeUuid");
			if(StringUtils.isNotBlank(attributeUuid)) {
				ProcessTaskFormAttributeDataVo processTaskFormAttributeDataVo = new ProcessTaskFormAttributeDataVo();
				processTaskFormAttributeDataVo.setProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
				processTaskFormAttributeDataVo.setAttributeUuid(attributeUuid);
				ProcessTaskFormAttributeDataVo processTaskFormAttributeData = processTaskMapper.getProcessTaskFormAttributeDataByProcessTaskIdAndAttributeUuid(processTaskFormAttributeDataVo);
				if(processTaskFormAttributeData != null) {
					/** 只有表单属性类型为用户选择器才生效**/
					if(ProcessFormHandler.FORMUSERSELECT.getHandler().equals(processTaskFormAttributeData.getType())) {
						String data = processTaskFormAttributeData.getData();
						if(StringUtils.isNotBlank(data)) {
							List<String> dataList = new ArrayList<>();
							if(data.startsWith("[") && data.endsWith("]")) {
								dataList = JSON.parseArray(data, String.class);
							}else {
								dataList.add(data);
							}
							if(CollectionUtils.isNotEmpty(dataList)) {
								for(String value : dataList) {
									/** 校验属性值是否合法，只有是当前存在的用户、组、角色才合法**/
									if(value.contains("#")) {
										String[] split = value.split("#");
										if(GroupSearch.USER.getValue().equals(split[0])) {
											if(userMapper.checkUserIsExists(split[1]) == 0) {
												continue;
											}
										}else if(GroupSearch.TEAM.getValue().equals(split[0])) {
											if(teamMapper.checkTeamIsExists(split[1]) == 0) {
												continue;
											}
										}else if(GroupSearch.ROLE.getValue().equals(split[0])) {
											if(roleMapper.checkRoleIsExists(split[1]) == 0) {
												continue;
											}
										}else {
											continue;
										}
										processTaskStepWorkerList.add(new ProcessTaskStepWorkerVo(currentProcessTaskStepVo.getProcessTaskId(), currentProcessTaskStepVo.getId(), split[0], split[1]));
									}
								}
							}
						}
					}
				}
			}			
		}
		return processTaskStepWorkerList;
	}
}
