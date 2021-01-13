package codedriver.module.process.api.process;

import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.OperationType;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.process.dto.WorkerDispatcherVo;
import codedriver.framework.process.workerdispatcher.core.WorkerDispatcherFactory;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class WorkerDispatcherListApi extends PrivateApiComponentBase {
	
	@Override
	public String getToken() {
		return "process/workerdispatcher/list";
	}

	@Override
	public String getName() {
		return "处理人分派器列表接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Override
	@Output({
			@Param(name = "workerDispatcherList",
					explode = WorkerDispatcherVo[].class,
					desc = "处理人分派器列表") })
	@Description(desc = "处理人分派器列表接口")
	public Object myDoService(JSONObject jsonObj) throws Exception {
		return WorkerDispatcherFactory.getAllActiveWorkerDispatcher();
	}
	
}
