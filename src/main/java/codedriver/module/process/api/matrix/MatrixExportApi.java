package codedriver.module.process.api.matrix;

import codedriver.framework.apiparam.core.ApiParamType;
import codedriver.framework.attribute.constvalue.AttributeHandler;
import codedriver.framework.process.constvalue.ProcessMatrixType;
import codedriver.framework.process.dao.mapper.MatrixAttributeMapper;
import codedriver.framework.process.dao.mapper.MatrixMapper;
import codedriver.framework.process.dto.ProcessMatrixAttributeVo;
import codedriver.framework.process.dto.ProcessMatrixDataVo;
import codedriver.framework.process.dto.ProcessMatrixVo;
import codedriver.framework.process.exception.process.MatrixNotFoundException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.BinaryStreamApiComponentBase;
import codedriver.module.process.service.MatrixDataService;
import codedriver.module.process.service.MatrixService;
import codedriver.module.process.util.ExcelUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: codedriver
 * @description:
 * @create: 2020-03-26 19:04
 **/
@Service
public class MatrixExportApi extends BinaryStreamApiComponentBase {

    @Autowired
    private MatrixService matrixService;

    @Autowired
    private MatrixAttributeMapper attributeMapper;

    @Autowired
    private MatrixDataService dataService;

    @Autowired
    private MatrixMapper matrixMapper;

    @Override
    public String getToken() {
        return "matrix/export";
    }

    @Override
    public String getName() {
        return "矩阵导出接口";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({ @Param( name = "matrixUuid", desc = "矩阵uuid", type = ApiParamType.STRING, isRequired = true)})
    @Description( desc = "矩阵导出接口")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String matrixUuid = paramObj.getString("matrixUuid");
        ProcessMatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
        if(matrixVo == null) {
        	throw new MatrixNotFoundException(matrixUuid);
        }
        if(ProcessMatrixType.CUSTOM.getValue().equals(matrixVo.getType())) {
        	List<ProcessMatrixAttributeVo> attributeVoList = attributeMapper.getMatrixAttributeByMatrixUuid(matrixUuid);
            if (CollectionUtils.isNotEmpty(attributeVoList)){
                List<String> headerList = new ArrayList<>();
                List<String> columnList = new ArrayList<>();
                List<List<String>> columnSelectValueList = new ArrayList<>();
                headerList.add("uuid");
                columnList.add("uuid");
                columnSelectValueList.add(new ArrayList<>());
                for (ProcessMatrixAttributeVo attributeVo : attributeVoList){
                    headerList.add(attributeVo.getName());
                    columnList.add(attributeVo.getUuid());
                    List<String> selectValueList = new ArrayList<>();
                    decodeDataConfig(attributeVo, selectValueList);
                    columnSelectValueList.add(selectValueList);
                }
                ProcessMatrixDataVo dataVo = new ProcessMatrixDataVo();
                dataVo.setNeedPage(false);
                dataVo.setMatrixUuid(paramObj.getString("matrixUuid"));
                List<Map<String, String>> dataMapList = dataService.searchDynamicTableData(dataVo);
                String fileNameEncode = matrixVo.getName() + ".xls";
                Boolean flag = request.getHeader("User-Agent").indexOf("like Gecko") > 0;
                if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
                    fileNameEncode = URLEncoder.encode(fileNameEncode, "UTF-8");// IE浏览器
                } else {
                    fileNameEncode = new String(fileNameEncode.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
                }
                response.setContentType("application/vnd.ms-excel;charset=utf-8");
                response.setHeader("Content-Disposition", "attachment;fileName=\"" + fileNameEncode + "\"");
                ExcelUtil.exportExcel( headerList, columnList, columnSelectValueList, dataMapList, response.getOutputStream());
            }
        }else {
        	JSONObject dataObj = matrixService.getMatrixExternalData(paramObj.getString("matrixUuid"));
            List<String> headerList = dataObj.getJSONArray("headerList").toJavaList(String.class);
            List<String> columnList = dataObj.getJSONArray("columnList").toJavaList(String.class);
            List<Map<String, String>> dataMapList= (List<Map<String,String>>) dataObj.get("dataMapList");
            String fileNameEncode = matrixVo.getName() + ".xls";
            Boolean flag = request.getHeader("User-Agent").indexOf("like Gecko") > 0;
            if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
                fileNameEncode = URLEncoder.encode(fileNameEncode, "UTF-8");// IE浏览器
            } else {
                fileNameEncode = new String(fileNameEncode.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
            }
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;fileName=\"" + fileNameEncode + "\"");
            ExcelUtil.exportExcel( headerList, columnList, dataMapList, response.getOutputStream());
        }
        
        return null;
    }

    //解析config，抽取属性下拉框值
    private void decodeDataConfig(ProcessMatrixAttributeVo attributeVo, List<String> selectValueList){
        if (StringUtils.isNotBlank(attributeVo.getConfig())){
            String config = attributeVo.getConfig();
            JSONObject configObj = JSONObject.parseObject(config);
            JSONArray dataList = configObj.getJSONArray("dataList");
            if(CollectionUtils.isNotEmpty(dataList)) {
            	for(int i = 0; i < dataList.size(); i++) {
            		JSONObject dataObj = dataList.getJSONObject(i);
            		if(MapUtils.isNotEmpty(dataObj)) {
            			String value = dataObj.getString("value");
            			if(StringUtils.isNotBlank(value)) {
                    		selectValueList.add(value);
            			}
            		}
            	}
            }
//            if (AttributeHandler.SELECT.getValue().equals(configObj.getString("handler"))){
//                if (configObj.containsKey("config")){
//                    JSONArray configArray = configObj.getJSONArray("config");
//                    for (int i = 0; i < configArray.size(); i++){
//                        JSONObject param = configArray.getJSONObject(i);
//                        selectValueList.add(param.getString("value"));
//                    }
//                }
//            }
        }
    }
}
