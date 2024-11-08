package ces.platform.infoplat.taglib;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import ces.coral.log.Logger;
import ces.platform.infoplat.core.Channel;
import ces.platform.infoplat.core.tree.TreeNode;

/**
 * <b>�ļ���:</b>.java<br>
 * <b>��������:</b><br>
 * <b>��Ȩ����:</b>�Ϻ�������Ϣ��չ���޹�˾(CES)2003
 * @author ֣��ǿ
 * @version 0.1.2004.0630
 * 
 * Created on 2004-4-14
 *
*/
public class DSTempRefManageTag extends BodyTagSupport {

    public static final String IP_DSTRMAP_KEYWORD = "ip_dstrMap";

    public static final String IP_DSMAP_KEYWORD = "ip_dsMap";

    private Channel channel = null;

    Logger log = new Logger(this.getClass());

    private String channelPath = null;

    public int doStartTag() throws JspException {
        channelPath = pageContext.getRequest().getParameter("channelPath");
        if (channelPath == null) {
            log.error("ͨ��request�õ���pathΪnull");
            return SKIP_PAGE;
        }
        HashMap dstrMap = new HashMap();
        HashMap dsMap = new HashMap();
        pageContext.setAttribute(IP_DSTRMAP_KEYWORD, dstrMap);
        try {
            channel = (Channel) Channel.getInstance(channelPath);
            TreeNode[] dsList = channel.getList();
            if (dsList != null) {
                for (int i = 0; i < dsList.length; i++) {
                    dsMap.put(dsList[i].getPath().trim(), dsList[i]);
                }
            }
        } catch (Exception e) {
            log.error("���Ƶ��" + channelPath + "�µ����Դ�б�ʱ����", e);
            return SKIP_BODY;
        }
        pageContext.setAttribute(IP_DSMAP_KEYWORD, dsMap);
        return EVAL_BODY_TAG;
    }

    /**
	 * ��ȡ��ǩ��body����
	 * @return SKIP_BODY = 0
	 * @throws JspException
	 */
    public int doAfterBody() throws JspException {
        BodyContent bodycontent = getBodyContent();
        if (bodycontent != null) {
            this.setBodyContent(bodycontent);
        }
        return EVAL_BODY_TAG;
    }

    public int doEndTag() throws JspException {
        HashMap dsMap = (HashMap) pageContext.getAttribute(IP_DSMAP_KEYWORD);
        if (dsMap != null && dsMap.size() > 0) {
            Iterator values = dsMap.values().iterator();
            while (values.hasNext()) {
                Channel dsInChannel = null;
                try {
                    dsInChannel = (Channel) values.next();
                    dsInChannel.setUseStatus("0");
                    dsInChannel.setRefresh(true);
                    dsInChannel.update();
                } catch (Exception e) {
                    log.error("�޸����Դ" + dsInChannel.getName() + "״̬��Ϊ0ʱ����", e);
                }
            }
        }
        HashMap dstrMap = (HashMap) pageContext.getAttribute(IP_DSTRMAP_KEYWORD);
        if (dstrMap != null && dstrMap.size() > 0) {
            Channel channel;
            try {
                channel = (Channel) Channel.getInstance(channelPath);
                channel.updateIndexDFile(dstrMap);
            } catch (Exception e) {
                log.error("����Ƶ�������ļ���CHD���ı�ǩ�������", e);
            }
        }
        if (this.getBodyContent() != null) {
            JspWriter jspwriter = super.pageContext.getOut();
            String s = this.getBodyContent().getString();
            this.bodyContent = null;
            try {
                jspwriter.print(s);
            } catch (IOException ioexception) {
                throw new JspException("DataListTag: ���������ݵ��������");
            }
        }
        return EVAL_PAGE;
    }

    /**
	 * @return
	 */
    public Channel getChannel() {
        return channel;
    }

    /**
	 * @return
	 */
    public String getChannelPath() {
        return channelPath;
    }
}
