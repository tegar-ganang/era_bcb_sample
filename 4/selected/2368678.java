package ces.platform.infoplat.ui.website.action;

import javax.servlet.http.*;
import org.apache.struts.action.*;
import ces.coral.log.*;
import ces.platform.infoplat.core.*;
import ces.platform.infoplat.core.base.*;
import ces.platform.infoplat.core.tree.*;
import ces.platform.infoplat.ui.common.defaultvalue.*;
import ces.platform.infoplat.utils.*;
import ces.platform.system.facade.*;

/**
 *  <b>����û�Ȩ�޽���</b> <br>
 *
 *
 *@Title      ��Ϣƽ̨
 *@Company    �Ϻ�������Ϣ���޹�˾
 *@version    2.5
 *@author     ����
 *@created    2004��4��15��
 */
public class AddActorAction extends Action {

    Logger log = new Logger(getClass());

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        String users = request.getParameter("users");
        String treePath = request.getParameter("treePath");
        try {
            String resID = "";
            TreeNode tn = TreeNode.getInstance(treePath);
            if (tn == null) {
                throw new Exception("�Ҳ���path=" + treePath + "�Ķ���!");
            }
            String resName = tn.getTitle();
            if (tn.getType() != null && (tn.getType().trim().equalsIgnoreCase("chan") || tn.getType().trim().equalsIgnoreCase("ds"))) {
                Channel channel = (Channel) tn;
                resID = Integer.toString(channel.getChannelID() + Const.CHANNEL_TYPE_RES);
            } else {
                Site site = (Site) tn;
                resID = Integer.toString(site.getSiteID() + Const.SITE_TYPE_RES);
            }
            AuthorityManager am = new AuthorityManager();
            boolean hasResource = am.hasSysResource(resID, Const.OPERATE_TYPE_ID);
            if (!hasResource) {
                int operateTypeID = Const.OPERATE_TYPE_ID;
                int resTypeID = Const.RES_TYPE_ID;
                String remark = "";
                am.createExtResource(resID, resName, resTypeID, operateTypeID, remark);
            }
            int[] userIds = Function.strArray2intArray(Function.stringToArray(users));
            LoginUser lu = new LoginUser();
            lu.setRequest(request);
            int loginUserId = Integer.parseInt(lu.getDefaultValue());
            Pepodom addActor = new Pepodom();
            for (int i = 0; i < userIds.length; i++) {
                addActor.setLoginProvider(loginUserId);
                addActor.setResID(resID);
                addActor.setUserID(userIds[i]);
                addActor.setResTypeID(Const.RES_TYPE_ID);
                addActor.setOperateID(Const.OPERATE_ID_SITECHANNEL);
                addActor.setOperateTypeID(Const.OPERATE_TYPE_ID);
                log.debug("1*: " + loginUserId + " : " + resID + " : " + userIds[i] + " : " + Const.RES_TYPE_ID + " : " + Const.OPERATE_ID_SITECHANNEL + " : " + Const.OPERATE_TYPE_ID);
                addActor.addNewActor();
            }
        } catch (Exception ex) {
            log.error("����û�Ȩ�޳���!", ex);
            request.setAttribute(Const.ERROR_MESSAGE_NAME, ex.toString());
            return mapping.findForward("error");
        }
        request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "�û�ѡ��ɹ�!");
        return mapping.findForward("operateSuccess");
    }
}
