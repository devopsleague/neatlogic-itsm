package codedriver.module.process.api.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.process.dao.mapper.CatalogMapper;
import codedriver.framework.process.dao.mapper.ChannelMapper;
import codedriver.framework.process.dto.CatalogVo;
import codedriver.framework.process.dto.ChannelVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.process.service.CatalogService;

@Service
public class CatalogChannelTreeSearchApi extends ApiComponentBase {

	@Autowired
	private CatalogService catalogService;
	
	@Autowired
	private CatalogMapper catalogMapper;
	
	@Autowired
	private ChannelMapper channelMapper;
	
	@Override
	public String getToken() {
		return "process/catalog/channel/tree/search";
	}

	@Override
	public String getName() {
		return "服务目录及通道树查询接口";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({})
	@Output({
		@Param(name="Return",explode=CatalogVo[].class,desc="服务目录及通道树")
	})
	@Description(desc = "服务目录及通道树查询接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {				
		
		Map<String, CatalogVo> uuidKeyMap = new HashMap<>();
		if(!catalogService.checkLeftRightCodeIsExists()) {
//			catalogMapper.getCatalogLockByUuid(CatalogVo.ROOT_UUID);
			catalogService.rebuildLeftRightCode(CatalogVo.ROOT_PARENTUUID, 0);
		}
//		CatalogVo rootCatalog = catalogMapper.getCatalogByUuid(CatalogVo.ROOT_UUID);
		CatalogVo rootCatalog = catalogService.buildRootCatalog();
		List<CatalogVo> catalogList = catalogMapper.getCatalogListForTree(rootCatalog.getLft(), rootCatalog.getRht());
		if(CollectionUtils.isNotEmpty(catalogList)) {
			for(CatalogVo catalogVo : catalogList) {
				uuidKeyMap.put(catalogVo.getUuid(), catalogVo);			
			}
			for(CatalogVo catalogVo : catalogList) {
				String parentUuid = catalogVo.getParentUuid();
				CatalogVo parent = uuidKeyMap.get(parentUuid);
				if(parent != null) {
					catalogVo.setParent(parent);
				}				
			}
		}
		
		List<ChannelVo> channelList = channelMapper.getChannelListForTree(null);
		if(CollectionUtils.isNotEmpty(channelList)) {
			for(ChannelVo channelVo : channelList) {
				String parentUuid = channelVo.getParentUuid();
				CatalogVo parent = uuidKeyMap.get(parentUuid);
				if(parent != null) {
					channelVo.setParent(parent);
				}
			}
		}
		
		CatalogVo root = uuidKeyMap.get(CatalogVo.ROOT_UUID);
		if(root != null) {
			return root.getChildren();
		}
		return new ArrayList<>();
	}
}
