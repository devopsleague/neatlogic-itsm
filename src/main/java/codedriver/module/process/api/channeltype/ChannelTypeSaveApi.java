package codedriver.module.process.api.channeltype;

import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.process.dao.mapper.ChannelTypeMapper;
import codedriver.framework.process.exception.channeltype.ChannelTypeHasReferenceException;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.process.auth.CHANNELTYPE_MODIFY;

import codedriver.module.process.service.ProcessTaskSerialnumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.dao.mapper.ProcessTaskSerialNumberMapper;
import codedriver.framework.process.dto.ChannelTypeVo;
import codedriver.framework.process.dto.ProcessTaskSerialNumberPolicyVo;
import codedriver.framework.process.exception.channeltype.ChannelTypeNameRepeatException;
import codedriver.framework.process.exception.channeltype.ChannelTypeNotFoundException;
import codedriver.framework.process.exception.processtaskserialnumberpolicy.ProcessTaskSerialNumberPolicyHandlerNotFoundException;
import codedriver.framework.process.processtaskserialnumberpolicy.core.IProcessTaskSerialNumberPolicyHandler;
import codedriver.framework.process.processtaskserialnumberpolicy.core.ProcessTaskSerialNumberPolicyHandlerFactory;

import java.util.Objects;
import java.util.function.Function;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
@AuthAction(action = CHANNELTYPE_MODIFY.class)
public class ChannelTypeSaveApi extends PrivateApiComponentBase {

    @Autowired
    private ChannelTypeMapper channelTypeMapper;
    @Autowired
    private ProcessTaskSerialNumberMapper processTaskSerialNumberMapper;
    @Autowired
    private ProcessTaskSerialnumberService processTaskSerialNumberService;

    @Override
    public String getToken() {
        return "process/channeltype/save";
    }

    @Override
    public String getName() {
        return "服务类型信息保存接口";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "uuid", type = ApiParamType.STRING, desc = "服务类型uuid"),
        @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "名称"),
        @Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", isRequired = true, desc = "状态"),
        @Param(name = "prefix", type = ApiParamType.STRING, isRequired = true, desc = "工单号前缀"),
        @Param(name = "handler", type = ApiParamType.STRING, isRequired = true, desc = "工单号策略"),
        @Param(name = "color", type = ApiParamType.STRING, isRequired = true, desc = "颜色"),
        @Param(name = "description", type = ApiParamType.STRING, xss = true, desc = "描述")})
    @Output({@Param(name = "Return", type = ApiParamType.STRING, desc = "服务类型uuid")})
    @Description(desc = "服务类型信息保存接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        ChannelTypeVo channelTypeVo = JSON.parseObject(jsonObj.toJSONString(), new TypeReference<ChannelTypeVo>() {});
        if (channelTypeMapper.checkChannelTypeNameIsRepeat(channelTypeVo) > 0) {
            throw new ChannelTypeNameRepeatException(channelTypeVo.getName());
        }

        Integer sort = channelTypeMapper.getChannelTypeMaxSort();
        if (sort == null) {
            sort = 0;
        }
        sort++;
        channelTypeVo.setSort(sort);
        String uuid = jsonObj.getString("uuid");
        if (uuid != null) {
            if (channelTypeMapper.checkChannelTypeIsExists(uuid) == 0) {
                throw new ChannelTypeNotFoundException(uuid);
            }
            if (channelTypeMapper.checkChannelTypeHasReference(uuid) > 0
                && Objects.equals(channelTypeVo.getIsActive(), 0)) {
                throw new ChannelTypeHasReferenceException(channelTypeVo.getName(), "禁用");
            }
            channelTypeMapper.updateChannelTypeByUuid(channelTypeVo);
        } else {
            channelTypeMapper.insertChannelType(channelTypeVo);
        }

        IProcessTaskSerialNumberPolicyHandler handler =
            ProcessTaskSerialNumberPolicyHandlerFactory.getHandler(channelTypeVo.getHandler());
        if (handler == null) {
            throw new ProcessTaskSerialNumberPolicyHandlerNotFoundException(channelTypeVo.getHandler());
        }
        JSONObject config = handler.makeupConfig(jsonObj);
        Long startValue = config.getLong("startValue");
        ProcessTaskSerialNumberPolicyVo policy = new ProcessTaskSerialNumberPolicyVo();
        policy.setChannelTypeUuid(channelTypeVo.getUuid());
        policy.setHandler(channelTypeVo.getHandler());
        policy.setConfig(config.toJSONString());
        ProcessTaskSerialNumberPolicyVo oldPolicy = processTaskSerialNumberMapper.getProcessTaskSerialNumberPolicyByChannelTypeUuid(uuid);
        if (oldPolicy != null) {
            processTaskSerialNumberMapper.updateProcessTaskSerialNumberPolicyByChannelTypeUuid(policy);
            if (oldPolicy.getSerialNumberSeed() > startValue) {
                Function<ProcessTaskSerialNumberPolicyVo, Long> function = (serialNumberPolicyVo) -> serialNumberPolicyVo.getSerialNumberSeed();
                processTaskSerialNumberService.updateProcessTaskSerialNumberPolicySerialNumberSeedByChannelTypeUuid(uuid, function);
            }
        } else {
            processTaskSerialNumberMapper.insertProcessTaskSerialNumberPolicy(policy);
        }
        return channelTypeVo.getUuid();
    }

    public IValid name() {
        return value -> {
            ChannelTypeVo channelTypeVo = JSON.toJavaObject(value, ChannelTypeVo.class);
            if (channelTypeMapper.checkChannelTypeNameIsRepeat(channelTypeVo) > 0) {
                return new FieldValidResultVo(new ChannelTypeNameRepeatException(channelTypeVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

}
