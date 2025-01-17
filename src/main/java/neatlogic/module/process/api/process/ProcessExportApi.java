package neatlogic.module.process.api.process;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.process.auth.PROCESS_BASE;
import neatlogic.framework.process.dto.ProcessVo;
import neatlogic.framework.process.exception.process.ProcessNotFoundException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.process.dao.mapper.process.ProcessMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@AuthAction(action = PROCESS_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ProcessExportApi extends PrivateBinaryStreamApiComponentBase {
	
	@Resource
	private ProcessMapper processMapper;
	
	@Override
	public String getToken() {
		return "process/export";
	}

	@Override
	public String getName() {
		return "流程导出";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name = "uuid", type = ApiParamType.STRING, isRequired = true, desc = "流程uuid")
	})
	@Description(desc = "流程导出")
	@Override
	public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String uuid = paramObj.getString("uuid");
		if(processMapper.checkProcessIsExists(uuid) == 0) {
			throw new ProcessNotFoundException(uuid);
		}	
		ProcessVo processVo = processMapper.getProcessByUuid(uuid);
		//设置导出文件名
		String fileNameEncode = processVo.getName() + ".process";
		Boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
		if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
			fileNameEncode = URLEncoder.encode(fileNameEncode, "UTF-8");// IE浏览器
		} else {
			fileNameEncode = new String(fileNameEncode.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
		}
		response.setContentType("application/x-msdownload");
		
		response.setHeader("Content-Disposition", " attachment; filename=\"" + fileNameEncode + "\"");
		//获取序列化字节数组
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(processVo);
		ServletOutputStream os = null;
		os = response.getOutputStream();
		IOUtils.write(baos.toByteArray(), os);
		if (os != null) {
			os.flush();
			os.close();
		}
		if(oos != null) {
			oos.close();
		}
		if(baos != null) {
			baos.close();
		}
		return null;
	}

}
