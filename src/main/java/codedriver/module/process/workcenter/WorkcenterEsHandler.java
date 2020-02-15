package codedriver.module.process.workcenter;

import static com.techsure.multiattrsearch.query.QueryBuilder.attr;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.techsure.multiattrsearch.MultiAttrsObjectPatch;
import com.techsure.multiattrsearch.MultiAttrsObjectPool;
import com.techsure.multiattrsearch.query.QueryBuilder;
import com.techsure.multiattrsearch.query.QueryResult;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.asynchronization.threadpool.CommonThreadPool;
import codedriver.framework.process.dao.mapper.CatalogMapper;
import codedriver.framework.process.dao.mapper.ChannelMapper;
import codedriver.framework.process.dao.mapper.ProcessTaskAuditMapper;
import codedriver.framework.process.dao.mapper.ProcessTaskMapper;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerUtilBase;
import codedriver.module.process.constvalue.ProcessStepType;
import codedriver.module.process.constvalue.ProcessTaskStepAction;
import codedriver.module.process.dto.CatalogVo;
import codedriver.module.process.dto.ChannelVo;
import codedriver.module.process.dto.ProcessTaskContentVo;
import codedriver.module.process.dto.ProcessTaskStepAuditVo;
import codedriver.module.process.dto.ProcessTaskStepContentVo;
import codedriver.module.process.dto.ProcessTaskStepVo;
import codedriver.module.process.dto.ProcessTaskStepWorkerVo;
import codedriver.module.process.dto.ProcessTaskVo;
import codedriver.module.process.workcenter.dto.WorkcenterVo;

@Service
public class WorkcenterEsHandler extends CodeDriverThread{
	static Logger logger = LoggerFactory.getLogger(ProcessStepHandlerUtilBase.class);
	private MultiAttrsObjectPool objectPool;
	private ProcessTaskStepVo currentProcessTaskStepVo;
	private static final ThreadLocal<List<WorkcenterEsHandler>> ES_HANDLERS = new ThreadLocal<>();
	
	
	protected static ProcessTaskMapper processTaskMapper;
	
	protected static ChannelMapper channelMapper;
	
	protected static CatalogMapper catalogMapper;
	
	protected static ProcessTaskAuditMapper processTaskAuditMapper;
	
	@Autowired
	public void setProcessTaskMapper(ProcessTaskMapper _processTaskMapper) {
		processTaskMapper = _processTaskMapper;
	}
	
	@Autowired
	public void setChannelMapper(ChannelMapper _channelMapper) {
		channelMapper = _channelMapper;
	}
	
	@Autowired
	public void setChannelMapper(CatalogMapper _catalogMapper) {
		catalogMapper = _catalogMapper;
	}
	
	@Autowired
	public void setProcessTaskAuditMapper(ProcessTaskAuditMapper _processTaskAuditMapper) {
		processTaskAuditMapper = _processTaskAuditMapper;
	}
	
	public WorkcenterEsHandler(ProcessTaskStepVo _currentProcessTaskStepVo) {
		currentProcessTaskStepVo = _currentProcessTaskStepVo;
	}
	
	@PostConstruct
	public void init() {
		/*Map<String, String> esClusters = Config.ES_CLUSTERS;
		if (esClusters.isEmpty()) {
			throw new IllegalStateException("ES集群信息未配置，es.cluster.<cluster-name>=<ip:port>[,<ip:port>...]");
		}

		MultiAttrsSearchConfig config = new MultiAttrsSearchConfig();
		config.setPoolName(POOL_NAME);

		Map.Entry<String, String> cluster = esClusters.entrySet().iterator().next();
		config.addCluster(cluster.getKey(), cluster.getValue());
		if (esClusters.size() > 1) {
			logger.warn("multiple clusters available, only cluster {} was used (picked randomly) for testing",
					cluster.getKey());
		}

		objectPool = MultiAttrsSearch.getObjectPool(config);*/
	}

	/**
	 *  创建查询器
	 * @param tenantId
	 * @return
	 */
	public QueryBuilder createQueryBuilder(String tenantId) {
		return objectPool.createQueryBuilder().from(tenantId);
	}
	
	/**
	 *   搜索工单
	 * @param workcenterVo
	 * @return 
	 */
	public QueryResult searchTask(WorkcenterVo workcenterVo){
		//TODO lvzk 条件解析拼成es api 的格式查询
		QueryBuilder.ConditionBuilder cond = null;
		cond = attr("title").contains("标题1");
        /*if (status != null) {
            cond = attr("status").eq(status);
        }
        if (!tags.isEmpty()) {
            cond = cond == null ? attr("tags").containsAny(tags) : cond.and().attr("tags").containsAny(tags);
        }
        if (title != null && !StringUtils.isBlank(title)) {
            cond = cond == null ? attr("title").contains(title) : cond.and().attr("title").contains(title);
        }*/
		QueryBuilder builder = createQueryBuilder(TenantContext.get().getTenantUuid())
             .select("title", "status", "created_at")
             .orderBy("created_time", false)
            .limit(workcenterVo.getCurrentPage(), workcenterVo.getPageSize());
	     if (cond != null) {
	         builder.where(cond);
	     }
	     QueryResult result = builder.build().execute();
	     return result;
	}
	
	protected static synchronized void update(ProcessTaskStepVo currentProcessTaskStepVo) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			WorkcenterEsHandler handler = new WorkcenterEsHandler(currentProcessTaskStepVo);
			CommonThreadPool.execute(handler);
		} else {
			List<WorkcenterEsHandler> handlerList = ES_HANDLERS.get();
			if (handlerList == null) {
				handlerList = new ArrayList<>();
				ES_HANDLERS.set(handlerList);
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
					@Override
					public void afterCommit() {
						List<WorkcenterEsHandler> handlerList = ES_HANDLERS.get();
						for (WorkcenterEsHandler handler : handlerList) {
							CommonThreadPool.execute(handler);
						}
					}

					@Override
					public void afterCompletion(int status) {
						ES_HANDLERS.remove();
					}
				});
			}
			handlerList.add(new WorkcenterEsHandler(currentProcessTaskStepVo));
		}
	}
	
	@Override
	protected void execute() {
		String oldName = Thread.currentThread().getName();
		Thread.currentThread().setName("WOEKCENTER-UPDATE-" + currentProcessTaskStepVo.getId());
		try {
			updateTask(TenantContext.get().getTenantUuid(),currentProcessTaskStepVo);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			Thread.currentThread().setName(oldName);
		}
	}
	
	/**
	 *  跟新es 工单信息
	 * @param tenantId
	 * @param currentProcessTaskStepVo
	 */
	public void updateTask(String tenantId,ProcessTaskStepVo currentProcessTaskStepVo) {
		 Long taskId = currentProcessTaskStepVo.getProcessTaskId();
		 objectPool.checkout(tenantId, null);
		 MultiAttrsObjectPatch patch = objectPool.update(currentProcessTaskStepVo.getProcessTaskId().toString());
		 /** 获取工单信息 **/
		 ProcessTaskVo processTaskVo = processTaskMapper.getProcessTaskBaseInfoById(currentProcessTaskStepVo.getProcessTaskId());
		 /** 获取服务信息 **/
		 ChannelVo channel = channelMapper.getChannelByUuid(processTaskVo.getChannelUuid());
		 /** 获取服务目录信息 **/
		 CatalogVo catalog = catalogMapper.getCatalogByUuid(channel.getParentUuid());
		 /** 获取开始节点内容信息 **/
		 ProcessTaskContentVo startContentVo = null;
		 List<ProcessTaskStepVo> stepList = processTaskMapper.getProcessTaskStepByProcessTaskIdAndType(currentProcessTaskStepVo.getProcessTaskId(), ProcessStepType.START.getValue());
		 if (stepList.size() == 1) {
			ProcessTaskStepVo startStepVo = stepList.get(0);
			List<ProcessTaskStepContentVo> contentList = processTaskMapper.getProcessTaskStepContentProcessTaskStepId(startStepVo.getId());
			if (contentList.size() > 0) {
				ProcessTaskStepContentVo contentVo = contentList.get(0);
				startContentVo = processTaskMapper.getProcessTaskContentByHash(contentVo.getContentHash());
			}
		 }
		 /** 获取转交记录 **/
		 List<ProcessTaskStepAuditVo> transferAuditList = processTaskAuditMapper.getProcessTaskAuditList(new ProcessTaskStepAuditVo(processTaskVo.getId(),ProcessTaskStepAction.TRANSFER.getValue()));
		 List<String> transferUserIdList = new ArrayList<String>();
		 for(ProcessTaskStepAuditVo auditVo : transferAuditList) {
			 transferUserIdList.add(auditVo.getUserId());
		 }
		 /** 获取工单当前步骤 **/
		 List<ProcessTaskStepVo>  processTaskActiveStepList = processTaskMapper.getProcessTaskActiveStepByProcessTaskId(currentProcessTaskStepVo.getProcessTaskId());
		 List<String> activeStepIdList = new ArrayList<String>();
		 List<String> activeStepWorkerList = new ArrayList<String>();
		 List<String> activeStepStatusList = new ArrayList<String>();
		 for(ProcessTaskStepVo step : processTaskActiveStepList) {
			 activeStepIdList.add(step.getId().toString());
			 for(ProcessTaskStepWorkerVo worker : step.getWorkerList()) {
				 if(!StringUtils.isBlank(worker.getTeamUuid())) {
					 activeStepWorkerList.add(step.getId()+"@team#"+worker.getTeamUuid());
				 }
				 if(!StringUtils.isBlank(worker.getUserId())) {
					 activeStepWorkerList.add(step.getId()+"@user#"+worker.getUserId());
				 }
				 if(!StringUtils.isBlank(worker.getRoleName())) {
					 activeStepWorkerList.add(step.getId()+"@role#"+worker.getRoleName());
				 }
			 }
			 activeStepStatusList.add(step.getId()+"@"+step.getStatus());
			
		 }
		 
		 //标题
		 patch.set("title", processTaskVo.getTitle());
		 //工单状态
		 patch.set("status", processTaskVo.getStatusText());
		 //优先级
		 patch.set("priority", processTaskVo.getPriority());
		 //服务目录
		 patch.set("catalog", catalog.getUuid());
		 //服务
		 patch.set("channel", channel.getUuid());
		 //上报内容
		 patch.set("content", startContentVo.getContent());
		 //上报人
		 patch.set("owner",processTaskVo.getOwner());
		 //代报人
		 patch.set("reporter", processTaskVo.getReporter());
		 //转交人
		 patch.setStrings("transferFromUsers", transferUserIdList);
		 //当前步骤idList
		 patch.setStrings("stepIds", activeStepIdList);
		 //当前步骤处理人List
		 patch.setStrings("stepUsers", activeStepWorkerList);
		 //当前步骤状态
		 patch.setStrings("stepStatus", activeStepStatusList);
		 //时间窗口
		 patch.set("worktime", channel.getWorktimeUuid());
		 //超时时间
		 patch.set("expiredTime", processTaskVo.getExpireTime());
		 //表单属性
		 
		 
		 
		 
		 try {
			 patch.commit();
		 } catch (Exception e) {
			 logger.error("failed to update title of task{id={}}, reason: {}", taskId, e.getMessage());
		 }
	 }
	
}
