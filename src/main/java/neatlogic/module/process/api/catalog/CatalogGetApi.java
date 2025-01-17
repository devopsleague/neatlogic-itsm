package neatlogic.module.process.api.catalog;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.AuthorityVo;
import neatlogic.framework.process.auth.PROCESS_BASE;
import neatlogic.framework.process.constvalue.CatalogChannelAuthorityAction;
import neatlogic.framework.process.dto.CatalogVo;
import neatlogic.framework.process.exception.catalog.CatalogNotFoundEditTargetException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.process.dao.mapper.catalog.CatalogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class CatalogGetApi extends PrivateApiComponentBase {

	@Resource
	private CatalogMapper catalogMapper;
	
	@Override
	public String getToken() {
		return "process/catalog/get";
	}

	@Override
	public String getName() {
		return "nmpac.cataloggetapi.getname";
	}

	@Override
	public String getConfig() {
		return null;
	}
	
	@Input({
		@Param(name = "uuid", type = ApiParamType.STRING, isRequired= true, desc = "term.itsm.cataloguuid")
		})
	@Output({
		@Param(explode=CatalogVo.class,desc="term.itsm.cataloginfo")
	})
	@Description(desc = "nmpac.cataloggetapi.getname")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String uuid = jsonObj.getString("uuid");
		CatalogVo catalog = catalogMapper.getCatalogByUuid(uuid);
		if(catalog == null) {
			throw new CatalogNotFoundEditTargetException(uuid);
		}
//		CatalogVo catalogVo = new CatalogVo();
//		catalogVo.setParentUuid(uuid);
//		List<CatalogVo> catalogList = catalogMapper.getCatalogList(catalogVo);
//		ChannelVo channelVo = new ChannelVo();
//		channelVo.setParentUuid(uuid);
//		int count = channelMapper.searchChannelCount(channelVo);
//		catalog.setChildrenCount(count + catalogList.size());
		List<AuthorityVo> authorityVoList = catalogMapper.getCatalogAuthorityListByCatalogUuid(uuid);
//		catalog.setAuthorityVoList(authorityVoList);
		List<AuthorityVo> viewAuthorityVoList = authorityVoList.stream().filter(e -> Objects.equals(e.getAction(), CatalogChannelAuthorityAction.VIEW.getValue())).collect(Collectors.toList());
		catalog.setViewAuthorityList(AuthorityVo.getAuthorityList(viewAuthorityVoList));
		List<AuthorityVo> reportAuthorityVoList = authorityVoList.stream().filter(e -> Objects.equals(e.getAction(), CatalogChannelAuthorityAction.REPORT.getValue())).collect(Collectors.toList());
		catalog.setReportAuthorityList(AuthorityVo.getAuthorityList(reportAuthorityVoList));
		return catalog;
	}

}
