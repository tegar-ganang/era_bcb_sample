package ces.platform.infoplat.ui.website.action;

import javax.servlet.http.*;
import org.apache.struts.action.*;
import ces.coral.log.*;
import ces.platform.infoplat.core.*;
import ces.platform.infoplat.core.base.*;
import ces.platform.infoplat.core.tree.*;
import ces.platform.infoplat.ui.common.defaultvalue.*;
import ces.platform.infoplat.utils.*;

/**
 *  <b>Ƶ��Ȩ���б����-->��ѯƵ��Ȩ���б���ʾ</b> <br>
 *
 *
 *@Title      ��Ϣƽ̨
 *@Company    �Ϻ�������Ϣ���޹�˾
 *@version    2.5
 *@author     ����
 *@created    2004��4��14��
 */
public class ChannelPepodomAction extends Action {

    Logger log = new Logger(getClass());

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        String status = request.getParameter("status");
        String treePath = request.getParameter("treePath");
        try {
            Channel channel = new Channel();
            channel = (Channel) TreeNode.getInstance(treePath);
            int resID = channel.getChannelID() + Const.CHANNEL_TYPE_RES;
            LoginUser lu = new LoginUser();
            lu.setRequest(request);
            int loginUserId = Integer.parseInt(lu.getDefaultValue());
            Pepodom[] ps = Pepodom.getExtPepodom(Integer.toString(resID));
            if (status.trim().equalsIgnoreCase("save")) {
                String cascade = request.getParameter("cascade");
                if (cascade == null) {
                    cascade = "";
                }
                boolean cas = false;
                if (cascade.trim().equalsIgnoreCase("1")) {
                    cas = true;
                }
                String checkedValues = request.getParameter("checkedValues");
                String[] operateIDs = Function.stringToArray(checkedValues);
                for (int i = 0; i < ps.length; i++) {
                    ps[i].setLoginProvider(loginUserId);
                    ps[i].setOperateID(operateIDs[i]);
                }
                Pepodom.assignExtPepodom(ps, treePath, cas);
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "�û�Ȩ�ޱ���ɹ�!");
                return mapping.findForward("inure");
            } else {
                ps = Pepodom.getExtPepodom(String.valueOf(resID));
                request.setAttribute("result", ps);
                request.setAttribute("treePath", treePath);
                return mapping.findForward("list");
            }
        } catch (Exception ex) {
            log.error("��Ȩ����ȡƵ��Ȩ���б����!", ex);
            request.setAttribute(Const.ERROR_MESSAGE_NAME, ex.toString());
            return mapping.findForward("error");
        }
    }
}
