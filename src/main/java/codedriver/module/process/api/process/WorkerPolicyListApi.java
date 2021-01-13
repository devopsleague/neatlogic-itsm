package codedriver.module.process.api.process;

import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.OperationType;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.process.dto.WorkerPolicyVo;
import codedriver.framework.process.workerpolicy.core.WorkerPolicyHandlerFactory;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class WorkerPolicyListApi extends PrivateApiComponentBase {

	@Override
	public String getToken() {
		return "process/worker/policy/list";
	}

	@Override
	public String getName() {
		return "指派策略列表接口";
	}

	@Override
	public String getConfig() {
		return null;
	}
	@Output({@Param(name="Return", explode = WorkerPolicyVo[].class, desc = "指派策略列表")})
	@Description(desc = "指派策略列表接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		return WorkerPolicyHandlerFactory.getAllActiveWorkerPolicy();
	}

}
