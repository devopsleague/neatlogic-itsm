package codedriver.module.process.workcenter.elasticsearch.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.multiattrsearch.MultiAttrsObjectPatch;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.elasticsearch.core.ElasticSearchPoolManager;
import codedriver.framework.process.constvalue.ProcessFormHandler;
import codedriver.framework.process.constvalue.ProcessStepType;
import codedriver.framework.process.constvalue.ProcessTaskOperationType;
import codedriver.framework.process.dao.mapper.CatalogMapper;
import codedriver.framework.process.dao.mapper.ChannelMapper;
import codedriver.framework.process.dao.mapper.FormMapper;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dao.mapper.SelectContentByHashMapper;
import codedriver.framework.process.dao.mapper.WorktimeMapper;
import codedriver.framework.process.dao.mapper.workcenter.WorkcenterMapper;
import codedriver.framework.process.dto.CatalogVo;
import codedriver.framework.process.dto.ChannelVo;
import codedriver.framework.process.dto.ProcessTaskContentVo;
import codedriver.framework.process.dto.ProcessTaskFormAttributeDataVo;
import codedriver.framework.process.dto.ProcessTaskSlaVo;
import codedriver.framework.process.dto.ProcessTaskStepAuditVo;
import codedriver.framework.process.dto.ProcessTaskStepContentVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.elasticsearch.core.ProcessTaskEsHandlerBase;
import codedriver.framework.process.workcenter.dto.WorkcenterFieldBuilder;

@Service
public class WorkcenterUpdateHandler extends ProcessTaskEsHandlerBase {
	Logger logger = LoggerFactory.getLogger(WorkcenterUpdateHandler.class);
	@Autowired
	WorkcenterMapper workcenterMapper;
	@Autowired
	FormMapper formMapper;
	@Autowired
	ProcessTaskMapper processTaskMapper;
	@Autowired
	ChannelMapper channelMapper;
	@Autowired
	CatalogMapper catalogMapper;
	@Autowired
	WorktimeMapper worktimeMapper;

    @Autowired
    private SelectContentByHashMapper selectContentByHashMapper;
	
	@Override
	public String getHandler() {
		return "processtask-update";
	}

	@Override
	public String getHandlerName() {
		return "更新es工单信息";
	}
	
	@Override
	public JSONObject getConfig(List<Object> params) {
		JSONObject paramJson = new JSONObject();
		ListIterator<Object> paramIterator =  params.listIterator();
		Long taskId = null;
		Long taskStepId = null;
		TO: while(paramIterator.hasNext()) {
			Object param = paramIterator.next();
			if(param instanceof ProcessTaskVo) {
				taskId = ((ProcessTaskVo)param).getId();
				break TO;
			}else{
				Method[] ms= param.getClass().getMethods();
				for(Method m : ms) {
					if(m.getName().equals("getProcessTaskId")) {
						try {
							taskId = (Long)param.getClass().getMethod("getProcessTaskId").invoke(param);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
								| NoSuchMethodException | SecurityException e) {
							logger.error(e.getMessage(),e);
						}
						if(taskId != null) {
							break TO;
						}
					}
					if(m.getName().equals("getProcessTaskStepId")) {
						try {
							taskStepId = (Long)param.getClass().getMethod("getProcessTaskStepId").invoke(param);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
								| NoSuchMethodException | SecurityException e) {
							logger.error(e.getMessage(),e);
						}
					}
				}
			}
		}
		if(taskId == null) {
			ProcessTaskStepVo  processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(taskStepId);
			if(processTaskStepVo != null) {
				taskId = processTaskStepVo.getProcessTaskId();
			}
		}
		paramJson.put("taskId", taskId);
		paramJson.put("tenantUuid", TenantContext.get().getTenantUuid());
		return paramJson;
	}
	
	@Override
	public void doService(JSONObject paramJson) {
		 Long taskId = paramJson.getLong("taskId");
		 String tenantUuid = paramJson.getString("tenantUuid");
		 getObjectPool().checkout(tenantUuid);
		 MultiAttrsObjectPatch patch = getObjectPool().save(taskId.toString());
		 /** 获取工单信息 **/
		 ProcessTaskVo processTaskVo = processTaskMapper.getProcessTaskBaseInfoById(taskId);
		 if(processTaskVo != null) {
			 /** 获取服务信息 **/
			 ChannelVo channel = channelMapper.getChannelByUuid(processTaskVo.getChannelUuid());
			 /** 获取服务目录信息 **/
			 CatalogVo catalog = catalogMapper.getCatalogByUuid(channel.getParentUuid());
			 /** 获取开始节点内容信息 **/
			 ProcessTaskContentVo startContentVo = null;
			 List<ProcessTaskStepVo> stepList = processTaskMapper.getProcessTaskStepByProcessTaskIdAndType(taskId, ProcessStepType.START.getValue());
			 if (stepList.size() == 1) {
				ProcessTaskStepVo startStepVo = stepList.get(0);
				List<ProcessTaskStepContentVo> processTaskStepContentList = processTaskMapper.getProcessTaskStepContentByProcessTaskStepId(startStepVo.getId());
				for(ProcessTaskStepContentVo processTaskStepContent : processTaskStepContentList) {
	                if (ProcessTaskOperationType.STARTPROCESS.getValue().equals(processTaskStepContent.getType())) {
	                    startContentVo = selectContentByHashMapper.getProcessTaskContentByHash(processTaskStepContent.getContentHash());
	                    break;
	                }
				}
			 }
			 /** 获取转交记录 **/
			 List<ProcessTaskStepAuditVo> transferAuditList = processTaskMapper.getProcessTaskAuditList(new ProcessTaskStepAuditVo(processTaskVo.getId(),ProcessTaskOperationType.TRANSFER.getValue()));
			
			 /** 获取工单当前步骤 **/
			 @SuppressWarnings("serial")
			 List<ProcessTaskStepVo>  processTaskStepList = processTaskMapper.getProcessTaskActiveStepByProcessTaskIdAndProcessStepType(taskId,new ArrayList<String>() {{add(ProcessStepType.PROCESS.getValue());add(ProcessStepType.START.getValue());}},null);
			 WorkcenterFieldBuilder builder = new WorkcenterFieldBuilder();
			 
			 /** 时效列表 **/
			 List<ProcessTaskSlaVo> processTaskSlaList = processTaskMapper.getProcessTaskSlaByProcessTaskId(processTaskVo.getId());
				
			 //form
			 JSONArray formArray = new JSONArray();
			 List<ProcessTaskFormAttributeDataVo> formAttributeDataList = processTaskMapper.getProcessTaskStepFormAttributeDataByProcessTaskId(taskId);
			 for (ProcessTaskFormAttributeDataVo attributeData : formAttributeDataList) {
				 if(attributeData.getType().equals(ProcessFormHandler.FORMCASCADELIST.getHandler())
							||attributeData.getType().equals(ProcessFormHandler.FORMDIVIDER.getHandler())
							||attributeData.getType().equals(ProcessFormHandler.FORMDYNAMICLIST.getHandler())
							||attributeData.getType().equals(ProcessFormHandler.FORMSTATICLIST.getHandler())){
					 continue;
				 }
				 JSONObject formJson = new JSONObject();
				 formJson.put("key", attributeData.getAttributeUuid());
				 Object dataObj = attributeData.getDataObj();
				 if(dataObj == null) {
					 continue;
				 }
				 formJson.put("value_"+ProcessFormHandler.getDataType(attributeData.getType()),dataObj);
				 formArray.add(formJson);
			 }
			
			 //common
			 JSONObject WorkcenterFieldJson = builder
					.setId(taskId.toString())
					.setTitle(processTaskVo.getTitle())
			 		.setStatus(processTaskVo.getStatus())
			 		.setPriority(processTaskVo.getPriorityUuid())
			 		.setCatalog(catalog.getUuid())
			 		.setChannelType(channel.getChannelTypeUuid())
			 		.setChannel(channel.getUuid())
			 		.setProcessUuid(processTaskVo.getProcessUuid())
			 		.setConfigHash(processTaskVo.getConfigHash())
			 		.setContent(startContentVo)
			 		.setStartTime(processTaskVo.getStartTime())
			 		.setEndTime(processTaskVo.getEndTime())
			 		.setOwner(processTaskVo.getOwner())
			 		.setReporter(processTaskVo.getReporter(),processTaskVo.getOwner())
			 		.setStepList(processTaskStepList)
			 		.setTransferFromUserList(transferAuditList)
			 		.setWorktime(channel.getWorktimeUuid())
			 		.setExpiredTime(processTaskSlaList)
			 		.build();
			
			 patch.set("form", formArray);
			 patch.set("common", WorkcenterFieldJson);
			 patch.commit();
		 }else {
			 ElasticSearchPoolManager.getObjectPool(ProcessTaskEsHandlerBase.POOL_NAME).delete(taskId.toString());
		 }
	}

}
