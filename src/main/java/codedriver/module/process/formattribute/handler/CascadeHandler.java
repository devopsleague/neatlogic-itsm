package codedriver.module.process.formattribute.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.process.constvalue.ProcessFormHandler;
import codedriver.framework.process.dto.AttributeDataVo;
import codedriver.framework.process.dto.ProcessMatrixColumnVo;
import codedriver.framework.process.exception.form.AttributeValidException;
import codedriver.framework.process.formattribute.core.IFormAttributeHandler;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentFactory;
import codedriver.module.process.api.matrix.MatrixColumnDataSearchForSelectNewApi;

@Component
public class CascadeHandler implements IFormAttributeHandler {

	@Override
	public String getType() {
		return ProcessFormHandler.FORMCASCADELIST.getHandler();
	}

	@Override
	public boolean valid(AttributeDataVo attributeDataVo, JSONObject configObj) throws AttributeValidException {
		return false;
	}

	@Override
	public Object valueConversionText(AttributeDataVo attributeDataVo, JSONObject configObj) {
		Object dataObj = attributeDataVo.getDataObj();
		if(dataObj != null) {
			List<String> textList = new ArrayList<>();
			List<String> valueList = JSON.parseArray(JSON.toJSONString(dataObj), String.class);
			String dataSource = configObj.getString("dataSource");
			if("static".equals(dataSource)) {
				if(valueList.size() > 0) {
					JSONArray dataList = configObj.getJSONArray("dataList");
					for(int i = 0; i < dataList.size(); i++) {
						JSONObject firstObj = dataList.getJSONObject(i);
						if(Objects.equals(firstObj.getString("value"), valueList.get(0))) {
							textList.add(firstObj.getString("text"));
							if(valueList.size() > 1) {
								JSONArray secondChildren = firstObj.getJSONArray("children");
								for(int j = 0; j < secondChildren.size(); j++) {
									JSONObject secondObj = secondChildren.getJSONObject(j);
									if(Objects.equals(secondObj.getString("value"), valueList.get(1))) {
										textList.add(secondObj.getString("text"));
										if(valueList.size() > 2) {
											JSONArray thirdChildren = secondObj.getJSONArray("children");
											for(int k = 0; k < thirdChildren.size(); k++) {
												JSONObject thirdObj = thirdChildren.getJSONObject(k);
												if(Objects.equals(thirdObj.getString("value"), valueList.get(2))) {
													textList.add(secondObj.getString("text"));
												}
											}
										}
									}
								}
							}
						}
					}
				}
				
			}else {//其他，如动态数据源
				String matrixUuid = configObj.getString("matrixUuid");
				List<ValueTextVo> mappingList = JSON.parseArray(JSON.toJSONString(configObj.getJSONArray("mapping")), ValueTextVo.class);
				if(StringUtils.isNotBlank(matrixUuid) && CollectionUtils.isNotEmpty(valueList) && CollectionUtils.isNotEmpty(mappingList)) {
					MatrixColumnDataSearchForSelectNewApi restComponent = (MatrixColumnDataSearchForSelectNewApi)PrivateApiComponentFactory.getInstance(MatrixColumnDataSearchForSelectNewApi.class.getName());
					if (restComponent != null) {
						if(valueList.size() > 0 && mappingList.size() > 0) {
							List<ProcessMatrixColumnVo> sourceColumnList = new ArrayList<>();
							textList.add(getText(matrixUuid, mappingList.get(0), valueList.get(0), sourceColumnList, restComponent));
							if(valueList.size() > 1 && mappingList.size() > 1) {
								textList.add(getText(matrixUuid, mappingList.get(1), valueList.get(1), sourceColumnList, restComponent));
								if(valueList.size() > 2 && mappingList.size() > 2) {
									textList.add(getText(matrixUuid, mappingList.get(2), valueList.get(2), sourceColumnList, restComponent));
								}
							}
						}
					}
				}
			}
			return textList;
		}
		return dataObj;
	}

	private String getText(String matrixUuid, ValueTextVo mapping, String value, List<ProcessMatrixColumnVo> sourceColumnList, MatrixColumnDataSearchForSelectNewApi restComponent) {
	    if(StringUtils.isBlank(value)) {
	        return value;
	    }
		String[] split = value.split(IFormAttributeHandler.SELECT_COMPOSE_JOINER);
		try {
			JSONObject paramObj = new JSONObject();
			paramObj.put("matrixUuid", matrixUuid);
			List<String> columnList = new ArrayList<>();
			columnList.add(mapping.getValue());
			columnList.add(mapping.getText());
			paramObj.put("columnList", columnList);			
			sourceColumnList.add(new ProcessMatrixColumnVo(mapping.getValue(), split[0]));
			sourceColumnList.add(new ProcessMatrixColumnVo(mapping.getText(), split[1]));
			paramObj.put("sourceColumnList", sourceColumnList);
			JSONObject resultObj = (JSONObject) restComponent.myDoService(paramObj);
			JSONArray columnDataList = resultObj.getJSONArray("columnDataList");
			for(int i = 0; i < columnDataList.size(); i++) {
				JSONObject firstObj = columnDataList.getJSONObject(i);
				JSONObject textObj = firstObj.getJSONObject(mapping.getText());
				if(Objects.equals(textObj.getString("value"), split[1])) {
					return textObj.getString("text");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return split[1];
	}
}
