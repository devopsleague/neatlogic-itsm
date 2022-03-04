package codedriver.module.process.operationauth.handler;

import codedriver.framework.process.constvalue.ProcessTaskOperationType;
import codedriver.framework.process.dto.ProcessTaskStepVo;
import codedriver.framework.process.dto.ProcessTaskVo;
import codedriver.framework.process.exception.operationauth.ProcessTaskPermissionDeniedException;
import codedriver.framework.process.exception.operationauth.ProcessTaskTimerHandlerNotEnableOperateException;
import codedriver.framework.process.operationauth.core.OperationAuthHandlerBase;
import codedriver.framework.process.operationauth.core.OperationAuthHandlerType;
import codedriver.framework.process.operationauth.core.TernaryPredicate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class TimerOperateHandler extends OperationAuthHandlerBase {

    private final Map<ProcessTaskOperationType,
        TernaryPredicate<ProcessTaskVo, ProcessTaskStepVo, String, Map<Long, Map<ProcessTaskOperationType, ProcessTaskPermissionDeniedException>>>> operationBiPredicateMap = new HashMap<>();

    @PostConstruct
    public void init() {
        operationBiPredicateMap.put(ProcessTaskOperationType.STEP_RETREAT,
            (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap) -> {
                Long id = processTaskStepVo.getId();
                ProcessTaskOperationType operationType = ProcessTaskOperationType.STEP_RETREAT;
                operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                        .put(operationType, new ProcessTaskTimerHandlerNotEnableOperateException(operationType));
                return false;
            });
        operationBiPredicateMap.put(ProcessTaskOperationType.STEP_WORK,
            (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap) -> {
                Long id = processTaskStepVo.getId();
                ProcessTaskOperationType operationType = ProcessTaskOperationType.STEP_WORK;
                operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                        .put(operationType, new ProcessTaskTimerHandlerNotEnableOperateException(operationType));
                return false;
            });
        operationBiPredicateMap.put(ProcessTaskOperationType.STEP_COMMENT,
            (processTaskVo, processTaskStepVo, userUuid, operationTypePermissionDeniedExceptionMap) -> {
                Long id = processTaskStepVo.getId();
                ProcessTaskOperationType operationType = ProcessTaskOperationType.STEP_COMMENT;
                operationTypePermissionDeniedExceptionMap.computeIfAbsent(id, key -> new HashMap<>())
                        .put(operationType, new ProcessTaskTimerHandlerNotEnableOperateException(operationType));
                return false;
            });
    }

    @Override
    public String getHandler() {
        return OperationAuthHandlerType.TIMER.getValue();
    }

    @Override
    public Map<ProcessTaskOperationType, TernaryPredicate<ProcessTaskVo, ProcessTaskStepVo, String, Map<Long, Map<ProcessTaskOperationType, ProcessTaskPermissionDeniedException>>>>
        getOperationBiPredicateMap() {
        return operationBiPredicateMap;
    }

}
