package neatlogic.module.process.api.priority;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.process.auth.PRIORITY_MODIFY;
import neatlogic.framework.process.dto.PriorityVo;
import neatlogic.framework.process.exception.priority.PriorityIsInvokedException;
import neatlogic.framework.process.exception.priority.PriorityNotFoundException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.process.dao.mapper.catalog.PriorityMapper;
import neatlogic.module.process.dao.mapper.processtask.ProcessTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.DELETE)
@AuthAction(action = PRIORITY_MODIFY.class)
public class PriorityDeleteApi extends PrivateApiComponentBase {

    @Resource
    private PriorityMapper priorityMapper;

    @Resource
    private ProcessTaskMapper processTaskMapper;

    @Override
    public String getToken() {
        return "process/priority/delete";
    }

    @Override
    public String getName() {
        return "优先级信息删除";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "uuid", type = ApiParamType.STRING, isRequired = true, desc = "优先级uuid")
    })
    @Description(desc = "优先级信息删除")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String uuid = jsonObj.getString("uuid");
        PriorityVo priorityVo = priorityMapper.getPriorityByUuid(uuid);
        if (priorityVo == null) {
            throw new PriorityNotFoundException(uuid);
        }
        if (priorityMapper.checkPriorityIsInvoked(uuid) > 0) {
            throw new PriorityIsInvokedException(priorityVo.getName());
        }
        if (processTaskMapper.getProcessTaskIdByPriorityUuidLimitOne(uuid) != null) {
            throw new PriorityIsInvokedException(priorityVo.getName());
        }
        priorityMapper.deletePriorityByUuid(uuid);
        priorityMapper.updateSortDecrement(priorityVo.getSort(), null);
        return null;
    }

}
