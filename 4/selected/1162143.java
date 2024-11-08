package org.proteinarchitect.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.fmi.bioinformatics.format.SwissProt;
import org.fmi.bioinformatics.format.tools.ConvertFactory;
import org.fmi.bioinformatics.format.tools.PrintFactory;
import org.fmi.bioinformatics.graphics.ImageFactory;
import org.fmi.bioinformatics.graphics.map.ImageMap;
import org.proteinarchitect.core.JobResources;
import org.proteinarchitect.util.ContextUtil;
import org.proteinarchitect.util.ResultAccess;
import atv.Tree;

/**
 * Servlet implementation class for Servlet: ImageLoader
 * 
 */
public class ImageLoader extends AbstractLoader {

    private static final String PAGE_FORM_ACTION = ".";

    private static final long serialVersionUID = -5175362163111447466L;

    private static final String PREAMBLE_ID = "id_";

    public static final String FASTA = "fasta";

    public static final String TREE = "nhx";

    public static final String SWISS = "dat";

    public static final String IMAGE = "png";

    public static final String HTML = "html";

    public ImageLoader() {
        super();
    }

    public void destroy() {
        super.destroy();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    private static enum RequestType {

        HTML, PNG, TREE, FASTA, SWISSPROT
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        getLog().info("Process request " + pathInfo);
        if (null != pathInfo) {
            String pathId = getPathId(pathInfo);
            JobResources res = ContextUtil.getJobResource(pathId);
            if (null != res) {
                RequestType requType = getRequestType(request);
                ResultAccess access = new ResultAccess(res);
                Collection<Long> uIdColl = res.getUniqIds();
                boolean isFiltered = false;
                {
                    List<String> postSeqIds = getSeqList(request);
                    if (!postSeqIds.isEmpty()) {
                        isFiltered = true;
                        uIdColl = access.loadIds(postSeqIds);
                    }
                }
                try {
                    if ((requType.equals(RequestType.FASTA)) || (requType.equals(RequestType.SWISSPROT))) {
                        OutputStreamWriter out = null;
                        out = new OutputStreamWriter(response.getOutputStream());
                        for (Long uid : uIdColl) {
                            if (requType.equals(RequestType.FASTA)) {
                                SwissProt sw = access.getSwissprotEntry(uid);
                                if (null != sw) {
                                    PrintFactory.instance().print(ConvertFactory.instance().SwissProt2fasta(sw), out);
                                } else {
                                    System.err.println("Not able to read Swissprot entry " + uid + " in project " + res.getBaseDir());
                                }
                            } else if (requType.equals(RequestType.SWISSPROT)) {
                                File swissFile = res.getSwissprotFile(uid);
                                if (swissFile.exists()) {
                                    InputStream in = null;
                                    try {
                                        in = new FileInputStream(swissFile);
                                        IOUtils.copy(in, out);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        System.err.println("Problems with reading file to output stream " + swissFile);
                                    } finally {
                                        IOUtils.closeQuietly(in);
                                    }
                                } else {
                                    System.err.println("Swissprot file does not exist: " + swissFile);
                                }
                            }
                        }
                        out.flush();
                    } else {
                        if (uIdColl.size() <= 2) {
                            isFiltered = false;
                            uIdColl = res.getUniqIds();
                        }
                        Tree tree = access.getTreeByUniquId(uIdColl);
                        if (requType.equals(RequestType.TREE)) {
                            response.getWriter().write(tree.toNewHampshireX());
                        } else if (requType.equals(RequestType.PNG)) {
                            List<SwissProt> sp = access.getSwissprotEntriesByUniquId(uIdColl);
                            ImageMap map = ImageFactory.instance().createProteinCard(sp, tree, true, res);
                            response.setContentType("image/png");
                            response.addHeader("Content-Disposition", "filename=ProteinCards.png");
                            ImageFactory.instance().printPNG(map.getImage(), response.getOutputStream());
                            response.getOutputStream().flush();
                        } else if (requType.equals(RequestType.HTML)) {
                            List<SwissProt> sp = access.getSwissprotEntriesByUniquId(uIdColl);
                            createHtml(res, access, tree, request, response, sp, isFiltered);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    getLog().error("Problem with Request: " + pathInfo + "; type " + requType, e);
                }
            } else {
                getLog().error("Resource is null: " + pathId + "; path " + pathInfo);
            }
        } else {
            getLog().error("PathInfo is null!!!");
        }
    }

    private RequestType getExtension(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        String[] pathArr = pathInfo.split("[.]");
        RequestType type = null;
        if (pathArr.length > 1) {
            String tmp = pathArr[1];
            type = getTypeById(tmp);
        }
        return type;
    }

    private String getPathId(String pathInfo) {
        String pathId = pathInfo.replace("/", "");
        String[] pathArr = pathId.split("[.]");
        pathId = pathArr[0];
        pathId = pathId.replaceAll("[\\D]", "");
        return pathId;
    }

    private RequestType getTypeById(String id) {
        if (id.equalsIgnoreCase(HTML)) {
            return RequestType.HTML;
        } else if (id.equalsIgnoreCase(FASTA)) {
            return RequestType.FASTA;
        } else if (id.equalsIgnoreCase(IMAGE)) {
            return RequestType.PNG;
        } else if (id.equalsIgnoreCase(TREE)) {
            return RequestType.TREE;
        } else if (id.equalsIgnoreCase(SWISS)) {
            return RequestType.SWISSPROT;
        } else {
            return null;
        }
    }

    private RequestType getSubmitType(HttpServletRequest request) {
        RequestType requType = null;
        Set<String> keySet = request.getParameterMap().keySet();
        for (String tmpId : keySet) {
            if (tmpId.startsWith("_")) {
                tmpId = tmpId.substring(1);
                requType = getTypeById(tmpId);
                if (null != requType) {
                    break;
                }
            }
        }
        return requType;
    }

    private RequestType getRequestType(HttpServletRequest request) {
        RequestType ext = getExtension(request);
        if (null == ext) {
            ext = getSubmitType(request);
        }
        return null == ext ? RequestType.HTML : ext;
    }

    /**
	 * build ID list from parameters - look for preamble
	 * @param request
	 * @return
	 */
    private List<String> getSeqList(HttpServletRequest request) {
        List<String> ids = new ArrayList<String>();
        Set<String> keySet = request.getParameterMap().keySet();
        for (String tmpId : keySet) {
            if (hasPreamble(tmpId)) {
                ids.add(unmaskPreamble(tmpId));
            }
        }
        return ids;
    }

    private boolean hasPreamble(String id) {
        if (null != id && id.startsWith(PREAMBLE_ID)) {
            return true;
        } else {
            return false;
        }
    }

    private String maskWithPreamble(String id) {
        return PREAMBLE_ID + id.trim();
    }

    private String unmaskPreamble(String id) {
        return id.trim().replaceFirst(PREAMBLE_ID, "");
    }

    private void createHtml(JobResources res, ResultAccess access, Tree tree, HttpServletRequest request, HttpServletResponse response, List<SwissProt> spList, boolean isFiltered) throws Exception {
        System.out.println("Html map generation ... ");
        StringBuffer getImgQuery = new StringBuffer();
        getImgQuery.append(PAGE_FORM_ACTION).append("/").append(res.getBaseDir().getName()).append(".").append(IMAGE);
        if (isFiltered) {
            getImgQuery.append("?");
            boolean isFirst = true;
            for (SwissProt spEntry : spList) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    getImgQuery.append("&");
                }
                getImgQuery.append(maskWithPreamble(spEntry.getId().getEntry_name()) + "=id");
            }
        }
        ImageMap map = ImageFactory.instance().createProteinCard(spList, tree, false, res);
        if (res.hasMasterId()) {
            Long mId = res.getMasterId();
            SwissProt mSp = access.getSwissprotEntry(mId);
            String masterId = mSp.getId().getEntry_name();
            map.setQueryId(masterId);
        }
        response.setContentType("text/html; charset=iso-8859-15");
        OutputStreamWriter out = new OutputStreamWriter(response.getOutputStream());
        map.printHtml(out, null, getImgQuery.toString(), res.getBaseDir().getName());
        out.flush();
    }

    public void init() throws ServletException {
        super.init();
    }
}
