package codedriver.module.process.api.catalog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.apiparam.core.ApiParamType;
import codedriver.framework.process.dao.mapper.CatalogMapper;
import codedriver.framework.process.exception.CatalogIllegalParameterException;
import codedriver.framework.process.exception.CatalogNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.process.dto.CatalogVo;

@Service
@Transactional
public class CatalogMoveApi extends ApiComponentBase {

	@Autowired
	private CatalogMapper catalogMapper;
	
	@Override
	public String getToken() {
		return "catalog/move";
	}

	@Override
	public String getName() {
		return "服务目录移动位置接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name = "uuid", type = ApiParamType.STRING, isRequired = true, desc = "被移动的服务目录uuid"),
		@Param(name = "parentUuid", type = ApiParamType.STRING, isRequired = true, desc = "移动后的父级uuid"),
		@Param(name = "nextUuid", type = ApiParamType.STRING, desc = "移动后的下一个兄弟的节点uuid")
	})
	@Description(desc = "服务目录移动位置接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String uuid = jsonObj.getString("uuid");
		if(catalogMapper.checkCatalogIsExists(uuid) == 0) {
			throw new CatalogNotFoundException(uuid);
		}
		String parentUuid = jsonObj.getString("parentUuid");
		if(catalogMapper.checkCatalogIsExists(parentUuid) == 0) {
			throw new CatalogNotFoundException(parentUuid);
		}
		CatalogVo catalogVo = new CatalogVo();
		catalogVo.setParentUuid(parentUuid);		
		Integer sort;
		if(jsonObj.containsKey("nextUuid")) {
			String nextUuid = jsonObj.getString("nextUuid");
			CatalogVo nextCatalog = catalogMapper.getCatalogByUuid(nextUuid);
			if(nextCatalog == null) {
				throw new CatalogNotFoundException(nextUuid);
			}
			if(!parentUuid.equals(nextCatalog.getParentUuid())) {
				throw new CatalogIllegalParameterException("服务目录：'" + nextUuid + "'不是服务目录：'" + parentUuid + "'的子目录");
			}
			sort = nextCatalog.getSort();
			catalogMapper.updateAllNextCatalogSortForMove(sort, parentUuid);
		}else {
			sort = catalogMapper.getMaxSortByParentUuid(parentUuid) + 1;
		}
		catalogVo.setUuid(uuid);
		catalogVo.setSort(sort);
		catalogMapper.updateCatalogForMove(catalogVo);
		return null;
	}

}
