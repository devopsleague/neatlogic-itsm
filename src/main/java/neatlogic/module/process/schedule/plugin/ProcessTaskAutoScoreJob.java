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

package neatlogic.module.process.schedule.plugin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.process.constvalue.ProcessTaskStatus;
import neatlogic.module.process.dao.mapper.processtask.ProcessTaskMapper;
import neatlogic.module.process.dao.mapper.score.ProcessTaskScoreMapper;
import neatlogic.module.process.dao.mapper.score.ScoreTemplateMapper;
import neatlogic.framework.process.dto.ProcessTaskVo;
import neatlogic.framework.process.dto.score.ProcessTaskAutoScoreVo;
import neatlogic.framework.process.dto.score.ProcessTaskScoreVo;
import neatlogic.framework.process.dto.score.ScoreTemplateDimensionVo;
import neatlogic.framework.process.dto.score.ScoreTemplateVo;
import neatlogic.framework.process.stephandler.core.ProcessStepHandlerFactory;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.util.I18n;
import neatlogic.framework.util.WorkTimeUtil;
import neatlogic.framework.worktime.dao.mapper.WorktimeMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 工单自动评分定时类
 */
@Component
@DisallowConcurrentExecution
public class ProcessTaskAutoScoreJob extends JobBase {

	@Autowired
	private ProcessTaskMapper processTaskMapper;

	@Autowired
	private ScoreTemplateMapper scoreTemplateMapper;

	@Autowired
	private ProcessTaskScoreMapper processTaskScoreMapper;

	@Autowired
	private WorktimeMapper worktimeMapper;

	@Override
    public Boolean isMyHealthy(JobObject jobObject) {
        Long processTaskId = Long.valueOf(jobObject.getJobName());
        List<ProcessTaskScoreVo> processtaskScoreVos = processTaskScoreMapper.getProcessTaskScoreByProcesstaskId(processTaskId);
        if (CollectionUtils.isEmpty(processtaskScoreVos)) {
            return true;
        } else {
            return false;
        }
    }

	@Override
	public void reloadJob(JobObject jobObject) {
		String tenantUuid = jobObject.getTenantUuid();
		TenantContext.get().switchTenant(tenantUuid);
		Long processTaskId = Long.valueOf(jobObject.getJobName());
		List<ProcessTaskScoreVo> processtaskScoreVos = processTaskScoreMapper.getProcessTaskScoreByProcesstaskId(processTaskId);
		if(CollectionUtils.isEmpty(processtaskScoreVos)){
		    /** 如果没有评分记录，那么读取评分配置 */
		    ProcessTaskVo task = processTaskMapper.getProcessTaskById(processTaskId);
	        if(task != null && task.getStatus().equals(ProcessTaskStatus.SUCCEED.getValue())) {
	            String config = processTaskScoreMapper.getProcessTaskAutoScoreConfigByProcessTaskId(processTaskId);
	            Integer autoTime = (Integer)JSONPath.read(config, "config.autoTime");
	            if(autoTime != null) {
	                String autoTimeType = (String)JSONPath.read(config, "config.autoTimeType");
	                /**
            	            * 如果没有设置评分时限类型是自然日还是工作日，默认按自然日顺延
            	            * 如果设置为工作日，那么获取当前时间以后的工作日历，按工作日历顺延
	                 */
	                Date autoScoreDate = null;
	                if("workDay".equals(autoTimeType) && worktimeMapper.checkWorktimeIsExists(task.getWorktimeUuid()) > 0) {
	                    long expireTime = WorkTimeUtil.calculateExpireTime(task.getEndTime().getTime(), TimeUnit.DAYS.toMillis(autoTime), task.getWorktimeUuid());
	                    autoScoreDate = new Date(expireTime);
	                }else {
	                    autoScoreDate = new Date(task.getEndTime().getTime() + TimeUnit.DAYS.toMillis(autoTime));
	                }
	                ProcessTaskAutoScoreVo processTaskAutoScoreVo = new ProcessTaskAutoScoreVo();
	                processTaskAutoScoreVo.setProcessTaskId(processTaskId);
	                processTaskAutoScoreVo.setTriggerTime(autoScoreDate);
	                processTaskScoreMapper.updateProcessTaskAutoScoreByProcessTaskId(processTaskAutoScoreVo);
	                JobObject.Builder newJobObjectBuilder = new JobObject.Builder(processTaskId.toString(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid())
	                    .withBeginTime(autoScoreDate)
	                    .withIntervalInSeconds(60 * 60)
	                    .withRepeatCount(0);
	                JobObject newJobObject = newJobObjectBuilder.build();
	                schedulerManager.loadJob(newJobObject);
	            }
	        }
		}
	}

	@Override
	public void initJob(String tenantUuid) {
	    List<Long> processTaskIdList = processTaskScoreMapper.getAllProcessTaskAutoScoreProcessTaskIdList();
	    for(Long processTaskId : processTaskIdList) {
	        JobObject.Builder jobObjectBuilder = new JobObject.Builder(processTaskId.toString(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid());
            JobObject jobObject = jobObjectBuilder.build();
            this.reloadJob(jobObject);
	    }
	}

	@Override
	public void executeInternal(JobExecutionContext context, JobObject jobObject) throws JobExecutionException {	        	    
        Long processTaskId = Long.valueOf(jobObject.getJobName());
	    List<ProcessTaskScoreVo> processTaskScoreVos = processTaskScoreMapper.getProcessTaskScoreByProcesstaskId(processTaskId);
	    if(CollectionUtils.isEmpty(processTaskScoreVos)) {
	        ProcessTaskVo task = processTaskMapper.getProcessTaskById(processTaskId);
	        if(task != null) {
	            String config = processTaskScoreMapper.getProcessTaskAutoScoreConfigByProcessTaskId(processTaskId);
	            Long scoreTemplateId = (Long)JSONPath.read(config, "scoreTemplateId");
	            ScoreTemplateVo template = scoreTemplateMapper.getScoreTemplateById(scoreTemplateId);
	            if(template != null) {
	                List<ScoreTemplateDimensionVo> dimensionList = template.getDimensionList();
	                if(CollectionUtils.isNotEmpty(dimensionList)){
	                    for(ScoreTemplateDimensionVo vo : dimensionList){
	                        vo.setScore(5);
	                    }
	                    JSONObject paramObj = new JSONObject();
	                    paramObj.put("scoreTemplateId", scoreTemplateId);
	                    paramObj.put("scoreDimensionList", dimensionList);
	                    paramObj.put("content", new I18n("系统自动评价").toString());
	                    task.setParamObj(paramObj);
                        /** 执行转交前，设置当前用户为system,用于权限校验 **/
                        UserContext.init(SystemUser.SYSTEM);
                        ProcessStepHandlerFactory.getHandler().scoreProcessTask(task);
	                }
	            }
	        }
	    }	    
	}

	@Override
	public String getGroupName() {
		return TenantContext.get().getTenantUuid() + "-PROCESSTASK-AUTOSCORE";
	}

}
