package neatlogic.module.process.operationauth.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.process.operationauth.core.IOperationType;
import neatlogic.framework.process.constvalue.ProcessTaskStepOperationType;
import neatlogic.framework.process.dto.ProcessTaskStepVo;
import neatlogic.framework.process.dto.ProcessTaskVo;
import neatlogic.framework.process.exception.operationauth.ProcessTaskAutomaticHandlerNotEnableOperateException;
import neatlogic.framework.process.exception.operationauth.ProcessTaskPermissionDeniedException;
import neatlogic.framework.process.operationauth.core.OperationAuthHandlerBase;
import neatlogic.framework.process.operationauth.core.OperationAuthHandlerType;
import neatlogic.framework.process.operationauth.core.TernaryPredicate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class AutomaticOperateHandler extends OperationAuthHandlerBase {

    private final Map<IOperationType,
        TernaryPredicate<ProcessTaskVo, ProcessTaskStepVo, String, Map<Long, Map<IOperationType, ProcessTaskPermissionDeniedException>>, JSONObject>> operationBiPredicateMap = new HashMap<>();

    @PostConstruct
    public void init() {
        operationBiPredicateMap.put(ProcessTaskStepOperationType.STEP_RETREAT,
            (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
                Long id = processTaskStepVo.getId();
                IOperationType operationType = ProcessTaskStepOperationType.STEP_RETREAT;
                //1.提示“自动处理节点不支持'撤回'操作”；
                operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                        .put(operationType, new ProcessTaskAutomaticHandlerNotEnableOperateException(operationType));
                return false;
            });
        operationBiPredicateMap.put(ProcessTaskStepOperationType.STEP_WORK,
            (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
                Long id = processTaskStepVo.getId();
                IOperationType operationType = ProcessTaskStepOperationType.STEP_WORK;
                //1.提示“自动处理节点不支持'处理'操作”；
                operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                        .put(operationType, new ProcessTaskAutomaticHandlerNotEnableOperateException(operationType));
                return false;
            });
        operationBiPredicateMap.put(ProcessTaskStepOperationType.STEP_COMMENT,
            (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap, extraParam) -> {
                Long id = processTaskStepVo.getId();
                IOperationType operationType = ProcessTaskStepOperationType.STEP_COMMENT;
                //1.提示“自动处理节点不支持'回复'操作”；
                operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                        .put(operationType, new ProcessTaskAutomaticHandlerNotEnableOperateException(operationType));
                return false;
            });
    }

    @Override
    public String getHandler() {
        return OperationAuthHandlerType.AUTOMATIC.getValue();
    }

    @Override
    public Map<IOperationType, TernaryPredicate<ProcessTaskVo, ProcessTaskStepVo, String, Map<Long, Map<IOperationType, ProcessTaskPermissionDeniedException>>, JSONObject>>
        getOperationBiPredicateMap() {
        return operationBiPredicateMap;
    }

}
