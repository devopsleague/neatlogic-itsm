package codedriver.framework.process.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import codedriver.framework.process.dto.AttributeVo;
import codedriver.framework.process.dto.FormAttributeVo;
import codedriver.framework.process.dto.FormVersionVo;
import codedriver.framework.process.dto.FormVo;

@Component("processFormMapper")
public interface FormMapper {
	public FormVersionVo getActionFormVersionByFormUuid(String formUuid);

	public List<AttributeVo> getAttributeByFormUuid(String formUuid);

	public List<FormVo> searchForm(FormVo formVo);

	public int searchFormCount(FormVo formVo);

	public FormVo getFormByUuid(String formUuid);

	public FormVersionVo getFormVersionByUuid(String formVersionUuid);

	public List<FormVersionVo> getFormVersionByFormUuid(String formUuid);

	public Integer getMaxVersionByFormUuid(String formUuid);

	public int replaceForm(FormVo formVo);

	public int resetFormVersionIsActiveByFormUuid(String formUuid);

	public int updateFormVersion(FormVersionVo formVersionVo);

	public int insertFormVersion(FormVersionVo formVersionVo);

	public int insertFormAttribute(FormAttributeVo formAttributeVo);

	public int deleteFormAttributeByFormUuid(String formUuid);
}
