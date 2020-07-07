package codedriver.module.process.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.exception.team.TeamNotFoundException;
import codedriver.framework.process.dao.mapper.CatalogMapper;
import codedriver.framework.process.dao.mapper.ChannelMapper;
import codedriver.framework.process.dto.CatalogVo;
import codedriver.framework.process.dto.ChannelVo;
import codedriver.framework.process.exception.channel.ChannelNotFoundException;

@Service
public class CatalogServiceImpl implements CatalogService {

	@Autowired
	private CatalogMapper catalogMapper;
	
	@Autowired
	private ChannelMapper channelMapper;
	
	@Autowired
	private TeamMapper teamMapper;
	
	@Override
	public boolean checkLeftRightCodeIsExists() {
		int count = catalogMapper.getCatalogCount(new CatalogVo());
		CatalogVo rootCatalog = catalogMapper.getCatalogByUuid(CatalogVo.ROOT_UUID);
		if(rootCatalog == null) {
			throw new TeamNotFoundException(CatalogVo.ROOT_UUID);
		}
		if(Objects.equals(rootCatalog.getLft(), 1) && Objects.equals(rootCatalog.getRht(), count * 2)) {
			return true;
		}
		return false;
	}

	@Override
	public Integer rebuildLeftRightCode(String parentUuid, int parentLft) {
		List<CatalogVo> catalogList = catalogMapper.getCatalogListByParentUuid(parentUuid);
		for(CatalogVo catalog : catalogList) {
			if(catalog.getChildrenCount() == 0) {
				catalogMapper.updateCatalogLeftRightCode(catalog.getUuid(), parentLft + 1, parentLft + 2);
				parentLft += 2;
			}else {
				int lft = parentLft + 1;
				parentLft = rebuildLeftRightCode(catalog.getUuid(), lft);
				catalogMapper.updateCatalogLeftRightCode(catalog.getUuid(), lft, parentLft + 1);
				parentLft += 1;
			}
		}
		return parentLft;
	}

	@Override
	public List<String> getCurrentUserAuthorizedChannelUuidList() {
		List<String> resultList = new ArrayList<>();
		List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
		/** 查出当前用户所有已授权的目录uuid集合  **/
		List<String> currentUserAuthorizedCatalogUuidList = catalogMapper.getAuthorizedCatalogUuidList(UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList(), null);
		if(CollectionUtils.isNotEmpty(currentUserAuthorizedCatalogUuidList)) {
			/** 查出当前用户所有已授权的服务uuid集合  **/
			List<String> currentUserAuthorizedChannelUuidList = channelMapper.getAuthorizedChannelUuidList(UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList(), null);
			if(CollectionUtils.isNotEmpty(currentUserAuthorizedChannelUuidList)) {
				Map<String, CatalogVo> uuidKeyMap = new HashMap<>();
				CatalogVo rootCatalog = catalogMapper.getCatalogByUuid(CatalogVo.ROOT_UUID);
				/** 查出所有目录 **/
				List<CatalogVo> catalogList = catalogMapper.getCatalogListForTree(rootCatalog.getLft(), rootCatalog.getRht());				
				for(CatalogVo catalogVo : catalogList) {
					if(currentUserAuthorizedCatalogUuidList.contains(catalogVo.getUuid())) {
						catalogVo.setAuthority(true);
					}
					uuidKeyMap.put(catalogVo.getUuid(), catalogVo);		
				}
				
				for(CatalogVo catalogVo : catalogList) {
					String parentUuid = catalogVo.getParentUuid();
					CatalogVo parent = uuidKeyMap.get(parentUuid);
					if(parent != null) {
						catalogVo.setParent(parent);
					}				
				}
				
				List<ChannelVo> channelList = channelMapper.getChannelListForTree(1);
				for(ChannelVo channelVo : channelList) {
					if(currentUserAuthorizedChannelUuidList.contains(channelVo.getUuid())) {
						channelVo.setAuthority(true);
					}
					String parentUuid = channelVo.getParentUuid();
					CatalogVo parent = uuidKeyMap.get(parentUuid);
					if(parent != null) {
						channelVo.setParent(parent);
					}
				}
				/** 找出有上报权限的服务uuid集合 **/
				for(ChannelVo channelVo : channelList) {
					if(channelVo.isAuthority()) {
						resultList.add(channelVo.getUuid());
					}
				}
			}
			
		}
		
		return resultList;
	}

	@Override
	public boolean channelIsAuthority(String channelUuid) {
		ChannelVo channel = channelMapper.getChannelByUuid(channelUuid);
		if(channel == null ) {
			throw new ChannelNotFoundException(channelUuid);
		}
		/** 服务状态必须是激活**/
		if(Objects.equals(channel.getIsActive(), 1)) {
			List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid(true));
			/** 查出当前用户所有已授权的服务uuid集合  **/
			List<String> channelUuidList = channelMapper.getAuthorizedChannelUuidList(UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList(), channelUuid);
			/** 服务已授权 **/
			if(channelUuidList.contains(channelUuid)) {
				CatalogVo catalogVo = catalogMapper.getCatalogByUuid(channel.getParentUuid());
				if(catalogVo != null && !CatalogVo.ROOT_UUID.equals(catalogVo.getUuid())) {
					/** 查出当前用户所有已授权的目录uuid集合  **/
					List<String> currentUserAuthorizedCatalogUuidList = catalogMapper.getAuthorizedCatalogUuidList(UserContext.get().getUserUuid(true), teamUuidList, UserContext.get().getRoleUuidList(), null);
					List<CatalogVo> ancestorsAndSelfList = catalogMapper.getAncestorsAndSelfUuidByLftRht(catalogVo.getLft(), catalogVo.getRht());
					for(CatalogVo catalog : ancestorsAndSelfList) {
						if(Objects.equals(catalog.getIsActive(), 0)) {
							return false;
						}else if(!currentUserAuthorizedCatalogUuidList.contains(catalog.getUuid())) {
							return false;
						}
					}
				}
			}
		}
		
		return false;
	}

}
