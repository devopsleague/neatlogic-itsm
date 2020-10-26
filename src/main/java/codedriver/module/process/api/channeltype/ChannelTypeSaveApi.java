package codedriver.module.process.api.channeltype;

import codedriver.framework.process.exception.channeltype.ChannelTypeHasReferenceException;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.dao.mapper.ChannelMapper;
import codedriver.framework.process.dto.ChannelTypeVo;
import codedriver.framework.process.exception.channeltype.ChannelTypeNameRepeatException;
import codedriver.framework.process.exception.priority.PriorityNotFoundException;

import java.util.Objects;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
public class ChannelTypeSaveApi extends PrivateApiComponentBase {

	@Autowired
	private ChannelMapper channelMapper;

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

	@Input({
		@Param(name = "uuid", type = ApiParamType.STRING, desc = "服务类型uuid"),
		@Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "名称"),
		@Param(name = "isActive", type = ApiParamType.ENUM, rule = "0,1", isRequired=true, desc = "状态"),
		@Param(name = "prefix", type = ApiParamType.STRING, isRequired = true, desc = "工单号前缀"),
		@Param(name = "color", type = ApiParamType.STRING, isRequired = true, desc = "颜色"),
		@Param(name = "description", type = ApiParamType.STRING, xss = true, desc = "描述")
	})
	@Output({
		@Param(name = "Return", type = ApiParamType.STRING, desc = "服务类型uuid")
	})
	@Description(desc = "服务类型信息保存接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		ChannelTypeVo channelTypeVo = JSON.parseObject(jsonObj.toJSONString(), new TypeReference<ChannelTypeVo>() {});
		if(channelMapper.checkChannelTypeNameIsRepeat(channelTypeVo) > 0) {
			throw new ChannelTypeNameRepeatException(channelTypeVo.getName());
		}
		
		Integer sort = channelMapper.getChannelTypeMaxSort();
		if(sort == null) {
			sort = 0;
		}
		sort++;
		channelTypeVo.setSort(sort);
		String uuid = jsonObj.getString("uuid");
		if(uuid != null) {
			if(channelMapper.checkChannelTypeIsExists(uuid) == 0) {
				throw new PriorityNotFoundException(uuid);
			}
			if(channelMapper.checkChannelTypeHasReference(uuid) > 0 && Objects.equals(channelTypeVo.getIsActive(),0)){
				throw new ChannelTypeHasReferenceException(channelTypeVo.getName(),"禁用");
			}
			channelMapper.updateChannelTypeByUuid(channelTypeVo);
		}else {
			channelMapper.insertChannelType(channelTypeVo);
		}
		return channelTypeVo.getUuid();
	}

}
