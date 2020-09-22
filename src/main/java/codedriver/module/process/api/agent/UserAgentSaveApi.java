package codedriver.module.process.api.agent;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.UserAgentVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.exception.user.UserAgentIllegalException;
import codedriver.framework.exception.user.UserAgentRepeatException;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@OperationType(type = OperationTypeEnum.CREATE)
public class UserAgentSaveApi extends PrivateApiComponentBase {

	@Autowired
	private UserMapper userMapper;
	
	@Override
	public String getToken() {
		return "user/agent/save";
	}

	@Override
	public String getName() {
		return "保存用户授权代理";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name = "agentUuid", type = ApiParamType.STRING, isRequired = true,desc = "代理人uuid")
	})
	@Output({})
	@Description(desc = "保存用户授权代理")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		UserAgentVo userAgentVo = JSON.parseObject(jsonObj.toJSONString(), new TypeReference<UserAgentVo>() {});
		if(UserContext.get().getUserUuid().equals(userAgentVo.getAgentUuid())){
			throw new UserAgentIllegalException("请不要授权给自己");
		}
		if(userMapper.checkAgentExists(UserContext.get().getUserUuid()) > 0){
			throw new UserAgentRepeatException("请删除当前授权用户再进行新的授权");
		}
		if(userMapper.checkAgentExists(userAgentVo.getAgentUuid()) > 0){
			UserVo agent = userMapper.getUserByUuid(userAgentVo.getAgentUuid());
			throw new UserAgentRepeatException("用户：" + agent.getUserId() + "已存在与其他用户的代理关系");
		}
		userAgentVo.setUserUuid(UserContext.get().getUserUuid());
		userAgentVo.setFunc("processtask");
		userMapper.insertUserAgent(userAgentVo);
		return null;
	}

}
