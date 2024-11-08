package com.bocoon.app.cms.admin.lucene;

import static com.bocoon.app.cms.Constants.TPL_STYLE_LIST;
import static com.bocoon.app.cms.Constants.TPL_SUFFIX;
import static com.bocoon.app.cms.utils.FrontUtils.PARAM_STYLE_LIST;
import static com.bocoon.common.web.Constants.UTF8;
import static com.bocoon.common.web.freemarker.DirectiveUtils.OUT_LIST;
import static freemarker.template.ObjectWrapper.DEFAULT_WRAPPER;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import com.bocoon.app.cms.Constants;
import com.bocoon.app.cms.utils.FrontUtils;
import com.bocoon.common.web.freemarker.DirectiveUtils;
import com.bocoon.common.web.freemarker.ParamsRequiredException;
import com.bocoon.common.web.freemarker.DirectiveUtils.InvokeType;
import com.bocoon.common.web.springmvc.RealPathResolver;
import com.bocoon.entity.cms.main.CmsSite;
import com.bocoon.entity.cms.main.Content;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public class LuceneDirectiveList extends LuceneDirectiveAbstract {

    /**
	 * 模板名称
	 */
    public static final String TPL_NAME = "lucene_list";

    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
        CmsSite site = FrontUtils.getSite(env);
        int first = FrontUtils.getFirst(params);
        int count = FrontUtils.getCount(params);
        String query = getQuery(params);
        Integer siteId = getSiteId(params);
        Integer channelId = getChannelId(params);
        Date startDate = getStartDate(params);
        Date endDate = getEndDate(params);
        List<Content> list;
        try {
            String path = realPathResolver.get(Constants.LUCENE_PATH);
            list = luceneContentSvc.searchList(path, query, siteId, channelId, startDate, endDate, first, count);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Map<String, TemplateModel> paramWrap = new HashMap<String, TemplateModel>(params);
        paramWrap.put(OUT_LIST, DEFAULT_WRAPPER.wrap(list));
        Map<String, TemplateModel> origMap = DirectiveUtils.addParamsToVariable(env, paramWrap);
        InvokeType type = DirectiveUtils.getInvokeType(params);
        String listStyle = DirectiveUtils.getString(PARAM_STYLE_LIST, params);
        if (InvokeType.sysDefined == type) {
            if (StringUtils.isBlank(listStyle)) {
                throw new ParamsRequiredException(PARAM_STYLE_LIST);
            }
            env.include(TPL_STYLE_LIST + listStyle + TPL_SUFFIX, UTF8, true);
        } else if (InvokeType.userDefined == type) {
            if (StringUtils.isBlank(listStyle)) {
                throw new ParamsRequiredException(PARAM_STYLE_LIST);
            }
            FrontUtils.includeTpl(TPL_STYLE_LIST, site, env);
        } else if (InvokeType.custom == type) {
            FrontUtils.includeTpl(TPL_NAME, site, params, env);
        } else if (InvokeType.body == type) {
            body.render(env.getOut());
        } else {
            throw new RuntimeException("invoke type not handled: " + type);
        }
        DirectiveUtils.removeParamsFromVariable(env, paramWrap, origMap);
    }

    @Autowired
    private LuceneContentSvc luceneContentSvc;

    @Autowired
    private RealPathResolver realPathResolver;
}
