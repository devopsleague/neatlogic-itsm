package codedriver.module.process.api.channeltype;

import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.dao.mapper.ChannelMapper;
import codedriver.framework.process.dao.mapper.ProcessTaskSerialNumberMapper;
import codedriver.framework.process.dto.ChannelTypeVo;
import codedriver.framework.process.dto.ProcessTaskSerialNumberPolicyVo;
import codedriver.framework.process.exception.channeltype.ChannelTypeNotFoundException;
import codedriver.framework.process.processtaskserialnumberpolicy.core.IProcessTaskSerialNumberPolicyHandler;
import codedriver.framework.process.processtaskserialnumberpolicy.core.ProcessTaskSerialNumberPolicyHandlerFactory;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ChannelTypeGetApi extends PrivateApiComponentBase {

    @Autowired
    private ChannelMapper channelMapper;
    @Autowired
    private ProcessTaskSerialNumberMapper processTaskSerialNumberMapper;

    @Override
    public String getToken() {
        return "process/channeltype/get";
    }

    @Override
    public String getName() {
        return "服务类型信息获取接口";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "uuid", type = ApiParamType.STRING, isRequired = true, desc = "服务类型uuid")})
    @Output({@Param(name = "Return", explode = ChannelTypeVo.class, desc = "服务类型信息")})
    @Description(desc = "服务类型信息获取接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String uuid = jsonObj.getString("uuid");
        ChannelTypeVo channelType = channelMapper.getChannelTypeByUuid(uuid);
        if (channelType == null) {
            throw new ChannelTypeNotFoundException(uuid);
        }
        JSONObject resultObj = (JSONObject)JSON.toJSON(channelType);
        ProcessTaskSerialNumberPolicyVo processTaskSerialNumberPolicyVo =
            processTaskSerialNumberMapper.getProcessTaskSerialNumberPolicyLockByChannelTypeUuid(uuid);
        if (processTaskSerialNumberPolicyVo != null) {
            IProcessTaskSerialNumberPolicyHandler handler =
                ProcessTaskSerialNumberPolicyHandlerFactory.getHandler(processTaskSerialNumberPolicyVo.getHandler());
            JSONObject config = handler.makeupConfig(processTaskSerialNumberPolicyVo.getConfig());
            resultObj.putAll(config);
        }
        return resultObj;
    }

}
