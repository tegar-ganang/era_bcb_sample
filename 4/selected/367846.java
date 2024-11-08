package ces.platform.infoplat.ui.website.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import ces.coral.log.Logger;
import ces.platform.infoplat.core.ChannelSchedule;

public class ChannelScheduleAction extends Action {

    Logger log = new Logger(getClass());

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        String operate = request.getParameter("operate");
        String strChannelpath = request.getParameter("siteChannelPath");
        String[][] aTimes = null;
        ChannelSchedule schedule = new ChannelSchedule(strChannelpath);
        if (null != operate && operate.equals("save")) {
            String[] begins = request.getParameterValues("begin");
            String[] ends = request.getParameterValues("end");
            try {
                String times = "-";
                if (null != begins) {
                    for (int i = 0; i < begins.length; i++) {
                        if (!"".equals(begins[i]) && !"".equals(ends[i])) {
                            times += ";" + begins[i] + "-" + ends[i];
                        }
                    }
                    if (!"-".equals(times)) {
                        times = times.substring(2);
                    }
                }
                schedule.wirteFile(times);
            } catch (Exception ex) {
                log.error("ʱ������ó���!", ex);
            }
        }
        try {
            aTimes = schedule.getChannelScheduleTime();
        } catch (Exception ex) {
            log.error("��ʱ��γ���!", ex);
        }
        request.setAttribute("times", aTimes);
        return mapping.findForward("scheduleprop");
    }
}
