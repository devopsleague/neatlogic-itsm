package codedriver.module.process.audithandler.handler;

import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.process.audithandler.core.IProcessTaskStepAuditDetailHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.base.Objects;

import codedriver.framework.process.constvalue.ProcessTaskAuditDetailType;
import codedriver.framework.process.dao.mapper.SelectContentByHashMapper;
import codedriver.framework.process.dto.ProcessTaskStepAuditDetailVo;
import codedriver.framework.process.dto.ProcessTaskStepSubtaskVo;
@Service
public class SubtaskAuditHandler implements IProcessTaskStepAuditDetailHandler {
    @Autowired
    private SelectContentByHashMapper selectContentByHashMapper;
    
	@Override
	public String getType() {
		return ProcessTaskAuditDetailType.SUBTASK.getValue();
	}

	@Override
	public int handle(ProcessTaskStepAuditDetailVo processTaskStepAuditDetailVo) {
		if(StringUtils.isNotBlank(processTaskStepAuditDetailVo.getNewContent())) {
			ProcessTaskStepSubtaskVo processTaskStepSubtaskVo = JSON.parseObject(processTaskStepAuditDetailVo.getNewContent(), new TypeReference<ProcessTaskStepSubtaskVo>(){});
			processTaskStepSubtaskVo.setContent(selectContentByHashMapper.getProcessTaskContentStringByHash(processTaskStepSubtaskVo.getContentHash()));
			JSONObject content = new JSONObject();
			content.put("type", "content");
			content.put("typeName", "描述");
			content.put("newContent", processTaskStepSubtaskVo.getContent());
			content.put("changeType", "new");
			
			JSONObject targetTime = new JSONObject();
			targetTime.put("type", "targetTime");
			targetTime.put("typeName", "期望时间");
			targetTime.put("newContent", processTaskStepSubtaskVo.getTargetTime());
			targetTime.put("changeType", "new");

			JSONObject userName = new JSONObject();
			userName.put("type", "userName");
			userName.put("typeName", "处理人");
			userName.put("initType", GroupSearch.USER.getValue());
			userName.put("uuid", processTaskStepSubtaskVo.getUserUuid());
			userName.put("newContent", processTaskStepSubtaskVo.getUserName());
			userName.put("changeType", "new");

			if(StringUtils.isNotBlank(processTaskStepAuditDetailVo.getOldContent())) {
				ProcessTaskStepSubtaskVo oldProcessTaskStepSubtaskVo = JSON.parseObject(processTaskStepAuditDetailVo.getOldContent(), new TypeReference<ProcessTaskStepSubtaskVo>(){});		
				oldProcessTaskStepSubtaskVo.setContent(selectContentByHashMapper.getProcessTaskContentStringByHash(oldProcessTaskStepSubtaskVo.getContentHash()));
				if(!Objects.equal(oldProcessTaskStepSubtaskVo.getContent(), processTaskStepSubtaskVo.getContent())) {
					content.put("oldContent", oldProcessTaskStepSubtaskVo.getContent());
					content.put("changeType", "update");
				}
				if(!Objects.equal(oldProcessTaskStepSubtaskVo.getTargetTime(), processTaskStepSubtaskVo.getTargetTime())) {
					targetTime.put("oldContent", oldProcessTaskStepSubtaskVo.getTargetTime());
					targetTime.put("changeType", "update");
				}
				if(!Objects.equal(oldProcessTaskStepSubtaskVo.getUserName(), processTaskStepSubtaskVo.getUserName())) {
					userName.put("oldContent", oldProcessTaskStepSubtaskVo.getUserName());
					userName.put("changeType", "update");
				}
			}

			JSONArray subtask = new JSONArray();
			subtask.add(content);
			subtask.add(targetTime);
			subtask.add(userName);
			processTaskStepAuditDetailVo.setOldContent(null);
			processTaskStepAuditDetailVo.setNewContent(JSON.toJSONString(subtask));
		}
		return 1;
	}

}
