package leeon.mobile.BBSBrowser.yanxi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import leeon.mobile.BBSBrowser.IDocAction;
import leeon.mobile.BBSBrowser.NetworkException;
import leeon.mobile.BBSBrowser.actions.HttpConfig;
import leeon.mobile.BBSBrowser.models.BoardObject;
import leeon.mobile.BBSBrowser.models.DocObject;
import leeon.mobile.BBSBrowser.utils.HTTPUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

public class BBSYanxiDocAction implements IDocAction {

    @Override
    public List<DocObject> topTenDocList() throws NetworkException {
        return null;
    }

    /**
	 * 处理最后一页的重复数据
	 * @throws NetworkException 
	 */
    List<DocObject> dealFirstPage(List<DocObject> list, int startFromIndex, int total, boolean isTitle) throws NetworkException {
        if (total <= YanxiHttpConfig.BBS_PAGE_SIZE) return list;
        Iterator<DocObject> i = list.iterator();
        int stickyCount = 0;
        while (i.hasNext() && startFromIndex-- > 0) {
            DocObject o = i.next();
            if (o.isSticky()) {
                stickyCount++;
                continue;
            }
            i.remove();
        }
        if (list.size() < YanxiHttpConfig.BBS_PAGE_SIZE && list.size() > 0) {
            List<DocObject> l = boardDoc(list.get(0).getBoard(), isTitle, list.size(), stickyCount);
            l.addAll(list);
            return l;
        }
        return list;
    }

    /**
	 * 计算分页
	 */
    static int calculatePage(int total) {
        return (int) (total / YanxiHttpConfig.BBS_PAGE_SIZE) + (total % YanxiHttpConfig.BBS_PAGE_SIZE > 0 ? 1 : 0);
    }

    @Override
    public List<DocObject> boardDoc(BoardObject board, boolean isTitle) throws NetworkException {
        return boardDoc(board, isTitle, 0, 0);
    }

    @Override
    public List<DocObject> boardDoc(BoardObject board, boolean isTitle, int fetchedTotalCount, int stickyCount) throws NetworkException {
        String url = YanxiHttpConfig.BBS_URL + (isTitle ? YanxiHttpConfig.BBS_DOC_TITLE : YanxiHttpConfig.BBS_DOC) + board.getName();
        if (fetchedTotalCount != 0) {
            int currentFetchedPage = calculatePage(fetchedTotalCount);
            int totalPage = calculatePage(board.getTotal());
            int willFetchedPage = totalPage - currentFetchedPage;
            url += "&" + YanxiHttpConfig.BBS_DOC_PAGE_PARAM_NAME + "=" + willFetchedPage;
        }
        String html = getUrlHtmlContent(url);
        if (fetchedTotalCount != 0) return BBSYanxiBodyParseHelper.parseBoardDoc(board, html, isTitle); else {
            board.setTotal(0);
            return dealFirstPage(BBSYanxiBodyParseHelper.parseBoardDoc(board, html, isTitle), YanxiHttpConfig.BBS_PAGE_SIZE - board.getTotal() % YanxiHttpConfig.BBS_PAGE_SIZE, board.getTotal(), isTitle);
        }
    }

    @Override
    public List<DocObject> docContent(DocObject doc, boolean isTitle) throws NetworkException {
        if (!isTitle) {
            String url = YanxiHttpConfig.BBS_URL + YanxiHttpConfig.BBS_CON + doc.getBoard().getId() + "&" + YanxiHttpConfig.BBS_CON_FILE_PARAM_NAME + "=" + doc.getId();
            String html = getUrlHtmlContent(url);
            return Arrays.asList(BBSYanxiBodyParseHelper.parseDocContent(doc, html));
        } else {
            String url = YanxiHttpConfig.BBS_URL + YanxiHttpConfig.BBS_CON_TITLE + doc.getBoard().getName() + "&" + YanxiHttpConfig.BBS_CON_TITLE_PARAM_NAME + "=" + doc.getId();
            List<DocObject> ret = new ArrayList<DocObject>();
            int pno = 1;
            String url1 = url;
            while (BBSYanxiBodyParseHelper.parseTitleContent(doc, getUrlHtmlContent(url1), ret)) {
                url1 = url + "&" + YanxiHttpConfig.BBS_CON_PAGE_PARAM_NAME + "=" + (++pno);
            }
            for (int i = 0; i < ret.size(); i++) {
                List<DocObject> l = docContent(ret.get(i), false);
                ret.set(i, l.get(0));
            }
            return ret;
        }
    }

    @Override
    public List<DocObject> docContent(DocObject doc, boolean isTitle, String tag) throws NetworkException {
        return null;
    }

    @Override
    public List<DocObject> docContent(DocObject doc, boolean isTitle, DocObject gdoc) throws NetworkException {
        return null;
    }

    static String getUrlHtmlContent(String url) throws NetworkException {
        HttpClient client = HttpConfig.newInstance();
        HttpGet get = new HttpGet(url);
        try {
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            return HTTPUtil.toString(entity, BBSYanxiBodyParseHelper.BBS_CHARSET);
        } catch (Exception e) {
            throw new NetworkException(e);
        }
    }

    /**
	 * @param args
	 * @throws NetworkException 
	 */
    public static void main(String[] args) throws NetworkException {
        BoardObject b = new BoardObject("405", "Sex", "人之初");
        List<DocObject> list = new BBSYanxiDocAction().boardDoc(b, true);
        System.out.println(list);
    }
}
