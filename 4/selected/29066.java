package net.itsite.document.docu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.a.ItSiteUtil;
import net.itsite.document.documentconfig.DocumentConfigMgr;
import net.itsite.document.documentconfig.StorageBean;
import net.itsite.document.utils.ConvertParam;
import net.itsite.document.utils.DocViewerConverter;
import net.itsite.document.utils.FileUtils;
import net.itsite.document.utils.PdfToImageUtil;
import net.itsite.impl.AbstractCatalog;
import net.itsite.utils.DateUtils;
import net.itsite.utils.IOUtils;
import net.itsite.utils.StringsUtils;
import net.itsite.utils.ThreadPool;
import net.itsite.utils.UnFileUtils;
import net.simpleframework.ado.db.ExpressionValue;
import net.simpleframework.ado.db.IQueryEntitySet;
import net.simpleframework.ado.db.ITableEntityManager;
import net.simpleframework.ado.db.SQLValue;
import net.simpleframework.ado.db.event.TableEntityAdapter;
import net.simpleframework.applets.attention.AttentionBean;
import net.simpleframework.applets.attention.AttentionUtils;
import net.simpleframework.applets.tag.TagUtils;
import net.simpleframework.content.EContentStatus;
import net.simpleframework.content.EContentType;
import net.simpleframework.content.component.filepager.FileBean;
import net.simpleframework.content.component.filepager.FilePagerRegistry;
import net.simpleframework.content.component.filepager.FilePagerUtils;
import net.simpleframework.core.ado.IDataObjectQuery;
import net.simpleframework.my.space.MySpaceUtils;
import net.simpleframework.organization.OrgUtils;
import net.simpleframework.organization.account.Account;
import net.simpleframework.organization.account.AccountContext;
import net.simpleframework.organization.account.EAccountStatus;
import net.simpleframework.organization.account.IAccount;
import net.simpleframework.organization.account.IGetAccountAware;
import net.simpleframework.sysmgr.dict.DictUtils;
import net.simpleframework.util.ConvertUtils;
import net.simpleframework.util.HTMLBuilder;
import net.simpleframework.util.HTMLUtils;
import net.simpleframework.util.HTTPUtils;
import net.simpleframework.util.IoUtils;
import net.simpleframework.util.JavascriptUtils;
import net.simpleframework.util.StringUtils;
import net.simpleframework.web.EFunctionModule;
import net.simpleframework.web.WebUtils;
import net.simpleframework.web.page.PageRequestResponse;
import net.simpleframework.web.page.component.AbstractComponentRegistry;
import net.simpleframework.web.page.component.ComponentParameter;
import net.simpleframework.web.page.component.HandleException;
import com.itextpdf.text.pdf.PdfReader;

public final class DocuUtils {

    public static String $data$ = "$data$";

    public static String docuId = "docuId";

    public static String deploy = "/app/docu";

    public static String deployPath;

    public static IDocuApplicationModule applicationModule;

    public static long docuCounter = 0;

    public static String[] languages = { "Java", "C++", "C", "Javascript", "SQL", "XML", "HTML", "CSS", "PHP", "Python", "Groovy", "C#", "Ruby" };

    public static void setDocuCounter(int wsCounter) {
        DocuUtils.docuCounter = wsCounter;
    }

    static void doStatRebuild() {
        final ITableEntityManager tMgr = applicationModule.getDataObjectManager(DocuBean.class);
        final StringBuffer sql1 = new StringBuffer();
        sql1.append("update ").append(DocuApplicationModule.docu_documentshare.getName()).append(" d set remarks=(select count(id) from ");
        sql1.append(DocuApplicationModule.docu_remark.getName()).append(" where documentid=d.id) where d.status=?");
        tMgr.execute(new SQLValue(sql1.toString(), new Object[] { EContentStatus.publish }));
        sql1.setLength(0);
        sql1.append("update ").append(DocuApplicationModule.docu_documentshare.getName()).append(" d set attentions=(select count(id) from ");
        sql1.append(AttentionUtils.getTableEntityManager(AttentionBean.class).getTablename()).append(" where attentionId=d.id and vtype=" + EFunctionModule.docu.ordinal() + ")  where d.status=?");
        tMgr.execute(new SQLValue(sql1.toString(), new Object[] { EContentStatus.publish }));
        sql1.setLength(0);
        sql1.append("update ").append(DocuApplicationModule.docu_user.getName()).append(" d set upFiles=(select count(id) from ");
        sql1.append(DocuApplicationModule.docu_documentshare.getName()).append("  where status=? and userId=d.userId)");
        tMgr.execute(new SQLValue(sql1.toString(), new Object[] { EContentStatus.publish }));
        sql1.setLength(0);
        sql1.append("update ").append(DocuApplicationModule.docu_catalog.getName()).append(" osc set osc.counter=(select count(id) from ");
        sql1.append(DocuApplicationModule.docu_documentshare.getName()).append(" where status=? and catalogId=osc.id and osc.parentid<>0)");
        try {
            tMgr.execute(new SQLValue(sql1.toString(), new Object[] { EContentStatus.publish }));
        } catch (Exception e) {
        }
        sql1.setLength(0);
        sql1.append("update ").append(DocuApplicationModule.docu_catalog.getName()).append(" osc set counter=(select sum(osc1.counter) from ( SELECT counter,parentid from ");
        sql1.append(DocuApplicationModule.docu_catalog.getName()).append(") osc1 where osc1.parentid=osc.id ) where osc.parentid=0");
        try {
            tMgr.execute(new SQLValue(sql1.toString()));
        } catch (Exception e) {
        }
        tMgr.reset();
    }

    public static void addDocuCounter(int value) {
        synchronized (applicationModule) {
            DocuUtils.docuCounter += value;
        }
    }

    public static final int statWebstite() {
        final ITableEntityManager tMgr = applicationModule.getDataObjectManager();
        return tMgr.getCount(new ExpressionValue("status="));
    }

    public static String buildTitle(final PageRequestResponse requestResponse, final String catalogId) {
        final int t = ConvertUtils.toInt(requestResponse.getRequestParameter("t"), 0);
        final int s = ConvertUtils.toInt(requestResponse.getRequestParameter("s"), 0);
        final int od = ConvertUtils.toInt(requestResponse.getRequestParameter("od"), 0);
        final StringBuffer param = new StringBuffer();
        param.append(t).append("-").append(s).append("-").append(od).append("-0");
        final String c = requestResponse.getRequestParameter("c");
        final StringBuffer buf = new StringBuffer();
        buf.append("<a href=\"/docu.html\">所有分类</a>");
        buf.append(HTMLBuilder.NAV);
        final DocuCatalog catalog = applicationModule.getBean(DocuCatalog.class, catalogId);
        if (catalog != null) {
            buildTitle(requestResponse, buf, catalog, param.toString());
            buf.append("<a href='/docu/").append(param).append("-").append(catalog.getId()).append(".html").append("'>");
            buf.append(catalog.getText());
            buf.append("</a>");
        }
        if (StringUtils.hasText(c)) {
            buf.append(HTMLBuilder.NAV);
            buf.append("搜索结果");
        }
        return buf.toString();
    }

    public static void buildTitle(final PageRequestResponse requestResponse, final StringBuffer buf, final DocuCatalog catalog, final String param) {
        final DocuCatalog pCatalog = (DocuCatalog) catalog.parent(applicationModule);
        if (pCatalog != null) {
            buildTitle(requestResponse, buf, pCatalog, param);
            buf.append("<a href='/docu/").append(param).append("-").append(pCatalog.getId()).append(".html").append("'>");
            buf.append(pCatalog.getText());
            buf.append("</a>");
            buf.append(HTMLBuilder.NAV);
        }
    }

    public static String showCode(final PageRequestResponse requestResponse, final DocuBean docuBean) {
        final StringBuffer buf = new StringBuffer();
        final String path = requestResponse.getRequestParameter("path");
        buf.append("<pre class=\"brush: " + docuBean.getLanguage().toLowerCase() + "; gutter: true; html-script: false;\">");
        final StringBuffer html = new StringBuffer();
        final StorageBean storageBean = DocumentConfigMgr.getDocuMgr().getStorageMap().get(docuBean.getPath2());
        if (storageBean != null) {
            final InputStream is = storageBean.getInputStream(path);
            if (is != null) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = br.readLine()) != null) {
                        html.append(line).append("\r\n");
                    }
                } catch (IOException e) {
                } finally {
                    IOUtils.closeIO(br);
                }
            }
        }
        buf.append(HTMLUtils.htmlEscape(html.toString())).append("</pre>");
        return buf.toString();
    }

    public static String buildContent(final PageRequestResponse requestResponse, final DocuBean docuBean) {
        final StringBuffer buf = new StringBuffer();
        final String contextPath = requestResponse.request.getContextPath();
        final String img = "<img src='" + contextPath + "/app/docu/images/loading.gif' alt='loading'>";
        if ("flv".equalsIgnoreCase(docuBean.getExtension()) || "mp4".equalsIgnoreCase(docuBean.getExtension())) {
            buf.append("<div align='center'><div id='docuVideoId' align='center'>" + img + "</div></div>");
            final StringBuffer js = new StringBuffer();
            js.append("var so = new SWFObject(\"" + contextPath + "/app/docu/video/player.swf\",\"docuVideoId\", \"600\", \"400\", \"2\", \"#000000\");");
            js.append("so.addParam(\"allowfullscreen\", \"true\");");
            js.append("so.addParam(\"allowscriptaccess\", \"always\");");
            js.append("so.addParam(\"wmode\", \"opaque\");");
            js.append("so.addParam(\"quality\", \"high\");");
            js.append("so.addParam(\"salign\", \"lt\");");
            js.append("so.addVariable(\"CuPlayerFile\", \"" + contextPath + "/video?docuId=" + docuBean.getId() + "\");");
            js.append("so.addVariable(\"CuPlayerShowImage\", \"false\");");
            js.append("so.addVariable(\"CuPlayerAutoRepeat\", \"false\");");
            js.append("so.addVariable(\"CuPlayerShowControl\", \"true\");");
            js.append("so.addVariable(\"CuPlayerAutoHideControl\", \"false\");");
            js.append("so.addVariable(\"CuPlayerAutoHideTime\", \"6\");");
            js.append("so.addVariable(\"CuPlayerVolume\", \"80\");");
            js.append("so.write(\"docuVideoId\");");
            buf.append(JavascriptUtils.wrapScriptTag(JavascriptUtils.wrapWhenReady(js.toString())));
        } else if ("rar".equalsIgnoreCase(docuBean.getExtension()) || "zip".equalsIgnoreCase(docuBean.getExtension())) {
            buf.append("<div id='docuRarTreeId' align='center'>" + img + "</div>");
        } else if (docuBean.getDocuFunction() == EDocuFunction.code) {
            buf.append("<pre class=\"brush: " + docuBean.getLanguage().toLowerCase() + "; gutter: true; html-script: false;\">");
            final String path1 = DocuUtils.getDatabase(docuBean.getUserId());
            final StorageBean storageBean = DocumentConfigMgr.getDocuMgr().getStorageMap().get(docuBean.getPath2());
            if (storageBean != null) {
                final InputStream is = storageBean.getInputStream(docuBean, path1 + docuBean.getFileName());
                if (is != null) {
                    BufferedReader br = null;
                    try {
                        br = new BufferedReader(new InputStreamReader(is));
                        String line;
                        while ((line = br.readLine()) != null) {
                            buf.append(line).append("\r\n");
                        }
                    } catch (IOException e) {
                    } finally {
                        IOUtils.closeIO(br);
                    }
                }
            }
            buf.append("</pre>");
        } else if (docuBean.getDocuFunction() == EDocuFunction.docu) {
            buf.append("<div id='docuDocuId' align='center'>" + img + "</div><script>$ready(function(){$('docuDocuId').innerHTML=\"<iframe id='docviewframe' scrolling='no' frameborder='0' src='" + contextPath + "/docviewer.html?id=" + docuBean.getId() + "&info=true' height='530px' width='100%'/>\"});</script>");
        } else {
            buf.append(StringUtils.text(docuBean.getContent(), "没有描述信息"));
        }
        return buf.toString();
    }

    public static String catalogView2(final PageRequestResponse requestResponse) {
        final StringBuilder sb = new StringBuilder();
        int catalogId = ConvertUtils.toInt(requestResponse.getRequestParameter("catalogId"), 0);
        if (catalogId == 0) catalogId = ConvertUtils.toInt(requestResponse.getRequestParameter("catalogPId"), 0);
        DocuCatalog csc;
        final IQueryEntitySet<DocuCatalog> qs = applicationModule.queryBean("1=1", DocuCatalog.class);
        final Map<String, Boolean> map = new HashMap<String, Boolean>();
        while ((csc = qs.next()) != null) {
            if (!csc.getParentId().equals2(0)) {
                map.put(csc.getParentId().toString(), true);
            }
        }
        int i = 0;
        sb.append("<div class=\"ctitle");
        sb.append(i++ == 0 ? " ctitle_border_radius\"" : "\" style=\"border-top: 1px solid #ccc;\"").append(">");
        sb.append(buildTitle(requestResponse, String.valueOf(catalogId)));
        sb.append("</div><div style=\"border-top: 1px solid #ccc;\">");
        final int t = ConvertUtils.toInt(requestResponse.getRequestParameter("t"), 0);
        final int s = ConvertUtils.toInt(requestResponse.getRequestParameter("s"), 0);
        final int od = ConvertUtils.toInt(requestResponse.getRequestParameter("od"), 0);
        final IQueryEntitySet<DocuCatalog> qs2 = applicationModule.queryCatalogs(catalogId);
        if (qs2.getCount() > 0) {
            while ((csc = qs2.next()) != null) {
                sb.append("<span class=\"c_forums2\">");
                sb.append("<table style=\"width: 100%; height: 100%;\">");
                sb.append("<tr>");
                sb.append("<td  class=\"c2\">");
                if (map.get(csc.getId().toString()) != null) {
                    sb.append("<a href=\"/docu/").append(StringsUtils.u(t, "-", s, "-", od, "-0-", csc.getId()));
                } else {
                    sb.append("<a href=\"/docu/").append(StringsUtils.u(t, "-", s, "-", od, "-0-", csc.getId()));
                }
                sb.append(".html");
                sb.append("\">").append(csc.getText()).append("</a>");
                sb.append("</td></tr></table></span>");
                final String desc = csc.getDescription();
                if (StringUtils.hasText(desc)) {
                    sb.append("<div style=\"display: none;\">");
                    sb.append(StringUtils.blank(HTMLUtils.convertHtmlLines(desc)));
                    sb.append("</div>");
                }
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    public static final IDataObjectQuery<?> queryDocu(final PageRequestResponse requestResponse, final DocuBean docuBean, final String docu, final String type) {
        if ("lucene".equals(type)) {
            final String title = WebUtils.toLocaleString(requestResponse.getRequestParameter("title"));
            return applicationModule.createLuceneManager(new ComponentParameter(requestResponse.request, requestResponse.response, null)).getLuceneQuery(title);
        }
        final int catalogId = ConvertUtils.toInt(requestResponse.getRequestParameter("catalogId"), 0);
        final ITableEntityManager tMgr = applicationModule.getDataObjectManager();
        final List<Object> ol = new ArrayList<Object>();
        final StringBuffer sql = new StringBuffer();
        final StringBuffer order = new StringBuffer();
        if (!"all".equals(docu)) {
            sql.append("docuFunction=?");
            ol.add(EDocuFunction.whichOne(docu));
        } else {
            sql.append("1=1 ");
        }
        if (catalogId != 0) {
            sql.append(" and catalogId in(").append(AbstractCatalog.Utils.getJoinCatalog(catalogId, DocuUtils.applicationModule, DocuCatalog.class)).append(")");
        }
        if ("userPoint".equals(type)) {
            return OrgUtils.getTableEntityManager(Account.class).query(new ExpressionValue("status=? order by points desc", new Object[] { EAccountStatus.normal }), Account.class);
        } else if ("new".equals(type)) {
            order.append(",createDate desc");
        } else if ("download".equals(type)) {
            order.append(",downCounter desc");
        } else if ("grade".equals(type)) {
            order.append(",totalGrade desc");
        } else if ("view".equals(type)) {
            order.append(",views desc");
        } else if ("recommended".equals(type)) {
            sql.append(" and ttype=?");
            ol.add(EContentType.recommended);
            order.append(",createDate desc");
        } else if ("same".equals(type)) {
            ol.set(0, docuBean.getDocuFunction());
            sql.append(" and catalogId=? and id<>?");
            ol.add(docuBean.getCatalogId());
            ol.add(docuBean.getId());
        } else {
            order.append(",createDate desc");
        }
        sql.append(" and status=?");
        ol.add(EContentStatus.publish);
        sql.append(" order by ttop desc").append(order);
        return tMgr.query(new ExpressionValue(sql.toString(), ol.toArray(new Object[] {})), DocuBean.class);
    }

    public static String wrapOpenLink(final PageRequestResponse requestResponse, final DocuBean docuBean) {
        final StringBuffer buf = new StringBuffer();
        if (docuBean != null) {
            buf.append("<a target=\"_blank\"");
            buf.append(" href=\"" + applicationModule.getViewUrl(docuBean.getId()) + "\"");
            buf.append(">").append(docuBean.getTitle()).append("</a>");
        }
        return buf.toString();
    }

    /**
	 * 获得该用户当前存储目录
	 * 
	 * @param userId
	 * @return
	 */
    public static String getDatabase(final Object userId) {
        return "/" + userId + "/";
    }

    /**
	 * 下载文件
	 * 
	 * @param requestResponse
	 * @throws Exception
	 */
    public static void doDownload(final PageRequestResponse requestResponse) throws Exception {
        if (!ItSiteUtil.isLogin(requestResponse)) {
            return;
        }
        final DocuBean docuBean = DocuUtils.applicationModule.getBean(DocuBean.class, requestResponse.getRequestParameter(DocuUtils.docuId));
        if (docuBean != null) {
            final IAccount account = ItSiteUtil.getLoginAccount(requestResponse);
            final DocuLogBean firstLogBean = DocuUtils.applicationModule.getBeanByExp(DocuLogBean.class, "userId=? and docuId=? order by downDate desc", new Object[] { account.getId(), docuBean.getId() });
            final boolean hasDown = firstLogBean == null ? false : (DateUtils.getDateValue(new Date(), firstLogBean.getDownDate(), Calendar.HOUR) <= 24);
            final boolean isManager = ItSiteUtil.isManage(requestResponse, applicationModule);
            if (!isManager & !hasDown && docuBean.getPoint() > account.getPoints()) {
                return;
            }
            final String path1 = DocuUtils.getDatabase(docuBean.getUserId());
            final StorageBean storageBean = DocumentConfigMgr.getDocuMgr().getStorageMap().get(docuBean.getPath2());
            if (storageBean != null) {
                final InputStream is = storageBean.getInputStream(path1 + docuBean.getFileName());
                if (is != null) {
                    final OutputStream outputStream = HTTPUtils.getFileOutputStream(requestResponse.request, requestResponse.response, docuBean.getFileName(), docuBean.getFileSize());
                    try {
                        IoUtils.copyStream(is, outputStream);
                        docuBean.setDownCounter(docuBean.getDownCounter() + 1);
                        DocuUtils.applicationModule.doUpdate(new Object[] { "downCounter" }, docuBean, new TableEntityAdapter() {

                            @Override
                            public void afterUpdate(ITableEntityManager manager, Object[] objects) {
                                if (!account.getId().equals2(docuBean.getId())) {
                                    if (!isManager && !hasDown) {
                                        ItSiteUtil.switchPoint(requestResponse, docuBean.getPoint(), docuBean.getUserId(), docuBean.getId());
                                    }
                                }
                                final DocuLogBean logBean = new DocuLogBean();
                                logBean.setDocuId(docuBean.getId());
                                logBean.setUserId(account.getId());
                                DocuUtils.applicationModule.doUpdate(logBean);
                                ItSiteUtil.addSpaceLog(account, "下载-文档《" + wrapOpenLink(requestResponse, docuBean) + "》", EFunctionModule.space_log, docuBean.getId());
                                DocuUserBean userBean = DocuUtils.applicationModule.getBeanByExp(DocuUserBean.class, "userId=?", new Object[] { docuBean.getUserId() });
                                if (userBean == null) {
                                    userBean = new DocuUserBean();
                                }
                                userBean.setDownFiles(userBean.getDownFiles() + 1);
                                DocuUtils.applicationModule.doUpdate(userBean);
                                DocuUtils.applicationModule.createLuceneManager(new ComponentParameter(requestResponse.request, requestResponse.response, null)).objects2DocumentsBackground(docuBean);
                            }
                        });
                    } catch (Exception e) {
                    } finally {
                        is.close();
                        outputStream.close();
                    }
                }
            }
        }
    }

    /**
	 * 获得文件路径
	 * 
	 * @return
	 */
    public static String getFileImage(final PageRequestResponse requestResponse, final DocuBean docu) {
        final FileBean fileBean = new FileBean();
        fileBean.setFilename(docu.getFileName());
        fileBean.setFiletype(docu.getExtension());
        return AbstractComponentRegistry.getRegistry(FilePagerRegistry.filePager).getResourceHomePath(requestResponse) + "/images/" + FilePagerUtils.getIconW(fileBean).icon;
    }

    /**
	 * 获得文档图片路径
	 * 
	 * @param requestResponse
	 * @param docuBean
	 * @return
	 */
    public static String getPageImgSrc(final PageRequestResponse requestResponse, final DocuBean docuBean) {
        if (docuBean == null) {
            return "";
        }
        if (docuBean.getDocuFunction() == null) {
            return deploy + "/page/docu.png";
        }
        switch(docuBean.getDocuFunction()) {
            case docu:
                return deploy + "/docu_img_dl.jsp?docuId=" + docuBean.getId() + "&u=" + docuBean.getUserId() + "&d=" + docuBean.getPath2();
            default:
                return deploy + "/page/" + docuBean.getDocuFunction().name() + ".png";
        }
    }

    /**
	 * 图片
	 * 
	 * @param compParameter
	 * @param fileBean
	 */
    public static void doDownloadImg(final PageRequestResponse requestResponse) {
        final StorageBean storageBean = DocumentConfigMgr.getDocuMgr().getStorageMap().get(requestResponse.getRequestParameter("d"));
        if (storageBean != null) {
            final InputStream is = storageBean.getInputStream(getDatabase(requestResponse.getRequestParameter("u")) + requestResponse.getRequestParameter("docuId") + "/0.jpg");
            if (is != null) {
                OutputStream out = null;
                try {
                    IoUtils.copyStream(is, (out = requestResponse.getFileOutputStream("", 0)));
                } catch (final IOException e) {
                    throw HandleException.wrapException(e);
                } finally {
                    IOUtils.closeIO(is);
                    IOUtils.closeIO(out);
                }
            }
        }
    }

    /**
	 * 转换文件
	 */
    public static void convertFile(final String filePath, final DocuBean docuBean) {
        try {
            if (docuBean.getDocuFunction() == EDocuFunction.docu) {
                convertToOnline(filePath, docuBean);
            } else if ("RAR".equalsIgnoreCase(docuBean.getExtension())) {
                if (UnFileUtils.unRar(filePath + docuBean.getId(), filePath + docuBean.getFileName())) docuBean.setSuccess(2); else docuBean.setSuccess(3);
            } else if ("ZIP".equalsIgnoreCase(docuBean.getExtension())) {
                if (UnFileUtils.unZip(filePath + docuBean.getId(), filePath + docuBean.getFileName())) docuBean.setSuccess(2); else docuBean.setSuccess(3);
            } else {
                docuBean.setSuccess(2);
            }
        } catch (Exception e) {
            docuBean.setStatus(EDocuStatus.audit);
        } finally {
            final boolean auto = ConvertUtils.toBoolean(DictUtils.getSysDictByName("auto.auto_v").getText(), false);
            if (docuBean.getSuccess() == 2 && (auto || OrgUtils.isManagerMember(ItSiteUtil.getUserById(docuBean.getUserId())))) {
                docuBean.setStatus(EDocuStatus.publish);
            }
        }
    }

    /**
	 * 在线文件处理
	 * 
	 * @param filePath
	 * @param docuBean
	 */
    private static void convertToOnline(final String filePath, final DocuBean docuBean) throws Exception {
        File source = new File(filePath + File.separator + docuBean.getFileName());
        File dir = new File(filePath + File.separator + docuBean.getId());
        if (!dir.exists()) {
            dir.mkdir();
        }
        File in = source;
        boolean isSpace = false;
        if (source.getName().indexOf(" ") != -1) {
            in = new File(StringUtils.replace(source.getName(), " ", ""));
            try {
                IOUtils.copyFile(source, in);
            } catch (IOException e) {
                e.printStackTrace();
            }
            isSpace = true;
        }
        File finalPdf = null;
        try {
            String outPath = dir.getAbsolutePath();
            final File pdf = DocViewerConverter.toPDF(in, outPath);
            convertToSwf(pdf, outPath, docuBean);
            finalPdf = new File(outPath + File.separator + FileUtils.getFilePrefix(StringUtils.replace(source.getName(), " ", "")) + "_decrypted.pdf");
            if (!finalPdf.exists()) {
                finalPdf = pdf;
            }
            pdfByFirstPageToJpeg(finalPdf, outPath, docuBean);
            if (docuBean.getSuccess() == 2 && dir.listFiles().length < 2) {
                docuBean.setSuccess(3);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (isSpace) {
                IOUtils.delete(in);
            }
        }
    }

    /**
	 * 把上传的文档转化成swf格式
	 * 
	 * @param filePath
	 * @param docuBean
	 */
    private static void convertToSwf(final File pdf, String outPath, final DocuBean docuBean) {
        boolean split = true;
        PdfReader reader = null;
        try {
            reader = new PdfReader(pdf.getAbsolutePath());
            int pages = reader.getNumberOfPages();
            if (pages != 0) {
                docuBean.setFileNum(pages);
                docuBean.setSuccess(2);
            }
            ConvertParam param = new ConvertParam();
            param.pdfFile = pdf;
            param.outPath = outPath;
            int allows = DocuUtils.allowPages(docuBean);
            param.convertPages = String.valueOf(allows);
            if (allows < 10) {
                split = false;
            }
            param.splitPage = split;
            DocViewerConverter.toSwf(param);
        } catch (Exception e) {
            docuBean.setSuccess(3);
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
	 * 提取文档首页作为缩略图
	 * 
	 * @param filePath
	 * @param docuBean
	 */
    private static void pdfByFirstPageToJpeg(final File pdf, String outPath, final DocuBean docuBean) {
        if (pdf == null) {
            return;
        }
        PdfToImageUtil.pdfToImage(pdf, outPath, 0);
    }

    public static String documentNav(final PageRequestResponse requestResponse) {
        final StringBuilder buf = new StringBuilder();
        final IAccount account = ItSiteUtil.getLoginAccount(requestResponse);
        String param = StringUtils.text(requestResponse.getRequestParameter(OrgUtils.um().getUserIdParameterName()));
        final IGetAccountAware accountAware = MySpaceUtils.getAccountAware();
        final boolean isMe = accountAware.isMyAccount(requestResponse);
        if (StringUtils.hasText(param)) {
            param = OrgUtils.um().getUserIdParameterName() + "=" + param;
        }
        buildDocumentNav(buf, true, "myDocuTableAct", param, (isMe ? "我" : "Ta") + "的上传", "myAll", true);
        buildDocumentNav(buf, true, "myUploadAct", param, "上传文档", "myUpload", true);
        if (account == null) {
            return buf.toString();
        }
        if (isMe) {
            buildDocumentNav(buf, false, "myDocuTableNonAct", "docu_status=" + EDocuStatus.edit.name() + "&" + param, "编辑状态", "myNotEdit", true);
            buildDocumentNav(buf, false, "myDocuTableNonAct", "docu_status=" + EDocuStatus.audit.name() + "&" + param, "审核状态", "myNotAudit", true);
            buildDocumentNav(buf, false, "myDownloadAct", "t=4", "我的下载", "myDownload", true);
            buildDocumentNav(buf, false, "myDelLogAct", param, "申请删除日志", "myDelLog", false);
        }
        return buf.toString();
    }

    public static String documentManagerNav(final PageRequestResponse requestResponse) {
        final StringBuilder buf = new StringBuilder();
        final IAccount account = ItSiteUtil.getLoginAccount(requestResponse);
        String param = StringUtils.text(requestResponse.getRequestParameter(OrgUtils.um().getUserIdParameterName()));
        if (StringUtils.hasText(param)) {
            param = OrgUtils.um().getUserIdParameterName() + "=" + param;
        }
        buildDocumentNav(buf, true, "allDocuTableAct", "docu_type=all" + param, "所有文档", "myAll", false);
        if (account == null) {
            return buf.toString();
        }
        final IGetAccountAware accountAware = MySpaceUtils.getAccountAware();
        if (accountAware.isMyAccount(requestResponse)) {
            buf.append("<span style=\"margin: 0px 4px;\">|</span>");
            buildDocumentNav(buf, false, "allDocuTableNonAct", "docu_status=" + EDocuStatus.edit.name() + "&" + param, "编辑状态", "myNotEdit", true);
            buildDocumentNav(buf, false, "allDocuTableNonAct", "docu_status=" + EDocuStatus.audit.name() + "&" + param, "审核状态", "myNotAudit", true);
            buildDocumentNav(buf, false, "docuDownAct", param, "用户下载", "allNotDown", true);
            buildDocumentNav(buf, false, "docuDelLogAct", param, "申请删除日志", "allNotDelLog", false);
        }
        return buf.toString();
    }

    private static void buildDocumentNav(final StringBuilder buf, final boolean arrow, final String action, final String param, final String text, final String name, final boolean nav) {
        buf.append("<a id='").append(name).append("'");
        if (arrow) {
            buf.append("class='a2 nav_arrow'");
        }
        buf.append(" onclick=\"refreshDocu(this);$Actions['" + action + "']('" + param + "');\"");
        buf.append(">").append(text).append("</a>");
        if (nav) buf.append("<span style=\"margin: 0px 4px;\">|</span>");
    }

    public static int pageCount(String path1, String path2, String docuId) {
        final DocuBean docuBean = DocuUtils.applicationModule.getBean(DocuBean.class, docuId);
        if (docuBean != null) return docuBean.getFileNum();
        return 0;
    }

    static final ThreadPool<Runnable> docuPool = new ThreadPool<Runnable>(2);

    /**
	 * 线程转换
	 */
    public static void convertDocuThread(final DocuBean docuBean) {
        docuPool.execute(new Thread() {

            public void run() {
                final StorageBean storageBean = DocumentConfigMgr.getDocuMgr().getStorageMap().get(docuBean.getPath2());
                if (storageBean != null) {
                    storageBean.convertDocument(docuBean);
                    DocuUtils.applicationModule.doUpdate(docuBean, new TableEntityAdapter() {

                        public void afterUpdate(ITableEntityManager manager, Object[] objects) {
                            DocuUtils.publishDocu(docuBean);
                        }

                        ;
                    });
                }
            }
        });
    }

    public static void publishDocu(final DocuBean docuBean) {
        if (docuBean.getStatus() == EDocuStatus.publish) {
            AttentionUtils.insert(docuBean.getUserId(), EFunctionModule.docu, docuBean.getId());
            final IAccount account = ItSiteUtil.getAccountById(docuBean.getUserId());
            ItSiteUtil.addSpaceLog(account, "上传-文档《" + wrapOpenLink(null, docuBean) + "》", EFunctionModule.space_log, docuBean.getId());
            ItSiteUtil.addAccountLog(account.getId(), "上传文档", docuBean.getPoint(), 0, docuBean.getId());
            try {
                AccountContext.update(ItSiteUtil.getAccountById(docuBean.getUserId()), "docu_add", docuBean.getId());
            } catch (Exception e) {
            }
            TagUtils.syncTags(EFunctionModule.docu, docuBean.getCatalogId(), docuBean.getKeyworks(), docuBean.getId());
            DocuUserBean userBean = DocuUtils.applicationModule.getBeanByExp(DocuUserBean.class, "userId=?", new Object[] { docuBean.getUserId() });
            if (userBean == null) {
                userBean = new DocuUserBean();
                userBean.setUserId(docuBean.getUserId());
            }
            userBean.setUpFiles(userBean.getUpFiles() + 1);
            DocuUtils.applicationModule.doUpdate(userBean);
            DocuUtils.addDocuCounter(1);
        }
    }

    public static String searchFilterDocu(final PageRequestResponse requestResponse) {
        final StringBuffer buf = new StringBuffer();
        final String c = WebUtils.toLocaleString(requestResponse.getRequestParameter("c"));
        final String docu_ = StringUtils.text(requestResponse.getRequestParameter("docu"), "all");
        buf.append("<a onclick=\"$IT.A('docuSearchListPaperAct','docu=all&c=" + c + "');\"");
        if ("all".equals(docu_)) {
            buf.append(" class=\"nav_arrow\"");
        }
        buf.append(">全部</a>");
        for (final EDocuFunction docu : EDocuFunction.values()) {
            buf.append("<span style=\"margin: 0px 4px;\">|</span>");
            buf.append("<a id=\"le").append(c).append("\"");
            if (docu.name().equals(docu_)) {
                buf.append(" class=\"nav_arrow\"");
            }
            buf.append(" onclick=\"$IT.A('docuSearchListPaperAct','docu=");
            buf.append(docu.name()).append("&c=").append(c).append("');\">");
            buf.append(docu.toString()).append("</span>");
            buf.append("</a>");
        }
        return buf.toString();
    }

    public static String searchSortDocu(final PageRequestResponse requestResponse) {
        final StringBuffer buf = new StringBuffer();
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("default", "默认");
        map.put("fileSize", "大小");
        map.put("createDate", "时间");
        map.put("point", "积分");
        map.put("downCounter", "下载");
        map.put("totalGrade", "评分");
        int i = 0;
        final String c = WebUtils.toLocaleString(requestResponse.getRequestParameter("c"));
        final String docu = StringUtils.text(requestResponse.getRequestParameter("docu"), "all");
        final String sort_ = StringUtils.text(requestResponse.getRequestParameter("sort"), "default");
        for (final String sort : map.keySet()) {
            buf.append("<a id=\"le").append(c).append("\"");
            if (sort.equals(sort_)) {
                buf.append(" class=\"nav_arrow\"");
            }
            buf.append(" onclick=\"$IT.A('docuSearchListPaperAct','sort=");
            buf.append(sort).append("&c=").append(c).append("&docu=").append(docu).append("');\">");
            buf.append(map.get(sort));
            buf.append("</a>");
            if (i++ != map.size() - 1) {
                buf.append("<span style=\"margin: 0px 4px;\">|</span>");
            }
        }
        return buf.toString();
    }

    public static int allowPages(DocuBean docuBean) {
        int cN = ConvertUtils.toInt(DictUtils.getSysDictByName("pages.pages_v").getText(), 0);
        int readNum = 0;
        int pages = getCanReadPage(docuBean, cN);
        if (cN != 0 && pages != 0) readNum = (pages > cN ? cN : pages); else if (cN == 0) readNum = pages; else if (pages == 0) {
            readNum = (cN > docuBean.getFileNum() ? docuBean.getFileNum() : cN);
        }
        return readNum;
    }

    public static int getCanReadPage(DocuBean docuBean, final int cN) {
        int pages = cN > docuBean.getFileNum() ? docuBean.getFileNum() : cN;
        if (docuBean.getAllowRead() >= 1) {
            return ((int) docuBean.getAllowRead() > pages ? pages : (int) docuBean.getAllowRead());
        }
        return (int) (docuBean.getAllowRead() * pages);
    }
}
