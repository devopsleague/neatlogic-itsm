package codedriver.module.process.api.channeltype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.apiparam.core.ApiParamType;
import codedriver.framework.process.dao.mapper.ChannelMapper;
import codedriver.framework.process.dto.ChannelTypeVo;
import codedriver.framework.process.exception.channeltype.ChannelTypeNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;

@Service
public class ChannelTypeGetApi extends ApiComponentBase {

	@Autowired
	private ChannelMapper channelMapper;

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

	@Input({
		@Param(name = "uuid", type = ApiParamType.STRING, isRequired = true, desc = "服务类型uuid")
	})
	@Output({
		@Param(name = "Return", explode = ChannelTypeVo.class, desc = "服务类型信息")
	})
	@Description(desc = "服务类型信息获取接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String uuid = jsonObj.getString("uuid");
		ChannelTypeVo channelType = channelMapper.getChannelTypeByUuid(uuid);
		if(channelType == null) {
			throw new ChannelTypeNotFoundException(uuid);
		}
		return channelType;
	}

}