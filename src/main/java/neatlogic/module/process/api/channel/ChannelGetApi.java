package neatlogic.module.process.api.channel;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.AuthorityVo;
import neatlogic.framework.process.auth.PROCESS_BASE;
import neatlogic.framework.process.constvalue.CatalogChannelAuthorityAction;
import neatlogic.framework.process.dto.ChannelPriorityVo;
import neatlogic.framework.process.dto.ChannelVo;
import neatlogic.framework.process.exception.channel.ChannelNotFoundEditTargetException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.process.dao.mapper.catalog.ChannelMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ChannelGetApi extends PrivateApiComponentBase {

	@Resource
	private ChannelMapper channelMapper;

	@Override
	public String getToken() {
		return "process/channel/get";
	}

	@Override
	public String getName() {
		return "nmpac.channelgetapi.getname";
	}

	@Override
	public String getConfig() {
		return null;
	}
	
	@Input({
		@Param(name = "uuid", type = ApiParamType.STRING, isRequired= true, desc = "term.itsm.channeluuid")
		})
	@Output({
		@Param(explode=ChannelVo.class,desc="term.itsm.channelinfo")
	})
	@Description(desc = "nmpac.channelgetapi.getname")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String uuid = jsonObj.getString("uuid");
		ChannelVo channelVo = channelMapper.getChannelByUuid(uuid);
		if(channelVo == null) {
			throw new ChannelNotFoundEditTargetException(uuid);
		}
		ChannelVo channel = new ChannelVo(channelVo);
		String processUuid = channelMapper.getProcessUuidByChannelUuid(uuid);
		channel.setProcessUuid(processUuid);
		String worktimeUuid = channelMapper.getWorktimeUuidByChannelUuid(uuid);
		channel.setWorktimeUuid(worktimeUuid);
		if (Objects.equals(channelVo.getIsActivePriority(), 1)) {
			List<String> priorityUuidList = new ArrayList<>();
			List<ChannelPriorityVo> channelPriorityList = channelMapper.getChannelPriorityListByChannelUuid(uuid);
			for(ChannelPriorityVo channelPriority : channelPriorityList) {
				priorityUuidList.add(channelPriority.getPriorityUuid());
				if(Objects.equals(channelPriority.getIsDefault(), 1)) {
					channel.setDefaultPriorityUuid(channelPriority.getPriorityUuid());
				}
			}
			channel.setPriorityUuidList(priorityUuidList);
		}

		List<AuthorityVo> authorityVoList = channelMapper.getChannelAuthorityListByChannelUuid(uuid);
		List<AuthorityVo> viewAuthorityVoList = authorityVoList.stream().filter(e -> Objects.equals(e.getAction(), CatalogChannelAuthorityAction.VIEW.getValue())).collect(Collectors.toList());
		channel.setViewAuthorityList(AuthorityVo.getAuthorityList(viewAuthorityVoList));
		List<AuthorityVo> reportAuthorityVoList = authorityVoList.stream().filter(e -> Objects.equals(e.getAction(), CatalogChannelAuthorityAction.REPORT.getValue())).collect(Collectors.toList());
		channel.setReportAuthorityList(AuthorityVo.getAuthorityList(reportAuthorityVoList));
		if(channelMapper.checkChannelIsFavorite(UserContext.get().getUserUuid(true), uuid) == 0) {
		    channel.setIsFavorite(0);
		}else {
		    channel.setIsFavorite(1);
		}
		return channel;
	}

}
