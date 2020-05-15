package codedriver.module.process.api.processtask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.apiparam.core.ApiParamType;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.file.dao.mapper.FileMapper;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.process.constvalue.ProcessTaskAuditDetailType;
import codedriver.framework.process.constvalue.ProcessTaskStepAction;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.ProcessTaskContentVo;
import codedriver.framework.process.dto.ProcessTaskFormAttributeDataVo;
import codedriver.framework.process.dto.ProcessTaskStepAuditVo;
import codedriver.framework.process.dto.ProcessTaskStepCommentVo;
import codedriver.framework.process.dto.ProcessTaskStepFormAttributeVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.exception.core.ProcessTaskRuntimeException;
import codedriver.framework.process.exception.process.ProcessStepHandlerNotFoundException;
import codedriver.framework.process.exception.processtask.ProcessTaskNotFoundException;
import codedriver.framework.process.exception.processtask.ProcessTaskStepNotFoundException;
import codedriver.framework.process.stephandler.core.IProcessStepHandler;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerFactory;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.process.service.ProcessTaskService;
@Service
@Transactional
public class ProcessTaskCommentApi extends ApiComponentBase {

	@Autowired
	private ProcessTaskMapper processTaskMapper;
	
	@Autowired
	private ProcessTaskService processTaskService;
	
	@Autowired
	private FileMapper fileMapper;
	
	@Override
	public String getToken() {
		return "processtask/comment";
	}

	@Override
	public String getName() {
		return "工单回复接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name = "processTaskId", type = ApiParamType.LONG, isRequired = true, desc = "工单id"),
		@Param(name = "processTaskStepId", type = ApiParamType.LONG, isRequired = true, desc = "步骤id"),
		@Param(name = "formAttributeDataList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "表单属性数据列表"),
		@Param(name = "content", type = ApiParamType.STRING, xss = true, desc = "描述"),
		@Param(name = "fileUuidList", type=ApiParamType.JSONARRAY, desc = "附件uuid列表")
	})
	@Output({
		@Param(name = "commentList", explode = ProcessTaskStepCommentVo[].class, desc = "当前步骤评论列表")
	})
	@Description(desc = "工单回复接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long processTaskId = jsonObj.getLong("processTaskId");
		ProcessTaskVo processTaskVo = processTaskMapper.getProcessTaskById(processTaskId);
		if(processTaskVo == null) {
			throw new ProcessTaskNotFoundException(processTaskId.toString());
		}
		Long processTaskStepId = jsonObj.getLong("processTaskStepId");
	
		ProcessTaskStepVo processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(processTaskStepId);
		if(processTaskStepVo == null) {
			throw new ProcessTaskStepNotFoundException(processTaskStepId.toString());
		}
		if(!processTaskId.equals(processTaskStepVo.getProcessTaskId())) {
			throw new ProcessTaskRuntimeException("步骤：'" + processTaskStepId + "'不是工单：'" + processTaskId + "'的步骤");
		}

		IProcessStepHandler handler = ProcessStepHandlerFactory.getHandler(processTaskStepVo.getHandler());
		if(handler == null) {
			throw new ProcessStepHandlerNotFoundException(processTaskStepVo.getHandler());
		}
		handler.verifyActionAuthoriy(processTaskId, processTaskStepId, ProcessTaskStepAction.COMMENT);
		
		//删除暂存活动
		ProcessTaskStepAuditVo auditVo = new ProcessTaskStepAuditVo();
		auditVo.setProcessTaskId(processTaskId);
		auditVo.setProcessTaskStepId(processTaskStepId);
		auditVo.setAction(ProcessTaskStepAction.SAVE.getValue());
		auditVo.setUserId(UserContext.get().getUserId(true));
		List<ProcessTaskStepAuditVo> processTaskStepAuditList = processTaskMapper.getProcessTaskStepAuditList(auditVo);
		for(ProcessTaskStepAuditVo processTaskStepAudit : processTaskStepAuditList) {
			processTaskMapper.deleteProcessTaskStepAuditById(processTaskStepAudit.getId());
		}
		//表单属性显示控制
		Map<String, String> formAttributeActionMap = new HashMap<>();
		List<ProcessTaskStepFormAttributeVo> processTaskStepFormAttributeList = processTaskMapper.getProcessTaskStepFormAttributeByProcessTaskStepId(processTaskStepId);
		if(processTaskStepFormAttributeList.size() > 0) {
			for(ProcessTaskStepFormAttributeVo processTaskStepFormAttributeVo : processTaskStepFormAttributeList) {
				formAttributeActionMap.put(processTaskStepFormAttributeVo.getAttributeUuid(), processTaskStepFormAttributeVo.getAction());
			}
		}
		//获取旧表单数据
		List<ProcessTaskFormAttributeDataVo> oldProcessTaskFormAttributeDataList = processTaskMapper.getProcessTaskStepFormAttributeDataByProcessTaskId(processTaskId);
		if(CollectionUtils.isNotEmpty(oldProcessTaskFormAttributeDataList)) {
			Iterator<ProcessTaskFormAttributeDataVo> iterator = oldProcessTaskFormAttributeDataList.iterator();
			while(iterator.hasNext()) {
				ProcessTaskFormAttributeDataVo processTaskFormAttributeDataVo = iterator.next();
				String attributeUuid = processTaskFormAttributeDataVo.getAttributeUuid();
				if(formAttributeActionMap.get(attributeUuid) != null) {//只读或隐藏
					iterator.remove();
				}
			}
			oldProcessTaskFormAttributeDataList.sort(ProcessTaskFormAttributeDataVo::compareTo);
			jsonObj.put(ProcessTaskAuditDetailType.FORM.getOldDataParamName(), JSON.toJSONString(oldProcessTaskFormAttributeDataList));
		}
		//写入当前步骤的表单属性值
		JSONArray formAttributeDataList = jsonObj.getJSONArray("formAttributeDataList");
		if(CollectionUtils.isNotEmpty(formAttributeDataList)) {
			List<ProcessTaskFormAttributeDataVo> processTaskFormAttributeDataList = new ArrayList<>(formAttributeDataList.size());
			for(int i = 0; i < formAttributeDataList.size(); i++) {
				JSONObject formAttributeDataObj = formAttributeDataList.getJSONObject(i);
				String attributeUuid = formAttributeDataObj.getString("attributeUuid");
				if(formAttributeActionMap.get(attributeUuid) != null) {//对于只读或隐藏的属性，当前用户不能修改，不更新数据库中的值，不进行修改前后对比
					continue;
				}
				ProcessTaskFormAttributeDataVo attributeData = new ProcessTaskFormAttributeDataVo();
				String dataList = formAttributeDataObj.getString("dataList");
				attributeData.setData(dataList);
				attributeData.setProcessTaskId(processTaskId);
				attributeData.setAttributeUuid(formAttributeDataObj.getString("attributeUuid"));
				attributeData.setType(formAttributeDataObj.getString("handler"));
				attributeData.setSort(i);
				processTaskMapper.replaceProcessTaskFormAttributeData(attributeData);
				processTaskFormAttributeDataList.add(attributeData);
			}
			processTaskFormAttributeDataList.sort(ProcessTaskFormAttributeDataVo::compareTo);
			jsonObj.put(ProcessTaskAuditDetailType.FORM.getParamName(), JSON.toJSONString(processTaskFormAttributeDataList));
		}
		ProcessTaskStepCommentVo processTaskStepCommentVo = new ProcessTaskStepCommentVo();
		processTaskStepCommentVo.setProcessTaskId(processTaskId);
		processTaskStepCommentVo.setProcessTaskStepId(processTaskStepId);
		processTaskStepCommentVo.setFcu(UserContext.get().getUserId(true));
		ProcessTaskStepCommentVo commentVo = new ProcessTaskStepCommentVo();
		commentVo.setFcd(new Date());
		commentVo.setFcu(UserContext.get().getUserId(true));
		commentVo.setFcuName(UserContext.get().getUserName());
		String content = jsonObj.getString("content");
		if(StringUtils.isNotBlank(content)) {
			commentVo.setContent(content);
			ProcessTaskContentVo contentVo = new ProcessTaskContentVo(content);
			processTaskMapper.replaceProcessTaskContent(contentVo);
			jsonObj.put(ProcessTaskAuditDetailType.CONTENT.getParamName(), contentVo.getHash());
			processTaskStepCommentVo.setContentHash(contentVo.getHash());
		}
		
		String fileUuidListStr = jsonObj.getString("fileUuidList");
		if(StringUtils.isNotBlank(fileUuidListStr)) {
			List<String> fileUuidList = JSON.parseArray(fileUuidListStr, String.class);
			if(CollectionUtils.isNotEmpty(fileUuidList)) {
				for(String fileUuid : fileUuidList) {
					FileVo fileVo = fileMapper.getFileByUuid(fileUuid);
					if(fileVo == null) {
						throw new ProcessTaskRuntimeException("上传附件uuid:'" + fileUuid + "'不存在");
					}else {
						commentVo.getFileList().add(fileVo);
					}
				}
				ProcessTaskContentVo fileUuidListContentVo = new ProcessTaskContentVo(fileUuidListStr);
				processTaskMapper.replaceProcessTaskContent(fileUuidListContentVo);
				processTaskStepCommentVo.setFileUuidListHash(fileUuidListContentVo.getHash());
			}
		}
		processTaskMapper.insertProcessTaskStepComment(processTaskStepCommentVo);
		List<ProcessTaskStepCommentVo> processTaskStepCommentList = processTaskMapper.getProcessTaskStepCommentListByProcessTaskStepId(processTaskStepId);
		for(ProcessTaskStepCommentVo processTaskStepComment : processTaskStepCommentList) {
			processTaskService.parseProcessTaskStepComment(processTaskStepComment);
		}
		
		//生成活动	
		processTaskStepVo.setParamObj(jsonObj);
		handler.activityAudit(processTaskStepVo, ProcessTaskStepAction.COMMENT);
		JSONObject resultObj = new JSONObject();
		List<ProcessTaskStepCommentVo> commentList = new ArrayList<>();
		//步骤评论列表
		ProcessTaskStepAuditVo processTaskStepAuditVo = new ProcessTaskStepAuditVo();
		processTaskStepAuditVo.setProcessTaskId(processTaskId);
		processTaskStepAuditVo.setProcessTaskStepId(processTaskStepId);
		processTaskStepAuditVo.setAction(ProcessTaskStepAction.COMMENT.getValue());
		processTaskStepAuditList = processTaskMapper.getProcessTaskStepAuditList(processTaskStepAuditVo);
		if(CollectionUtils.isNotEmpty(processTaskStepAuditList)) {
			for(ProcessTaskStepAuditVo processTaskStepAudit : processTaskStepAuditList) {
				commentList.add(new ProcessTaskStepCommentVo(processTaskStepAudit));
			}
		}
		commentList.add(commentVo);
		resultObj.put("commentList", processTaskStepCommentList);
		return resultObj;
	}

}
