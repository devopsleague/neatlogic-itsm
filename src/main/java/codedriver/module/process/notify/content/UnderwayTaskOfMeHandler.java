package codedriver.module.process.notify.content;

import codedriver.framework.common.constvalue.Expression;
import codedriver.framework.common.constvalue.FormHandlerType;
import codedriver.framework.common.constvalue.GroupSearch;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.condition.core.ConditionHandlerFactory;
import codedriver.framework.condition.core.IConditionHandler;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.RoleVo;
import codedriver.framework.dto.TeamVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.notify.core.INotifyHandler;
import codedriver.framework.notify.core.NotifyContentHandlerBase;
import codedriver.framework.notify.core.NotifyHandlerFactory;
import codedriver.framework.notify.dao.mapper.NotifyJobMapper;
import codedriver.framework.notify.dto.NotifyVo;
import codedriver.framework.notify.dto.job.NotifyJobVo;
import codedriver.framework.notify.exception.NotifyHandlerNotFoundException;
import codedriver.framework.notify.handler.EmailNotifyHandler;
import codedriver.framework.process.column.core.IProcessTaskColumn;
import codedriver.framework.process.column.core.ProcessTaskColumnFactory;
import codedriver.framework.process.constvalue.ProcessConditionModel;
import codedriver.framework.process.constvalue.ProcessStepType;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.constvalue.ProcessWorkcenterField;
import codedriver.framework.process.dao.mapper.CatalogMapper;
import codedriver.framework.process.dao.mapper.ChannelMapper;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.dto.*;
import codedriver.module.process.workcenter.column.handler.ProcessTaskCurrentStepNameColumn;
import codedriver.module.process.workcenter.column.handler.ProcessTaskCurrentStepWorkerColumn;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 待我处理的工单
 */
@Component
public class UnderwayTaskOfMeHandler extends NotifyContentHandlerBase {

	private Map<String, IProcessTaskColumn> columnComponentMap = ProcessTaskColumnFactory.columnComponentMap;

	@Autowired
	private NotifyJobMapper notifyJobMapper;

	@Autowired
	private ProcessTaskMapper processTaskMapper;

	@Autowired
	private ChannelMapper channelMapper;

	@Autowired
	private CatalogMapper catalogMapper;

	@Autowired
	private UserMapper userMapper;

	@Autowired
	private TeamMapper teamMapper;

	@Autowired
	private RoleMapper roleMapper;

	public enum ConditionOptions{
		STEPTEAM("stepteam","处理组",Expression.INCLUDE.getExpression());

		private String value;
		private String text;
		private String expression;
		private ConditionOptions(String value, String text, String expression) {
			this.value = value;
			this.text = text;
			this.expression = expression;
		}
		public String getValue() {
			return value;
		}
		public String getText() {
			return text;
		}
		public String getExpression() {
			return expression;
		}

		public static ConditionOptions getConditionOption(String _value) {
			for(ConditionOptions e : values()) {
				if(e.value.equals(_value)) {
					return e;
				}
			}
			return null;
		}
	}

	private static Map<String, JSONArray> messageAttrMap = new HashMap<>();

	/**
	 * 不同的通知内容与通知方式插件对应不同的属性
	 * 例如此插件下的邮件通知有邮件标题、邮件正文、接收人
	 * 将来扩展通知方式插件时，可在messageAttrMap中put对应的插件类与属性
	 */
	static {
		messageAttrMap.put(EmailNotifyHandler.class.getName(),new JSONArray(){
			{
				this.add(new JSONObject(){
					{
						this.put("label","标题");
						this.put("name","title");
						this.put("type", "text");
						this.put("validateList", new JSONArray(){
							{
								this.add("required");
							}
						});
					}
				});
				this.add(new JSONObject(){
					{
						this.put("label","内容");
						this.put("name","content");
						this.put("type", FormHandlerType.TEXTAREA.toString());
					}
				});
				this.add(new JSONObject(){
					{
						this.put("label","接收人");
						this.put("name","toList");
						this.put("type", FormHandlerType.SELECT.toString());
						this.put("placeholder","工单内容对应的处理人");
						this.put("disabled",true);
					}
				});
			}
		});
	}

	@Override
	public String getName() {
		return "待我处理的工单";
	}

	@Override
	public String getType() {
		return Type.DYNAMIC.getValue();
	}

	/**
	 * 获取工单筛选条件
	 * @return
	 */
	@Override
	protected JSONArray getMyConditionOptionList() {
		JSONArray params = new JSONArray();
		for(ConditionOptions option : ConditionOptions.values()) {
			IConditionHandler condition = ConditionHandlerFactory.getHandler(option.getValue());
			if(condition != null) {
				JSONObject obj = condition.getConfig();
				obj.put("type",condition.getHandler(ProcessConditionModel.SIMPLE.getValue()));
				obj.put("name",condition.getName());
				obj.put("label",condition.getDisplayName());
				/** 不同的条件有其特殊的表单属性，根据需要自行添加 */
				if(ConditionOptions.STEPTEAM.getValue().equals(option.getValue())){
					obj.put("groupList",new JSONArray(){
						{
							this.add(GroupSearch.TEAM.getValue());
						}
					});
					obj.put("multiple", true);
					obj.put("search", true);
				}
				params.add(obj);
			}
		}
		return params;
	}

	/**
	 * 根据通知方式插件获取发件相关属性
	 * @param handler
	 * @return
	 */
	@Override
	protected JSONArray getMyMessageAttrList(String handler) {
		return messageAttrMap.get(handler);
	}

	/**
	 * 获取工单列表数据列
	 * @return
	 */
	@Override
	protected List<ValueTextVo> getMyDataColumnList() {
		List<ValueTextVo> result = new ArrayList<>();
		Map<String, IProcessTaskColumn> columnComponentMap = ProcessTaskColumnFactory.columnComponentMap;
		Collection<IProcessTaskColumn> values = columnComponentMap.values();
		values.stream().forEach(o -> {
			if(!o.getDisabled() && o.getIsShow()){
				result.add(new ValueTextVo(o.getName(),o.getDisplayName()));
			}
		});
		return result;
	}

	/**
	 * 获取待发送数据
	 * @param id
	 */
	@Override
	protected List<NotifyVo> getMyNotifyData(Long id) {
	    List<NotifyVo> notifyList = new ArrayList<>();

		NotifyJobVo job = notifyJobMapper.getJobBaseInfoById(id);
		INotifyHandler notifyHandler = NotifyHandlerFactory.getHandler(job.getNotifyHandler());
		if(notifyHandler == null){
			throw new NotifyHandlerNotFoundException(job.getNotifyHandler());
		}
		String config = job.getConfig();
		String title = null;
		String content = null;
		JSONObject messageConfig = (JSONObject) JSONPath.read(config, "messageConfig");
		if(MapUtils.isNotEmpty(messageConfig)){
			title = messageConfig.getString("title");
			content = messageConfig.getString("content");
		}

		/** 需要的工单字段 */
		JSONArray dataColumns = (JSONArray)JSONPath.read(config, "dataColumnList");
		List<String> columnList = new ArrayList<>();
		if(CollectionUtils.isNotEmpty(dataColumns)){
			dataColumns.stream().forEach(o -> columnList.add(columnComponentMap.get(o.toString()).getDisplayName()));
		}

		/** 获取工单查询条件 */
		List<String> stepTeamUuidList = new ArrayList<>();
		JSONArray conditionList = (JSONArray) JSONPath.read(config, "conditionList");
		if (CollectionUtils.isNotEmpty(conditionList)) {
			JSONObject stepTeam = conditionList.getJSONObject(0);
			JSONArray valueList = stepTeam.getJSONArray("valueList");
			if (CollectionUtils.isNotEmpty(valueList)) {
				for (Object o : valueList) {
					stepTeamUuidList.add(o.toString().split("#")[1]);
				}
			}
		}

		List<Map<String, Object>> originalTaskList = getTaskList(stepTeamUuidList);

		/** 按处理人给工单分类 */
		Map<String, List<Map<String, Object>>> userTaskMap = getUserTaskMap(originalTaskList);

		getNotifyVoList(notifyList, title, content, columnList, userTaskMap);

		return notifyList;
	}

	private void getNotifyVoList(List<NotifyVo> notifyList, String title, String content, List<String> columnList, Map<String, List<Map<String, Object>>> userTaskMap) {
		if (MapUtils.isNotEmpty(userTaskMap)) {
			for (Map.Entry<String, List<Map<String, Object>>> entry : userTaskMap.entrySet()) {
				NotifyVo.Builder notifyBuilder = new NotifyVo.Builder(null);
				notifyBuilder.withTitleTemplate(title);
				notifyBuilder.addUserUuid(entry.getKey());

				/** 绘制工单列表 */
				StringBuilder taskTable = new StringBuilder();
				if (StringUtils.isNotBlank(content)) {
					taskTable.append(content + "</br>");
				}
				taskTable.append("<table>");
				taskTable.append("<tr>");
				for (String column : columnList) {
					taskTable.append("<th>" + column + "</th>");
				}
				taskTable.append("</tr>");
				for (Map<String, Object> map : entry.getValue()) {
					taskTable.append("<tr>");
					for (String column : columnList) {
						if (map.containsKey(column)) {
							taskTable.append("<td>" + (map.get(column) == null ? "" : map.get(column)) + "</td>");
						}
					}
					taskTable.append("</tr>");
				}
				taskTable.append("</table>");
				notifyBuilder.withContentTemplate(taskTable.toString());
				NotifyVo notifyVo = notifyBuilder.build();
				notifyList.add(notifyVo);
			}
		}
	}

	/**
	 * 按用户将工单分类
	 * key->userUuid
	 * value->task
	 */
	private Map<String, List<Map<String, Object>>> getUserTaskMap(List<Map<String, Object>> originalTaskList) {
		Map<String, List<Map<String, Object>>> userTaskMap = new HashMap<>();
		if (CollectionUtils.isNotEmpty(originalTaskList)) {
			for (Map<String, Object> map : originalTaskList) {
				Object o = map.get(new ProcessTaskCurrentStepNameColumn().getName());
				if (o != null) {
					Set<String> array = (HashSet<String>) o;
					for (String uuid : array) {
						List<Map<String, Object>> mapList = new ArrayList<>();
						if (userTaskMap.get(uuid) != null) {
							mapList = userTaskMap.get(uuid);
						}
						if (!mapList.stream().anyMatch(task -> task.get(ProcessWorkcenterField.ID.getName()).toString().equals(map.get(ProcessWorkcenterField.ID.getName()).toString()))) {
							mapList.add(map);
						}
						userTaskMap.put(uuid, mapList);
					}
				}
			}
		}
		return userTaskMap;
	}

	/**
	 * 构造原始工单数据
	 * originalTaskList中的map：
	 * 字段中文名->值
	 */
	private List<Map<String, Object>> getTaskList(List<String> stepTeamUuidList) {
		List<Map<String, Object>> originalTaskList = new ArrayList<>();
		List<ProcessTaskVo> processTaskList = processTaskMapper.getProcessTaskListByStepTeamUuidList(stepTeamUuidList);
		if (CollectionUtils.isNotEmpty(processTaskList)) {
			for (ProcessTaskVo processTaskVo : processTaskList) {
				Map<String, Object> map = new HashMap<>();

				/** 获取服务信息 **/
				ChannelVo channel = channelMapper.getChannelByUuid(processTaskVo.getChannelUuid());
				if (channel == null) {
					channel = new ChannelVo();
				}
				/** 获取服务目录信息 **/
				CatalogVo catalog = null;
				if (StringUtils.isNotBlank(channel.getParentUuid())) {
					catalog = catalogMapper.getCatalogByUuid(channel.getParentUuid());
				}
				if (catalog == null) {
					catalog = new CatalogVo();
				}

				/** 获取工单当前步骤 **/
				List<ProcessTaskStepVo> processTaskStepList = processTaskMapper
						.getProcessTaskActiveStepByProcessTaskIdAndProcessStepType(processTaskVo.getId(), new ArrayList<String>() {
							{
								add(ProcessStepType.PROCESS.getValue());
								add(ProcessStepType.START.getValue());
							}
						}, 1);
				/** 记录当前步骤名与处理人 */
				String currentStepName = null;
				String currentStepWorkerName = null;
				Set<String> currentStepWorkerUuidList = new HashSet<>();
				if (CollectionUtils.isNotEmpty(processTaskStepList)) {
					List<String> currentStepNameList = new ArrayList<>();
					Set<String> currentStepWorkerNameList = new HashSet<>();
					for (ProcessTaskStepVo step : processTaskStepList) {
						currentStepNameList.add(step.getName());
						if (ProcessTaskStatus.RUNNING.getValue().equals(processTaskVo.getStatus())
								&& (ProcessTaskStatus.DRAFT.getValue().equals(processTaskVo.getStatus())
								|| (ProcessTaskStatus.PENDING.getValue().equals(processTaskVo.getStatus())
								&& step.getIsActive() == 1) || ProcessTaskStatus.RUNNING.getValue().equals(processTaskVo.getStatus()))) {
							if (step.getStatus().equals(ProcessTaskStatus.PENDING.getValue()) && step.getIsActive() == 1) {
								for (ProcessTaskStepWorkerVo worker : step.getWorkerList()) {
									if (GroupSearch.USER.getValue().equals(worker.getType())) {
										UserVo user = userMapper.getUserBaseInfoByUuid(worker.getUuid());
										if (user != null) {
											currentStepWorkerNameList.add(user.getUserName());
											currentStepWorkerUuidList.add(user.getUuid());
										}
									} else if (GroupSearch.TEAM.getValue().equals(worker.getType())) {
										TeamVo team = teamMapper.getTeamByUuid(worker.getUuid());
										if (team != null) {
											currentStepWorkerNameList.add(team.getName());
											List<String> userUuidList = userMapper.getUserUuidListByTeamUuid(team.getUuid());
											if (CollectionUtils.isNotEmpty(userUuidList)) {
												currentStepWorkerUuidList.addAll(userUuidList);
											}
										}
									} else if (GroupSearch.ROLE.getValue().equals(worker.getType())) {
										RoleVo role = roleMapper.getRoleByUuid(worker.getUuid());
										if (role != null) {
											currentStepWorkerNameList.add(role.getName());
											List<String> userUuidList = userMapper.getUserUuidListByRoleUuid(role.getUuid());
											if (CollectionUtils.isNotEmpty(userUuidList)) {
												currentStepWorkerUuidList.addAll(userUuidList);
											}
										}
									}
								}
							} else {
								for (ProcessTaskStepUserVo userVo : step.getUserList()) {
									UserVo user = userMapper.getUserBaseInfoByUuid(userVo.getUserVo().getUuid());
									if (user != null) {
										currentStepWorkerNameList.add(user.getUserName());
										currentStepWorkerUuidList.add(user.getUuid());
									}
								}
							}
						}
					}
					currentStepName = String.join(",", currentStepNameList);
					currentStepWorkerName = String.join(",", currentStepWorkerNameList);
				}

				/** 记录超时时间 **/
				List<ProcessTaskSlaVo> processTaskSlaList = processTaskMapper.getProcessTaskSlaByProcessTaskId(processTaskVo.getId());
				StringBuilder sb = new StringBuilder();
				if (CollectionUtils.isNotEmpty(processTaskSlaList) && ProcessTaskStatus.RUNNING.getValue().equals(processTaskVo.getStatus())) {
					for (ProcessTaskSlaVo slaVo : processTaskSlaList) {
						ProcessTaskSlaTimeVo slaTimeVo = slaVo.getSlaTimeVo();
						Long expireTimeLong = slaTimeVo.getExpireTimeLong();
						if (!(slaTimeVo.getExpireTime() == null && expireTimeLong == null)) {
							Long expireTime = slaTimeVo.getExpireTime() == null ? expireTimeLong : slaTimeVo.getExpireTime().getTime();
							Long willOverTime = null;
							JSONObject configObj = slaVo.getConfigObj();
							if (configObj != null && configObj.containsKey("willOverTimeRule")) {
								Integer willOverTimeRule = configObj.getInteger("willOverTimeRule");
								if (willOverTimeRule != null && expireTime != null) {
									willOverTime = expireTime - willOverTimeRule * 60 * 100;
								}
							}
							long time;
							if (willOverTime != null && System.currentTimeMillis() > willOverTime) {
								time = System.currentTimeMillis() - willOverTime;
								sb.append(slaVo.getName())
										.append("距离超时：")
										.append(Math.floor(time / (1000 * 60 * 60 * 24)))
										.append("天;");
							} else if (expireTime != null && System.currentTimeMillis() > expireTime) {
								time = System.currentTimeMillis() - expireTime;
								sb.append(slaVo.getName())
										.append("已超时：")
										.append(Math.floor(time / (1000 * 60 * 60 * 24)))
										.append("天;");
							}
						}
					}
				}

				map.put(ProcessWorkcenterField.ID.getName(), processTaskVo.getId());
				map.put(ProcessWorkcenterField.SERIAL_NUMBER.getName(), processTaskVo.getSerialNumber());
				map.put(ProcessWorkcenterField.TITLE.getName(), processTaskVo.getTitle());
				map.put(ProcessWorkcenterField.CHANNELTYPE.getName(), processTaskVo.getChannelTypeName());
				map.put(ProcessWorkcenterField.CHANNEL.getName(), processTaskVo.getChannelName());
				map.put(ProcessWorkcenterField.CATALOG.getName(), catalog.getName());
				map.put(ProcessWorkcenterField.ENDTIME.getName(), processTaskVo.getEndTime());
				map.put(ProcessWorkcenterField.STARTTIME.getName(), processTaskVo.getStartTime());
				map.put(ProcessWorkcenterField.OWNER.getName(), processTaskVo.getOwnerName());
				map.put(ProcessWorkcenterField.REPORTER.getName(), processTaskVo.getReporterName());
				map.put(ProcessWorkcenterField.PRIORITY.getName(), processTaskVo.getPriorityName());
				map.put(ProcessWorkcenterField.STATUS.getName(), ProcessTaskStatus.getText(processTaskVo.getStatus()));
				map.put(ProcessWorkcenterField.WOKRTIME.getName(), processTaskVo.getWorktimeName());
				map.put(new ProcessTaskCurrentStepNameColumn().getDisplayName(), currentStepName);
				map.put(new ProcessTaskCurrentStepWorkerColumn().getDisplayName(), currentStepWorkerName);
				map.put(ProcessWorkcenterField.EXPIRED_TIME.getName(), sb.toString());
				/** 保留当前步骤处理人人的userUuid，以便后面据此给工单分类 */
				map.put(new ProcessTaskCurrentStepNameColumn().getName(), currentStepWorkerUuidList);
				originalTaskList.add(map);
			}
		}
		return originalTaskList;
	}

}
