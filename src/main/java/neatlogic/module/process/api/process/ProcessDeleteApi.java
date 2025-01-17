package neatlogic.module.process.api.process;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.process.auth.PROCESS_MODIFY;
import neatlogic.framework.process.dto.ProcessDraftVo;
import neatlogic.framework.process.exception.process.ProcessNotFoundException;
import neatlogic.framework.process.exception.process.ProcessReferencedCannotBeDeleteException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.process.dao.mapper.process.ProcessMapper;
import neatlogic.module.process.service.ProcessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.DELETE)
@AuthAction(action = PROCESS_MODIFY.class)
public class ProcessDeleteApi extends PrivateApiComponentBase {

	@Resource
	private ProcessMapper processMapper;

	@Resource
	private ProcessService processService;
	
	@Override
	public String getToken() {
		return "process/delete";
	}

	@Override
	public String getName() {
		return "删除流程";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name = "uuid", type = ApiParamType.STRING, isRequired = true, desc = "流程uuid")
	})
	@Output({
		@Param(name = "uuid", type = ApiParamType.STRING, desc = "流程uuid")
	})
	@Description(desc = "删除流程")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String uuid = jsonObj.getString("uuid");
		if(processMapper.checkProcessIsExists(uuid) == 0) {
			throw new ProcessNotFoundException(uuid);
		}
		if(processMapper.getProcessReferenceCount(uuid) > 0) {
			throw new ProcessReferencedCannotBeDeleteException(uuid);
		}
		processService.deleteProcessRelevantData(uuid);
		processMapper.deleteProcessByUuid(uuid);
		ProcessDraftVo processDraftVo = new ProcessDraftVo();
		processDraftVo.setProcessUuid(uuid);
		processMapper.deleteProcessDraft(processDraftVo);
		return uuid;
	}

}
