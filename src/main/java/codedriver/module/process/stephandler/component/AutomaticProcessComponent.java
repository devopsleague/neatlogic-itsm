/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.process.stephandler.component;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.asynchronization.threadpool.TransactionSynchronizationPool;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.process.constvalue.ProcessStepHandlerType;
import codedriver.framework.process.constvalue.ProcessStepMode;
import codedriver.framework.process.constvalue.ProcessTaskStatus;
import codedriver.framework.process.constvalue.ProcessTaskStepDataType;
import codedriver.framework.process.dao.mapper.ProcessTaskStepDataMapper;
import codedriver.framework.process.dto.ProcessTaskStepDataVo;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskStepWorkerVo;
import codedriver.framework.process.dto.automatic.AutomaticConfigVo;
import codedriver.framework.process.exception.core.ProcessTaskException;
import codedriver.framework.process.stephandler.core.ProcessStepHandlerBase;
import codedriver.framework.util.TimeUtil;
import codedriver.module.process.service.ProcessTaskAutomaticService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AutomaticProcessComponent extends ProcessStepHandlerBase {
    static Logger logger = LoggerFactory.getLogger(AutomaticProcessComponent.class);
    private static final ThreadLocal<List<RequestFirstThread>> AUTOMATIC_LIST = new ThreadLocal<>();

    @Resource
    ProcessTaskStepDataMapper processTaskStepDataMapper;

    @Resource
    ProcessTaskAutomaticService processTaskAutomaticService;


    @Override
    public String getHandler() {
        return ProcessStepHandlerType.AUTOMATIC.getHandler();
    }

    @Override
    public String getType() {
        return ProcessStepHandlerType.AUTOMATIC.getType();
    }

    @Override
    public ProcessStepMode getMode() {
        return ProcessStepMode.MT;
    }

    @SuppressWarnings("serial")
    @Override
    public JSONObject getChartConfig() {
        return new JSONObject() {
            {
                this.put("icon", "tsfont-auto");
                this.put("shape", "L-rectangle-50%:R-rectangle-50%");
                this.put("width", 68);
                this.put("height", 40);
            }
        };
    }

    @Override
    public String getName() {
        return ProcessStepHandlerType.AUTOMATIC.getName();
    }

    @Override
    public int getSort() {
        return 8;
    }

    @Override
    protected int myActive(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        ProcessTaskStepVo processTaskStepVo = processTaskMapper.getProcessTaskStepBaseInfoById(currentProcessTaskStepVo.getId());
        String stepConfig = selectContentByHashMapper.getProcessTaskStepConfigByHash(processTaskStepVo.getConfigHash());
        //获取参数
        JSONObject automaticConfig = null;
        try {
            JSONObject stepConfigObj = JSONObject.parseObject(stepConfig);
            currentProcessTaskStepVo.getParamObj().putAll(stepConfigObj);
            if (MapUtils.isNotEmpty(stepConfigObj)) {
                automaticConfig = stepConfigObj.getJSONObject("automaticConfig");
            }
        } catch (Exception ex) {
            logger.error("hash为" + processTaskStepVo.getConfigHash() + "的processtask_step_config内容不是合法的JSON格式", ex);
        }
        //初始化audit
        AutomaticConfigVo automaticConfigVo = new AutomaticConfigVo(automaticConfig);
        processTaskAutomaticService.initProcessTaskStepData(currentProcessTaskStepVo, automaticConfigVo, null, "request");
        requestFirst(currentProcessTaskStepVo, automaticConfig);
        return 0;
    }


    /**
     * automatic 第一次请求
     *
     * @param automaticConfig
     */
    private void requestFirst(ProcessTaskStepVo currentProcessTaskStepVo, JSONObject automaticConfig) {
        TransactionSynchronizationPool.execute(new RequestFirstThread(currentProcessTaskStepVo, automaticConfig));
    }

    private class RequestFirstThread extends CodeDriverThread {
        private JSONObject automaticConfig;
        private ProcessTaskStepVo currentProcessTaskStepVo;

        private RequestFirstThread(ProcessTaskStepVo currentProcessTaskStepVo, JSONObject automaticConfig) {
            super("REQUEST-FIRST");
            this.automaticConfig = automaticConfig;
            this.currentProcessTaskStepVo = currentProcessTaskStepVo;
        }

        @Override
        protected void execute() {
            UserContext.init(SystemUser.SYSTEM.getUserVo(), SystemUser.SYSTEM.getTimezone());
            AutomaticConfigVo automaticConfigVo = new AutomaticConfigVo(automaticConfig);
            JSONObject timeWindowConfig = automaticConfigVo.getTimeWindowConfig();
            automaticConfigVo.setIsRequest(true);
            Integer isTimeToRun = null;
            //检验执行时间窗口
            if (timeWindowConfig != null) {
                isTimeToRun = TimeUtil.isInTimeWindow(timeWindowConfig.getString("startTime"), timeWindowConfig.getString("endTime"));
            }
            if (timeWindowConfig == null || isTimeToRun == 0) {
                processTaskAutomaticService.runRequest(automaticConfigVo, currentProcessTaskStepVo);
            } else {//loadJob,定时执行第一次请求
                //初始化audit执行状态
                JSONObject audit = null;
                ProcessTaskStepDataVo data = processTaskStepDataMapper.getProcessTaskStepData(new ProcessTaskStepDataVo(currentProcessTaskStepVo.getProcessTaskId(), currentProcessTaskStepVo.getId(), ProcessTaskStepDataType.AUTOMATIC.getValue(), SystemUser.SYSTEM.getUserId()));
                JSONObject dataObject = data.getData();
                audit = dataObject.getJSONObject("requestAudit");
                audit.put("status", ProcessTaskStatus.getJson(ProcessTaskStatus.PENDING.getValue()));
                processTaskAutomaticService.initJob(automaticConfigVo, currentProcessTaskStepVo, dataObject);
                data.setData(dataObject.toJSONString());
                processTaskStepDataMapper.replaceProcessTaskStepData(data);
            }
        }

    }


    @Override
    protected int myTransfer(ProcessTaskStepVo currentProcessTaskStepVo, List<ProcessTaskStepWorkerVo> workerList) throws ProcessTaskException {
        return 1;
    }

    @Override
    protected int myAssign(ProcessTaskStepVo currentProcessTaskStepVo, Set<ProcessTaskStepWorkerVo> workerSet) throws ProcessTaskException {
        return defaultAssign(currentProcessTaskStepVo, workerSet);
    }

    @Override
    protected Set<Long> myGetNext(ProcessTaskStepVo currentProcessTaskStepVo, List<Long> nextStepIdList, Long nextStepId) throws ProcessTaskException {
        return defaultGetNext(nextStepIdList, nextStepId);
    }

    @Override
    protected int myRedo(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myStart(ProcessTaskStepVo processTaskStepVo) {
        return 0;
    }

    @Override
    public Boolean isAllowStart() {
        return null;
    }

    @Override
    protected int myStartProcess(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    protected int myHandle(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myComplete(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myCompleteAudit(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myReapproval(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myReapprovalAudit(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myRetreat(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 1;
    }

    @Override
    protected int myAbort(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myBack(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

    @Override
    protected int myHang(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int myRecover(ProcessTaskStepVo currentProcessTaskStepVo) {
        return 0;
    }

    @Override
    protected int mySaveDraft(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 1;
    }

    @Override
    protected int myPause(ProcessTaskStepVo currentProcessTaskStepVo) throws ProcessTaskException {
        return 0;
    }

}