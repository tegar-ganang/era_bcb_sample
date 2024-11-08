package ces.platform.infoplat.taglib;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import ces.coral.file.FileOperation;
import ces.coral.log.Logger;
import ces.platform.infoplat.core.Channel;
import ces.platform.infoplat.core.DS;
import ces.platform.infoplat.core.Template;
import ces.platform.infoplat.core.base.ConfigInfo;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.ui.website.defaultvalue.ChannelPath;
import ces.platform.infoplat.utils.Function;
import ces.platform.system.common.IdGenerator;

/**
 * <b>�ļ���:</b>DSTemplateRefTag.java<br>
 * <b>��������:</b>���Դ���ñ�ǩ<br>
 * <b>��Ȩ����:</b>�Ϻ�������Ϣ��չ���޹�˾(CES)2003
 * @author ֣��ǿ
 * @version 2.5.2004.0630
 *
 * Created on 2004-4-6
 *
 */
public class DSTemplateRefTag extends TagSupport {

    public static final String DS_PREFIX = "IPDS";

    public static final String DSTR_PREFIX = "ip";

    private String dstrId;

    private int templateId;

    private String params;

    private boolean isStatic = true;

    private String channelPath;

    private HashMap dsTagMap = null;

    Logger log = new Logger(this.getClass());

    public int doEndTag() throws JspException {
        DSTempRefManageTag papa = (DSTempRefManageTag) TagSupport.findAncestorWithClass(this, DSTempRefManageTag.class);
        if (papa == null) {
            throw new JspException("DSTemplateRefTag:�����ҵ�����ǩ ces.platform.infoplat.taglib.ds.DSTempRefManageTag");
        }
        Channel channel = papa.getChannel();
        channelPath = papa.getChannelPath();
        Template template = Template.getInstance(templateId);
        if (template == null) {
            log.error("ͨ��templateId" + templateId + "�õ���templateΪnull");
            return SKIP_PAGE;
        }
        StringTokenizer tokens = new StringTokenizer(params, ";");
        StringBuffer newParams = new StringBuffer();
        while (tokens.hasMoreTokens()) {
            if (this.dsTagMap == null) {
                this.dsTagMap = new HashMap(2);
            }
            String newParam = paramToTag(tokens.nextToken());
            if (newParam != null) {
                newParams.append(newParam);
                newParams.append(";");
            }
        }
        if (this.dsTagMap == null || this.dsTagMap.size() == 0) {
            return SKIP_PAGE;
        }
        String content = template.getDefineFileContent();
        if (content != null) {
            String templateRes = template.getResDirPath();
            String channelRes = null;
            try {
                channelRes = channel.getDefineFilePath() + "res_" + template.getAsciiName();
            } catch (Exception ex) {
            }
            try {
                FileOperation.copyDir(templateRes, channelRes);
            } catch (Exception e) {
                log.error("�ƶ����Դ��ԴĿ¼" + templateRes + "��" + channelRes + "ʧ��", e);
            }
            content = replaceAllTags(content, this.dsTagMap);
            String siteAsciiName = null;
            try {
                siteAsciiName = channel.getSiteAsciiName();
            } catch (Exception ex1) {
                log.error("", ex1);
            }
            if (!isStatic) {
                String contentPath = new StringBuffer(ConfigInfo.getInstance().getInfoplatDataDir()).append(File.separator).append("pub").append(File.separator).append(siteAsciiName).append(File.separator).append(channel.getAsciiName()).append(File.separator).append("dynContent_").append(this.dstrId).append(".data").toString();
                try {
                    Function.writeTextFile(content, contentPath, true);
                } catch (Exception e) {
                    log.error("���涯̬���Դʧ��", e);
                }
                content = "<" + DSTR_PREFIX + ":content dstrId=\"" + this.dstrId + "\">";
            }
            JspWriter out = pageContext.getOut();
            try {
                out.print(content.toString());
            } catch (IOException ioexception) {
                log.error("���Դģ������Tag�������: �������content��ݵ�ҳ��");
            }
        }
        if (newParams.toString().trim() != "") {
            HashMap dstrMap = (HashMap) pageContext.getAttribute(DSTempRefManageTag.IP_DSTRMAP_KEYWORD);
            dstrMap.put(this.dstrId, newParams.toString());
        }
        return EVAL_PAGE;
    }

    /**
	 * �Ѳ�����֯��tag ���tag�ĸ�ʽ���ԣ���ô��Ѹ�ٲ鵽����ĵط���
	 * @param param
	 * @return  ����֯�õ�param  ��������֯�õ�tag
	 * @info �������е�tag�ַ� �����Բ����쳣������null����tag�������?
	 */
    private String paramToTag(String param) {
        param = param.trim();
        StringBuffer newParam = new StringBuffer();
        String tagName = param.substring(0, param.indexOf(":"));
        newParam.append(tagName).append(":");
        Hashtable table = ConfigInfo.getInstance().getDses();
        DS ds = (DS) table.get(tagName.trim());
        if (ds == null) {
            log.error(tagName + "�������ļ���û��ע��");
            return null;
        }
        String tagParams = param.substring(param.indexOf(":") + 1, param.length());
        try {
            StringTokenizer tokens = new StringTokenizer(tagParams, "/");
            StringBuffer dsTag = new StringBuffer("<").append(DS_PREFIX).append(":").append(tagName);
            HashMap paramMap = new HashMap(6);
            boolean hasName = false;
            boolean hasPath = false;
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken().trim();
                String paramName = token.substring(0, token.indexOf("=")).trim();
                String paramValue = token.substring(token.indexOf("=") + 1, token.length()).trim();
                paramMap.put(paramName, paramValue);
                if (paramName.equalsIgnoreCase("name")) {
                    hasName = true;
                }
                if (paramName.equalsIgnoreCase("path")) {
                    hasPath = true;
                }
            }
            if (!hasName) {
                paramMap.put("name", "");
            }
            if (!hasPath) {
                paramMap.put("path", "");
            }
            Channel dsInChannel = null;
            String path = (String) paramMap.get("path");
            if (getDs(path) == null) {
                String name = (String) paramMap.get("name");
                dsInChannel = addNewDataSource(channelPath, name, ds);
            } else {
                dsInChannel = getDs(path);
            }
            if (dsInChannel == null) {
                return null;
            }
            Iterator it = paramMap.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                log.debug("paramToTag()----key=" + key);
                String paramValue = (String) paramMap.get(key);
                if (key.equalsIgnoreCase("path")) {
                    newParam.append(key).append("=").append(dsInChannel.getPath()).append("/");
                } else if (key.equalsIgnoreCase("name")) {
                    newParam.append(key).append("=").append(dsInChannel.getName()).append("/");
                } else {
                    dsTag.append(" ").append(key).append("=\"").append((String) paramMap.get(key)).append("\"");
                    newParam.append(key).append("=").append(paramValue).append("/");
                }
            }
            dsTag.append(">");
            if (this.dsTagMap != null) {
                log.debug("tagName000000------" + tagName + "------");
                log.debug("dsTag.toString()------" + dsTag.toString());
                this.dsTagMap.put(tagName.trim(), dsTag.toString());
            }
            return newParam.toString();
        } catch (Exception e) {
            log.error(tagName + "��ǩ�������?", e);
            return null;
        }
    }

    private Channel getDs(String dsPath) {
        Channel dsInChannel = null;
        if (!dsPath.trim().equals("")) {
            try {
                dsInChannel = (Channel) TreeNode.getInstance(dsPath);
                if (dsInChannel != null) {
                    if (dsInChannel.getUseStatus().equals("0")) {
                        dsInChannel.setUseStatus("1");
                        dsInChannel.update();
                    }
                    HashMap dsMap = (HashMap) pageContext.getAttribute(DSTempRefManageTag.IP_DSMAP_KEYWORD);
                    if (dsMap.containsKey(dsInChannel.getPath().trim())) {
                        dsMap.remove(dsInChannel.getPath().trim());
                    }
                } else {
                    dsInChannel = null;
                    throw new IllegalArgumentException("û���ҵ�path��ֵ��" + dsPath + "�����Դ");
                }
            } catch (Exception e) {
                log.error("���Դ����path��ֵ����ȷ��" + e.toString());
            }
        }
        return dsInChannel;
    }

    /**
	 *  ����µ����Դ
	 *  ����T_IP_CHANNEL���T_IP_TREE_FRAME��
	 *
	 */
    public Channel addNewDataSource(String dsParentPath, String name, DS ds) {
        Channel channel = null;
        try {
            ChannelPath channelPath = new ChannelPath();
            channelPath.setCPath(dsParentPath);
            String treeId = channelPath.getDefaultValue();
            int level = dsParentPath.length() / 5;
            int channelId = (int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_CHANNEL);
            String parentId = dsParentPath.substring((level - 1) * 5, dsParentPath.length());
            int siteID = Integer.parseInt(dsParentPath.substring(5, 10));
            channel = new Channel();
            channel.setPath(dsParentPath + treeId);
            channel.setSiteId(siteID);
            channel.setId(treeId);
            channel.setLevel(level);
            channel.setChannelID(channelId);
            channel.setParentId(parentId);
            channel.setChannelType("1");
            channel.setAsciiName(ds.getName() + "_" + channelId);
            String nameOrTitle = (name.equals("")) ? ds.getDescription() : name;
            channel.setName(nameOrTitle);
            channel.setTitle(nameOrTitle);
            channel.setTemplateId(String.valueOf(templateId));
            channel.setOrderNo(0);
            channel.setUseStatus("1");
            channel.setType("ds");
            channel.add();
        } catch (Exception e) {
            log.error("���Դ���ñ�ǩ����µ����Դ����", e);
        }
        return channel;
    }

    private String replaceAllTags(String content, HashMap map) {
        StringBuffer newContent = new StringBuffer();
        int startTag = content.indexOf("<" + DS_PREFIX);
        while (startTag != -1) {
            newContent.append(content.substring(0, startTag));
            content = content.substring(startTag);
            String tag = content.substring(0, content.indexOf(">") + 1);
            String tagName = tag.substring(tag.indexOf(":") + 1).trim();
            tagName = tagName.substring(0, tagName.indexOf(" ")).trim();
            log.debug("tagName=" + tagName);
            String newTag = (String) map.get(tagName);
            log.debug("size=" + map.size());
            log.debug("newTag=" + newTag);
            log.debug("tag=" + tag);
            if (newTag != null) {
                newContent.append(newTag);
                content = content.substring(tag.indexOf(">") + 1);
            } else {
                newContent.append(tag);
                content = content.substring(tag.indexOf(">") + 1);
            }
            startTag = content.indexOf("<" + DS_PREFIX);
        }
        newContent.append(content);
        return newContent.toString();
    }

    /**
	 * @param i
	 */
    public void setDstrId(String s) {
        dstrId = s;
    }

    /**
	 * @param string
	 */
    public void setStatic(String string) {
        isStatic = string.trim().equalsIgnoreCase("true");
    }

    public void setStatic(boolean b) {
        isStatic = b;
    }

    /**
	 * @param string
	 */
    public void setParams(String string) {
        params = string;
    }

    /**
	 * @param i
	 */
    public void setTemplateId(int i) {
        templateId = i;
    }
}
