package codedriver.module.process.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.multiattrsearch.MultiAttrsObject;
import com.techsure.multiattrsearch.query.QueryResult;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.elasticsearch.core.ElasticSearchPoolManager;
import codedriver.framework.process.dao.mapper.FormMapper;
import codedriver.framework.process.exception.workcenter.WorkcenterConditionException;
import codedriver.framework.process.workcenter.EsHandler;
import codedriver.framework.process.workcenter.column.core.IWorkcenterColumn;
import codedriver.framework.process.workcenter.column.core.WorkcenterColumnFactory;
import codedriver.framework.process.workcenter.condition.core.IWorkcenterCondition;
import codedriver.framework.process.workcenter.condition.core.WorkcenterConditionFactory;
import codedriver.framework.process.workcenter.condition.handler.ProcessTaskContentCondition;
import codedriver.framework.process.workcenter.condition.handler.ProcessTaskIdCondition;
import codedriver.framework.process.workcenter.condition.handler.ProcessTaskTitleCondition;
import codedriver.framework.process.workcenter.dao.mapper.WorkcenterMapper;
import codedriver.framework.process.workcenter.elasticsearch.core.WorkcenterEsHandlerBase;
import codedriver.framework.util.TimeUtil;
import codedriver.module.process.constvalue.ProcessExpression;
import codedriver.module.process.constvalue.ProcessFormHandlerType;
import codedriver.module.process.constvalue.ProcessWorkcenterConditionModel;
import codedriver.module.process.constvalue.ProcessWorkcenterConditionType;
import codedriver.module.process.dto.FormAttributeVo;
import codedriver.module.process.workcenter.dto.WorkcenterConditionGroupRelVo;
import codedriver.module.process.workcenter.dto.WorkcenterConditionGroupVo;
import codedriver.module.process.workcenter.dto.WorkcenterConditionRelVo;
import codedriver.module.process.workcenter.dto.WorkcenterConditionVo;
import codedriver.module.process.workcenter.dto.WorkcenterTheadVo;
import codedriver.module.process.workcenter.dto.WorkcenterVo;
@Service
public class WorkcenterService {
	@Autowired
	WorkcenterMapper workcenterMapper;
	
	@Autowired
	FormMapper formMapper;
	
	
	/**
	 *   搜索工单
	 * @param workcenterVo
	 * @return 
	 */
	private  QueryResult searchTask(WorkcenterVo workcenterVo){
		String selectColumn = "*";
		String where = assembleWhere(workcenterVo);
		String orderBy = "order by createTime desc";
		String sql = String.format("select %s from techsure %s %s limit %d", selectColumn,where,orderBy,workcenterVo.getPageSize());
		return EsHandler.searchSql(ElasticSearchPoolManager.getObjectPool(WorkcenterEsHandlerBase.POOL_NAME), sql);
		
	}
	/**
	 * 工单中心根据条件获取工单列表数据
	 * @param workcenterVo
	 * @return
	 */
	public JSONObject doSearch(WorkcenterVo workcenterVo) {
		JSONObject returnObj = new JSONObject();
		//搜索es
		QueryResult result = searchTask(workcenterVo);;
		List<MultiAttrsObject> resultData = result.getData();
		//返回的数据重新加工
		List<JSONObject> dataList = new ArrayList<JSONObject>();
		Map<String, IWorkcenterColumn> columnComponentMap = WorkcenterColumnFactory.columnComponentMap;
		//获取用户历史自定义theadList
		List<WorkcenterTheadVo> theadList = workcenterMapper.getWorkcenterThead(new WorkcenterTheadVo(workcenterVo.getUuid(),UserContext.get().getUserId()));
		//矫正theadList 或存在表单属性或固定字段增删
		//多删
		ListIterator<WorkcenterTheadVo> it = theadList.listIterator();
		while(it.hasNext()) {
			WorkcenterTheadVo thead = it.next();
			if(thead.getType().equals(ProcessWorkcenterConditionType.COMMON.getValue())) {
				if(!columnComponentMap.containsKey(thead.getName())) {
					it.remove();
				}else {
					thead.setDisplayName(columnComponentMap.get(thead.getName()).getDisplayName());
				}
			}else {
				List<String> channelUuidList = workcenterVo.getChannelUuidList();
				if(CollectionUtils.isNotEmpty(channelUuidList)) {
					List<FormAttributeVo> formAttrList = formMapper.getFormAttributeListByChannelUuidList(channelUuidList);
					List<FormAttributeVo> theadFormList = formAttrList.stream().filter(attr->attr.getUuid().equals(thead.getName())).collect(Collectors.toList());
					if(CollectionUtils.isEmpty(theadFormList)){
						it.remove();
					}else {
						thead.setDisplayName(theadFormList.get(0).getLabel());
					}
				}
			}
		}
		//少补
		for (Map.Entry<String, IWorkcenterColumn> entry : columnComponentMap.entrySet()) {
    		IWorkcenterColumn column = entry.getValue();
    		if(CollectionUtils.isEmpty(theadList.stream().filter(data->column.getName().endsWith(data.getName())).collect(Collectors.toList()))) {
    			theadList.add(new WorkcenterTheadVo(column));
    		}
    	}

		if (!resultData.isEmpty()) {
            for (MultiAttrsObject el : resultData) {
            	JSONObject taskJson = new JSONObject();
            	taskJson.put("taskId", el.getId());
            	for (Map.Entry<String, IWorkcenterColumn> entry : columnComponentMap.entrySet()) {
            		IWorkcenterColumn column = entry.getValue();
            		taskJson.put(column.getName(), column.getValue(el));
            	}
            	dataList.add(taskJson);
            }
        }

		returnObj.put("theadList", theadList);
		returnObj.put("tbodyList", dataList);
		returnObj.put("rowNum", result.getTotal());
		returnObj.put("pageSize", workcenterVo.getPageSize());
		returnObj.put("currentPage", workcenterVo.getCurrentPage());
		returnObj.put("pageCount", PageUtil.getPageCount(result.getTotal(), workcenterVo.getPageSize()));
		return returnObj;
	}
	
	/**
	 * 工单中心根据条件获取工单列表数据
	 * @param workcenterVo
	 * @return
	 */
	public Integer doSearchCount(WorkcenterVo workcenterVo) {
		//搜索es
		QueryResult result = searchTask(workcenterVo);;
		return result.getTotal();
	}
	
	/**
	 * 根据关键字获取所有过滤选项
	 * @param keyword
	 * @return
	 */
	public JSONArray getKeywordOptions(String keyword,Integer pageSize){
		//搜索标题
		JSONArray returnArray = getKeywordOption(new ProcessTaskTitleCondition(),keyword,pageSize);
		//搜索ID
		returnArray.addAll(getKeywordOption(new ProcessTaskIdCondition(),keyword,pageSize));
		//搜索内容
		returnArray.addAll(getKeywordOption(new ProcessTaskContentCondition(),keyword,pageSize));
		return returnArray;
	}
	
	/**
	 * 根据单个关键字获取过滤选项
	 * @param keyword
	 * @return
	 */
	private JSONArray getKeywordOption(IWorkcenterCondition condition, String keyword,Integer pageSize) {
		JSONArray returnArray = new JSONArray();
		WorkcenterVo workcenter = getKeywordCondition(condition,keyword);
		workcenter.setPageSize(pageSize);
		List<MultiAttrsObject> titleData = searchTask(workcenter).getData();
		if (!titleData.isEmpty()) {
			JSONObject titleObj = new JSONObject();
			JSONArray titleDataList = new JSONArray();
            for (MultiAttrsObject titleEl : titleData) {
            	titleDataList.add(WorkcenterColumnFactory.getHandler(condition.getName()).getValue(titleEl));
            }
            titleObj.put("dataList", titleDataList);
            titleObj.put("value", condition.getName());
            titleObj.put("text",condition.getDisplayName());
            returnArray.add(titleObj);
		}
		return returnArray;
	}
	
	/**
	 * 拼接关键字过滤选项
	 * @param type 搜索内容类型
	 * @return 
	 */
	private WorkcenterVo getKeywordCondition(IWorkcenterCondition condition,String keyword) {
		JSONObject  searchObj = new JSONObject();
		JSONArray conditionGroupList = new JSONArray();
		JSONObject conditionGroup = new JSONObject();
		JSONArray conditionList = new JSONArray();
		JSONObject conditionObj = new JSONObject();
		conditionObj.put("name", String.format("%s#%s",condition.getType(),condition.getName()));
		JSONArray valueList = new JSONArray();
		valueList.add(keyword);
		conditionObj.put("valueList", valueList);
		conditionObj.put("expression", ProcessExpression.LIKE.getExpression());
		conditionList.add(conditionObj);
		conditionGroup.put("conditionList", conditionList);
		conditionGroupList.add(conditionGroup);
		searchObj.put("conditionGroupList", conditionGroupList);
		
		return new WorkcenterVo(searchObj);
		
	}
	
	/**
	 * 拼接where条件
	 * @param workcenterVo
	 * @return
	 */
	private static String assembleWhere(WorkcenterVo workcenterVo) {
		Map<String,String> groupRelMap = new HashMap<String,String>();
		StringBuilder whereSb = new StringBuilder();
		whereSb.append(" where ");
		List<WorkcenterConditionGroupRelVo> groupRelList = workcenterVo.getWorkcenterConditionGroupRelList();
		if(CollectionUtils.isNotEmpty(groupRelList)) {
			//将group 以连接表达式 存 Map<fromUuid_toUuid,joinType> 
			for(WorkcenterConditionGroupRelVo groupRel : groupRelList) {
				groupRelMap.put(groupRel.getFrom()+"_"+groupRel.getTo(), groupRel.getJoinType());
			}
		}
		List<WorkcenterConditionGroupVo> groupList = workcenterVo.getConditionGroupList();
		if(CollectionUtils.isEmpty(groupList)) {
			return "";
		}
		String fromGroupUuid = null;
		String toGroupUuid = groupList.get(0).getUuid();
		for(WorkcenterConditionGroupVo group : groupList) {
			Map<String,String> conditionRelMap = new HashMap<String,String>();
			if(fromGroupUuid != null) {
				toGroupUuid = group.getUuid();
				whereSb.append(groupRelMap.get(fromGroupUuid+"_"+toGroupUuid));
			}
			whereSb.append("(");
			List<WorkcenterConditionRelVo> conditionRelList = group.getConditionRelList();
			if(CollectionUtils.isNotEmpty(conditionRelList)) {
				//将condition 以连接表达式 存 Map<fromUuid_toUuid,joinType> 
				for(WorkcenterConditionRelVo conditionRel : conditionRelList) {
					conditionRelMap.put(conditionRel.getFrom()+"_"+conditionRel.getTo(),conditionRel.getJoinType());
				}
			}
			List<WorkcenterConditionVo> conditionList = group.getConditionList();
			String fromConditionUuid = null;
			String toConditionUuid = conditionList.get(0).getUuid();
			for(WorkcenterConditionVo condition : conditionList) {
				if(fromConditionUuid != null) {
					toConditionUuid = condition.getUuid();
					whereSb.append(conditionRelMap.get(fromConditionUuid+"_"+toConditionUuid));
				}
				Object value = condition.getValueList().get(0);
				IWorkcenterCondition workcenterCondition = WorkcenterConditionFactory.getHandler(condition.getName());
				//Date 类型过滤条件特殊处理
				if(workcenterCondition != null && workcenterCondition.getHandler(ProcessWorkcenterConditionModel.SIMPLE.getValue()).equals(ProcessFormHandlerType.DATE.toString())){
					JSONArray dateJSONArray = JSONArray.parseArray(condition.getValueList().toString());
					if(CollectionUtils.isNotEmpty(dateJSONArray)) {
						JSONObject dateValue = (JSONObject) dateJSONArray.get(0);
						SimpleDateFormat format = new SimpleDateFormat(TimeUtil.TIME_FORMAT);
						String startTime = StringUtils.EMPTY;
						String endTime = StringUtils.EMPTY;
						String expression = condition.getExpression();
						if(dateValue.containsKey("startTime")) {
							startTime = format.format(new Date(dateValue.getLong("startTime")));
							endTime = format.format(new Date(dateValue.getLong("endTime")));
						}else {
							startTime = TimeUtil.timeTransfer(dateValue.getInteger("timeRange"), dateValue.getString("timeUnit"));
							endTime = TimeUtil.timeNow();
						}
						if(StringUtils.isEmpty(startTime)) {
							expression = ProcessExpression.LESSTHAN.getExpression();
							startTime = endTime;
						}else if(StringUtils.isEmpty(endTime)) {
							expression = ProcessExpression.GREATERTHAN.getExpression();
						}
						whereSb.append(String.format(ProcessExpression.getExpressionEs(expression),condition.getName(),startTime,endTime));
					}else {
						throw new WorkcenterConditionException(condition.getName());
					}
				}else {
					if(condition.getValueList().size()>1) {
						value = String.join("','",condition.getValueList());
					}
					whereSb.append(String.format(ProcessExpression.getExpressionEs(condition.getExpression()),condition.getName(),String.format("'%s'",  value)));
				}
				fromConditionUuid = toConditionUuid;
			}
			
			whereSb.append(")");
			fromGroupUuid = toGroupUuid;
		}
		return whereSb.toString();
	}

}