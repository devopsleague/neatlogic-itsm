package codedriver.module.process.api.score;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.process.dao.mapper.score.ScoreTemplateMapper;
import codedriver.framework.process.exception.score.ScoreTemplateNotFoundException;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
@OperationType(type = OperationTypeEnum.DELETE)
public class ScoreTemplateDeleteApi extends ApiComponentBase {

    @Autowired
    private ScoreTemplateMapper scoreTemplateMapper;

    @Override
    public String getToken() {
        return "score/template/delete";
    }

    @Override
    public String getName() {
        return "删除评分模版";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({ @Param( name = "id", type = ApiParamType.LONG, desc = "评分模版ID" ,isRequired = true)
    })
    @Output({})
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");

        if(scoreTemplateMapper.checkScoreTemplateExistsById(id) == null){
            throw new ScoreTemplateNotFoundException(id);
        }
        scoreTemplateMapper.deleteScoreTemplate(id);
        return null;
    }
}
