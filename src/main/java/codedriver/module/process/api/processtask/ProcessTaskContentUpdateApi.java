package codedriver.module.process.api.processtask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.constvalue.ProcessStepType;
import codedriver.framework.process.constvalue.ProcessTaskAuditDetailType;
import codedriver.framework.process.constvalue.ProcessTaskStepAction;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.ProcessTaskContentVo;
import codedriver.framework.process.dto.ProcessTaskFileVo;
import codedriver.framework.process.dto.ProcessTaskStepContentVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.exception.core.ProcessTaskRuntimeException;
import codedriver.framework.process.exception.processtask.ProcessTaskNotFoundException;
import codedriver.framework.process.exception.processtask.ProcessTaskStepNotFoundException;
import codedriver.framework.process.stephandler.core.IProcessStepHandler;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerFactory;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;

@Service
@Transactional
public class ProcessTaskContentUpdateApi extends ApiComponentBase {
	
	@Autowired
	private ProcessTaskMapper processTaskMapper;
	
	@Override
	public String getToken() {
		return "processtask/content/update";
	}

	@Override
	public String getName() {
		return "工单上报描述内容及附件更新接口";
	}

	@Override
	public String getConfig() {
		return null;
	}
	@Input({
		@Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "工单id"),
		@Param(name = "processTaskStepId", type = ApiParamType.LONG, desc = "步骤id"),
		@Param(name = "content", type = ApiParamType.STRING, desc = "描述"),
		@Param(name = "fileUuidList", type=ApiParamType.JSONARRAY, desc = "附件uuid列表")
	})
	@Description(desc = "工单上报描述内容及附件更新接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long processTaskId = jsonObj.getLong("processTaskId");
		ProcessTaskVo processTaskVo = processTaskMapper.getProcessTaskById(processTaskId);
		if(processTaskVo == null) {
			throw new ProcessTaskNotFoundException(processTaskId.toString());
		}
		// 锁定当前流程
		processTaskMapper.getProcessTaskLockById(processTaskId);
		
		ProcessTaskStepVo processTaskStepVo = new ProcessTaskStepVo();
		processTaskStepVo.setProcessTaskId(processTaskId);
		Long processTaskStepId = jsonObj.getLong("processTaskStepId");
		if(processTaskStepId != null) {
			processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
			if(processTaskStepVo == null) {
				throw new ProcessTaskStepNotFoundException(processTaskStepId.toString());
			}
			if(!processTaskId.equals(processTaskStepVo.getProcessTaskId())) {
				throw new ProcessTaskRuntimeException("步骤：'" + processTaskStepId + "'不是工单：'" + processTaskId + "'的步骤");
			}
		}
		
		IProcessStepHandler handler = ProcessStepHandlerFactory.getHandler();
		handler.verifyActionAuthoriy(processTaskId, processTaskStepId, ProcessTaskStepAction.UPDATE);
		
		//获取开始步骤id
		List<ProcessTaskStepVo> processTaskStepList = processTaskMapper.getProcessTaskStepByProcessTaskIdAndType(processTaskId, ProcessStepType.START.getValue());
		if(processTaskStepList.size() != 1) {
			throw new ProcessTaskRuntimeException("工单：'" + processTaskId + "'有" + processTaskStepList.size() + "个开始步骤");
		}
		Long startProcessTaskStepId = processTaskStepList.get(0).getId();
		//获取上报描述内容hash
		String oldContentHash = null;
		List<ProcessTaskStepContentVo> processTaskStepContentList = processTaskMapper.getProcessTaskStepContentProcessTaskStepId(startProcessTaskStepId);
		if(!processTaskStepContentList.isEmpty()) {
			oldContentHash = processTaskStepContentList.get(0).getContentHash();
			jsonObj.put(ProcessTaskAuditDetailType.CONTENT.getOldDataParamName(), oldContentHash);
		}
		//获取上传附件uuid
		String newFileUuidListStr = jsonObj.getString("fileUuidList");
		String oldFileUuidListStr = null;
		ProcessTaskFileVo processTaskFileVo = new ProcessTaskFileVo();
		processTaskFileVo.setProcessTaskId(processTaskId);
		processTaskFileVo.setProcessTaskStepId(startProcessTaskStepId);
		List<ProcessTaskFileVo> processTaskFileList = processTaskMapper.searchProcessTaskFile(processTaskFileVo);
		if(processTaskFileList.size() > 0) {
			List<String> oldFileUuidList = new ArrayList<>();
			for(ProcessTaskFileVo processTaskFile : processTaskFileList) {
				oldFileUuidList.add(processTaskFile.getFileUuid());
			}
			oldFileUuidListStr = JSON.toJSONString(oldFileUuidList);
			ProcessTaskContentVo processTaskContentVo = new ProcessTaskContentVo(oldFileUuidListStr);
			jsonObj.put(ProcessTaskAuditDetailType.FILE.getOldDataParamName(), processTaskContentVo.getHash());
			processTaskMapper.deleteProcessTaskFile(processTaskFileVo);
		}
		/** 保存新附件uuid **/
		if (StringUtils.isNotBlank(newFileUuidListStr)) {
			List<String> fileUuidList = JSON.parseArray(newFileUuidListStr, String.class);
			for (String fileUuid : fileUuidList) {
				processTaskFileVo.setFileUuid(fileUuid);
				processTaskMapper.insertProcessTaskFile(processTaskFileVo);
			}
		}
		
		String newContentHash = null;
		String content = jsonObj.getString("content");
		if(StringUtils.isNotBlank(content)) {
			ProcessTaskContentVo contentVo = new ProcessTaskContentVo(content);
//			newContentHash = contentVo.getHash();
//			processTaskMapper.replaceProcessTaskContent(contentVo);
			processTaskMapper.replaceProcessTaskStepContent(new ProcessTaskStepContentVo(processTaskId, startProcessTaskStepId, contentVo.getHash()));
//			jsonObj.put(ProcessTaskAuditDetailType.CONTENT.getParamName(), newContentHash);
		}else if(oldContentHash != null){
			processTaskMapper.deleteProcessTaskStepContent(new ProcessTaskStepContentVo(processTaskId, startProcessTaskStepId, oldContentHash));
		}
		//生成活动
		if(!Objects.equals(oldContentHash, newContentHash) || !Objects.equals(oldFileUuidListStr, newFileUuidListStr)) {
			processTaskStepVo.setParamObj(jsonObj);
			handler.activityAudit(processTaskStepVo, ProcessTaskStepAction.UPDATECONTENT);
		}
		return null;
	}

}
