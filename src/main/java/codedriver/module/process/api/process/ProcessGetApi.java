package codedriver.module.process.api.process;

import codedriver.framework.process.util.ProcessConfigUtil;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.process.auth.PROCESS_MODIFY;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.dao.mapper.ProcessMapper;
import codedriver.framework.process.dto.ProcessVo;
import codedriver.framework.process.exception.process.ProcessNotFoundException;

import javax.annotation.Resource;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@AuthAction(action = PROCESS_MODIFY.class)
public class ProcessGetApi extends PrivateApiComponentBase {

    @Resource
    private ProcessMapper processMapper;

    @Override
    public String getToken() {
        return "process/get";
    }

    @Override
    public String getName() {
        return "获取单个流程图数据接口";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
			@Param(name = "uuid", type = ApiParamType.STRING, isRequired = true, desc = "流程uuid")
    })
    @Output({
			@Param(explode = ProcessVo.class)
    })
    @Description(desc = "获取单个流程图数据接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String uuid = jsonObj.getString("uuid");
        ProcessVo processVo = processMapper.getProcessByUuid(uuid);
        if (processVo == null) {
            throw new ProcessNotFoundException(uuid);
        }
        processVo.setConfig(ProcessConfigUtil.regulateProcessConfig(processVo.getConfig()));
        int count = processMapper.getProcessReferenceCount(uuid);
        processVo.setReferenceCount(count);
        return processVo;
    }
}
