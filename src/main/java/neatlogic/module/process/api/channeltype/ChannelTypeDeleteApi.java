package neatlogic.module.process.api.channeltype;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.process.auth.CHANNELTYPE_MODIFY;
import neatlogic.framework.process.dto.ChannelTypeVo;
import neatlogic.framework.process.exception.channeltype.ChannelTypeHasReferenceException;
import neatlogic.framework.process.exception.channeltype.ChannelTypeNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.process.dao.mapper.catalog.ChannelTypeMapper;
import neatlogic.module.process.dao.mapper.processtask.ProcessTaskSerialNumberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.DELETE)
@AuthAction(action = CHANNELTYPE_MODIFY.class)
public class ChannelTypeDeleteApi extends PrivateApiComponentBase {

    @Resource
    private ChannelTypeMapper channelTypeMapper;

    @Resource
    private ProcessTaskSerialNumberMapper processTaskSerialNumberMapper;

    @Override
    public String getToken() {
        return "process/channeltype/delete";
    }

    @Override
    public String getName() {
        return "服务类型信息删除";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "uuid", type = ApiParamType.STRING, isRequired = true, desc = "服务类型uuid")})
    @Output({})
    @Description(desc = "服务类型信息删除")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String uuid = jsonObj.getString("uuid");
        if (channelTypeMapper.checkChannelTypeIsExists(uuid) == 0) {
            throw new ChannelTypeNotFoundException(uuid);
        }
        if (channelTypeMapper.checkChannelTypeHasReference(uuid) > 0) {
            ChannelTypeVo type = channelTypeMapper.getChannelTypeByUuid(uuid);
            throw new ChannelTypeHasReferenceException(type.getName(), "删除");
        }
        channelTypeMapper.deleteChannelTypeByUuid(uuid);
        processTaskSerialNumberMapper.deleteProcessTaskSerialNumberPolicyByChannelTypeUuid(uuid);
        return null;
    }

}
