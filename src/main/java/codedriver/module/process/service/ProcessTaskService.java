package codedriver.module.process.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.process.constvalue.ProcessTaskOperationType;
import codedriver.framework.process.dto.ProcessTaskSlaTimeVo;
import codedriver.framework.process.dto.ProcessTaskStepReplyVo;
import codedriver.framework.process.dto.ProcessTaskStepSubtaskContentVo;
import codedriver.framework.process.dto.ProcessTaskStepSubtaskVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.dto.automatic.AutomaticConfigVo;

public interface ProcessTaskService {

	/**
	 * 
	* @Description: 创建子任务 
	* @param processTaskStepSubtaskVo 
	* @return void
	 */
	public void createSubtask(ProcessTaskStepSubtaskVo processTaskStepSubtaskVo);
	/**
	 * 
	* @Description: 编辑子任务 
	* @param processTaskStepSubtaskVo 
	* @return void
	 */
	public void editSubtask(ProcessTaskStepSubtaskVo processTaskStepSubtaskVo);
	/**
	 * 
	* @Description: 打回重做子任务 
	* @param processTaskStepSubtaskVo 
	* @return void
	 */
	public void redoSubtask(ProcessTaskStepSubtaskVo processTaskStepSubtaskVo);
	/**
	 * 
	* @Description: 完成子任务 
	* @param processTaskStepSubtaskVo 
	* @return void
	 */
	public void completeSubtask(ProcessTaskStepSubtaskVo processTaskStepSubtaskVo);
	/**
	 * 
	* @Description: 取消子任务 
	* @param processTaskStepSubtaskVo 
	* @return void
	 */
	public void abortSubtask(ProcessTaskStepSubtaskVo processTaskStepSubtaskVo);
	/**
	 * 
	* @Description: 回复子任务 
	* @param processTaskStepSubtaskVo 
	* @return void
	 */
	public List<ProcessTaskStepSubtaskContentVo> commentSubtask(ProcessTaskStepSubtaskVo processTaskStepSubtaskVo);
	/**
	 * 
	* @Description: 工单上报/查看/处理页面，返回表单formConfig时，设置属性只读/隐藏控制数据
	* @param processTaskVo 工单信息
	* @param formAttributeActionMap 处理页面时，表单属性只读/隐藏控制数据
	* @param mode 0：查看页面，1：处理页面
	* @return void
	 */
	public void setProcessTaskFormAttributeAction(ProcessTaskVo processTaskVo, Map<String, String> formAttributeActionMap, int mode);

	public void parseProcessTaskStepReply(ProcessTaskStepReplyVo processTaskStepReplyVo);

	/**
	 * 执行请求
	 * @param automaticConfigVo
	 * @param currentProcessTaskStepVo
	 */
	public Boolean runRequest(AutomaticConfigVo automaticConfigVo, ProcessTaskStepVo currentProcessTaskStepVo);

	public ProcessTaskStepVo getProcessTaskStepDetailInfoById(Long processTaskStepId);

	public JSONObject initProcessTaskStepData(ProcessTaskStepVo currentProcessTaskStepVo, AutomaticConfigVo automaticConfig,
			JSONObject data, String type);

	/**
	 * 初始化job
	 * @param automaticConfigVo
	 * @param currentProcessTaskStepVo
	 */
	public void initJob(AutomaticConfigVo automaticConfigVo, ProcessTaskStepVo currentProcessTaskStepVo, JSONObject data);
	/**
	 * 
	* @Time:2020年7月28日
	* @Description: 获取自定义按钮映射数据
	* @param processTaskStepId
	* @return Map<String,String>
	 */
//	public Map<String, String> getCustomButtonTextMap(Long processTaskStepId);
	
	/**
	 * 
	* @Author: linbq
	* @Time:2020年8月21日
	* @Description: 检查工单参数是否合法 
	* @param processTaskId 工单id
	* @param processTaskStepId 步骤id
	* @param nextStepId 下一步骤id
	* @return boolean
	 */
	public boolean checkProcessTaskParamsIsLegal(Long processTaskId, Long processTaskStepId, Long nextStepId);
	/**
     * 
    * @Author: linbq
    * @Time:2020年8月21日
    * @Description: 检查工单参数是否合法 
    * @param processTaskId 工单id
    * @param processTaskStepId 步骤id
    * @return boolean
     */
	public boolean checkProcessTaskParamsIsLegal(Long processTaskId, Long processTaskStepId);
	/**
     * 
    * @Author: linbq
    * @Time:2020年8月21日
    * @Description: 检查工单参数是否合法 
    * @param processTaskId 工单id
    * @return boolean
     */
	public boolean checkProcessTaskParamsIsLegal(Long processTaskId);
	/**
	 * 
	* @Author: linbq
	* @Time:2020年8月21日
	* @Description: 获取工单信息 
	* @param processTaskId 工单id
	* @return ProcessTaskVo
	 */
	public ProcessTaskVo getProcessTaskDetailById(Long processTaskId);
	
	/**
     * 
    * @Author: linbq
    * @Time:2020年8月21日
    * @Description: 获取步骤回复列表
    * @param processTaskStepId 步骤id
    * @return List<ProcessTaskStepCommentVo>
     */
	public List<ProcessTaskStepReplyVo> getProcessTaskStepReplyListByProcessTaskStepId(Long processTaskStepId);
	/**
     * 
    * @Author: linbq
    * @Time:2020年8月21日
    * @Description: 获取步骤子任务列表
    * @param processTaskStepId 步骤id
    * @return List<ProcessTaskStepSubtaskVo>
     */
	public List<ProcessTaskStepSubtaskVo> getProcessTaskStepSubtaskListByProcessTaskStepId(Long processTaskStepId);
	/**
     * 
    * @Author: linbq
    * @Time:2020年8月21日
    * @Description: 获取可分配处理人的步骤列表
    * @param processTaskStepId 步骤id
    * @return List<ProcessTaskStepVo>
     */
	public List<ProcessTaskStepVo> getAssignableWorkerStepListByProcessTaskIdAndProcessStepUuid(Long processTaskId, String processStepUuid);
	/**
     * 
    * @Author: linbq
    * @Time:2020年8月21日
    * @Description: 获取步骤时效列表
    * @param processTaskStepId 步骤id
    * @return List<ProcessTaskSlaTimeVo>
     */
	public List<ProcessTaskSlaTimeVo> getSlaTimeListByProcessTaskStepIdAndWorktimeUuid(Long processTaskStepId, String worktimeUuid);
	/**
     * 
    * @Author: linbq
    * @Time:2020年8月21日
    * @Description: 设置下一步骤列表
    * @param ProcessTaskStepVo 步骤信息
    * @return void
     */
	public void setNextStepList(ProcessTaskStepVo processTaskStepVo);

	/**
	 * 
	* @Author: linbq
	* @Time:2020年8月24日
	* @Description: 设置步骤处理人、协助处理人、待办人等 
	* @param processTaskStepVo 
	* @return void
	 */
	public void setProcessTaskStepUser(ProcessTaskStepVo processTaskStepVo);
	/**
	 * 
	* @Author: linbq
	* @Time:2020年8月24日
	* @Description: 设置步骤配置、处理器全局配置信息 
	* @param processTaskStepVo 
	* @return void
	 */
	public void setProcessTaskStepConfig(ProcessTaskStepVo processTaskStepVo);
	/**
	 * 
	* @Author: linbq
	* @Time:2020年8月24日
	* @Description: 获取步骤描述内容及附件列表 
	* @param processTaskStepId 步骤id
	* @return ProcessTaskStepCommentVo
	 */
	public ProcessTaskStepReplyVo getProcessTaskStepContentAndFileByProcessTaskStepIdId(Long processTaskStepId);
	/**
	 * 
	* @Author: linbq
	* @Time:2020年8月26日
	* @Description: TODO 
	* @param jsonObj 
	* @param processTaskStepReplyVo 旧的回复数据
	* @return boolean 如果保存成功返回true，否则返回false
	 */
	public boolean saveProcessTaskStepReply(JSONObject jsonObj, ProcessTaskStepReplyVo processTaskStepReplyVo);

	/**
     * 
     * @Time:2020年4月2日
     * @Description: 检查当前用户是否配置该权限
     * @param processTaskVo
     * @param processTaskStepVo
     * @param operationType 
     * @return boolean
     */
	public boolean checkOperationAuthIsConfigured(ProcessTaskVo processTaskVo, ProcessTaskStepVo processTaskStepVo, ProcessTaskOperationType operationType);
	/**
     * 
     * @Time:2020年4月3日
     * @Description: 获取工单中当前用户能撤回的步骤列表
     * @param processTaskId
     * @return Set<ProcessTaskStepVo>
     */
	public Set<ProcessTaskStepVo> getRetractableStepListByProcessTaskId(Long processTaskId);
	/**
     * 
     * @Time:2020年4月3日
     * @Description: 获取工单中当前用户能处理的步骤列表
     * @param processTaskId
     * @return List<ProcessTaskStepVo>
     */
	public List<ProcessTaskStepVo> getProcessableStepList(Long processTaskId);
	/**
     * 
     * @Time:2020年4月18日
     * @Description: 获取工单中当前用户能催办的步骤列表
     * @param processTaskId
     * @return List<ProcessTaskStepVo>
     */
    public List<ProcessTaskStepVo> getUrgeableStepList(Long processTaskId);
}
