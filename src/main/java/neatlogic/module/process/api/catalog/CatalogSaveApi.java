package neatlogic.module.process.api.catalog;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.AuthorityVo;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.process.auth.CATALOG_MODIFY;
import neatlogic.framework.process.constvalue.CatalogChannelAuthorityAction;
import neatlogic.module.process.dao.mapper.catalog.CatalogMapper;
import neatlogic.framework.process.dto.CatalogVo;
import neatlogic.framework.process.exception.catalog.CatalogNameRepeatException;
import neatlogic.framework.process.exception.catalog.CatalogNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
@AuthAction(action = CATALOG_MODIFY.class)
public class CatalogSaveApi extends PrivateApiComponentBase {

	@Resource
	private CatalogMapper catalogMapper;

	@Override
	public String getToken() {
		return "process/catalog/save";
	}

	@Override
	public String getName() {
		return "nmpac.catalogsaveapi.getname";
	}

	@Override
	public String getConfig() {
		return null;
	}
	
	@Input({
			@Param(name = "uuid", type = ApiParamType.STRING, desc = "common.uuid"),
			@Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, isRequired= true, maxLength = 50, desc = "common.name"),
			@Param(name = "parentUuid", type = ApiParamType.STRING, isRequired= true, desc = "common.parentUuid"),
			@Param(name = "isActive", type = ApiParamType.ENUM, isRequired= true, desc = "common.isactive", rule = "0,1"),
			@Param(name = "icon", type = ApiParamType.STRING, isRequired= false, desc = "common.icon"),
			@Param(name = "color", type = ApiParamType.STRING, isRequired= false, desc = "common.color"),
			@Param(name = "desc", type = ApiParamType.STRING, isRequired= false, desc = "common.description", maxLength = 200, xss = true),
			@Param(name = "reportAuthorityList", type = ApiParamType.JSONARRAY, desc = "common.reportauthoritylist", help = "可多选，格式[\"user#userUuid\",\"team#teamUuid\",\"role#roleUuid\"]"),
			@Param(name = "viewAuthorityList", type = ApiParamType.JSONARRAY, desc = "common.viewauthoritylist", help = "可多选，格式[\"user#userUuid\",\"team#teamUuid\",\"role#roleUuid\"]")
	})
	@Output({
		@Param(name = "uuid", type = ApiParamType.STRING, isRequired= true, desc = "common.uuid")
		})
	@Description(desc = "nmpac.catalogsaveapi.getname")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		CatalogVo catalogVo = jsonObj.toJavaObject(CatalogVo.class);
		//获取父级信息
		String parentUuid = catalogVo.getParentUuid();
		//如果parentUuid为0，则表明其目标父目录为root
		if(!CatalogVo.ROOT_UUID.equals(parentUuid)){
			CatalogVo parentCatalog = catalogMapper.getCatalogByUuid(parentUuid);
			if(parentCatalog == null) {
				throw new CatalogNotFoundException(parentUuid);
			}
		}
		if(catalogMapper.checkCatalogNameIsRepeat(catalogVo) > 0) {
			throw new CatalogNameRepeatException(catalogVo.getName());
		}

		String uuid = jsonObj.getString("uuid");
		if(StringUtils.isNotBlank(uuid)){//修改
			CatalogVo existedCatalog = catalogMapper.getCatalogByUuid(uuid);
			if(existedCatalog == null){
				throw new CatalogNotFoundException(uuid);
			}
			catalogMapper.deleteCatalogAuthorityByCatalogUuid(uuid);
			catalogMapper.updateCatalogByUuid(catalogVo);
		}else{//新增
			//更新插入位置右边的左右编码值
			int lft = LRCodeManager.beforeAddTreeNode("catalog", "uuid", "parent_uuid", parentUuid);
			catalogVo.setLft(lft);
			catalogVo.setRht(lft + 1);
			catalogMapper.insertCatalog(catalogVo);
		}

		List<String> reportAuthorityList = catalogVo.getReportAuthorityList();
		if (CollectionUtils.isNotEmpty(reportAuthorityList)) {
			List<AuthorityVo> authorityVoList = AuthorityVo.getAuthorityVoList(reportAuthorityList, CatalogChannelAuthorityAction.REPORT.getValue());
			for(AuthorityVo authorityVo : authorityVoList) {
				catalogMapper.insertCatalogAuthority(authorityVo, catalogVo.getUuid());
			}
		}
		List<String> viewAuthorityList = catalogVo.getViewAuthorityList();
		if (CollectionUtils.isNotEmpty(viewAuthorityList)) {
			List<AuthorityVo> viewAuthorityVoList = AuthorityVo.getAuthorityVoList(viewAuthorityList, CatalogChannelAuthorityAction.VIEW.getValue());
			for(AuthorityVo authorityVo : viewAuthorityVoList) {
				catalogMapper.insertCatalogAuthority(authorityVo, catalogVo.getUuid());
			}
		}
		return catalogVo.getUuid();
	}

	public IValid name(){
		return value -> {
			CatalogVo catalogVo = JSON.toJavaObject(value,CatalogVo.class);
			if(catalogMapper.checkCatalogNameIsRepeat(catalogVo) > 0) {
				return new FieldValidResultVo(new CatalogNameRepeatException(catalogVo.getName()));
			}
			return new FieldValidResultVo();
		};
	}
//	private Object backup(JSONObject jsonObj) throws Exception {
//		catalogMapper.getCatalogCountOnLock();
//		if(catalogMapper.checkLeftRightCodeIsWrong() > 0) {
//			catalogService.rebuildLeftRightCode();
//		}
//		//构造一个虚拟的root节点
////		CatalogVo rootCatalogVo = catalogService.buildRootCatalog();
//		CatalogVo catalogVo = JSON.toJavaObject(jsonObj, CatalogVo.class);
//		//获取父级信息
//		String parentUuid = catalogVo.getParentUuid();
//		CatalogVo parentCatalog = null;
//		//如果parentUuid为0，则表明其目标父目录为root
//		if(CatalogVo.ROOT_UUID.equals(parentUuid)){
//			parentCatalog = catalogService.buildRootCatalog();
//		}else{
//			parentCatalog = catalogMapper.getCatalogByUuid(parentUuid);
//			if(parentCatalog == null) {
//				throw new CatalogNotFoundException(parentUuid);
//			}
//		}
//		if(catalogMapper.checkCatalogNameIsRepeat(catalogVo) > 0) {
//			throw new CatalogNameRepeatException(catalogVo.getName());
//		}
//
//		String uuid = catalogVo.getUuid();
//		CatalogVo existedCatalog = catalogMapper.getCatalogByUuid(uuid);
//		if(existedCatalog == null) {//新增
//			catalogVo.setUuid(null);
//			catalogVo.setLft(parentCatalog.getRht());
//			catalogVo.setRht(catalogVo.getLft() + 1);
//			//更新插入位置右边的左右编码值
//			catalogMapper.batchUpdateCatalogLeftCode(catalogVo.getLft(), 2);
//			catalogMapper.batchUpdateCatalogRightCode(catalogVo.getLft(), 2);
//		}else {//修改
//			catalogMapper.deleteCatalogAuthorityByCatalogUuid(uuid);
//			catalogVo.setLft(existedCatalog.getLft());
//			catalogVo.setRht(existedCatalog.getRht());
//		}
//
//		catalogMapper.replaceCatalog(catalogVo);
//		List<AuthorityVo> authorityList = catalogVo.getAuthorityVoList();
//		if(CollectionUtils.isNotEmpty(authorityList)) {
//			for(AuthorityVo authorityVo : authorityList) {
//				catalogMapper.insertCatalogAuthority(authorityVo,catalogVo.getUuid());
//			}
//		}
//		return catalogVo.getUuid();
//	}
}
