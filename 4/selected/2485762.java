package net.videgro.oma.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.videgro.oma.business.MemberFilter;
import net.videgro.oma.domain.BirthdayComparator;
import net.videgro.oma.domain.Member;
import net.videgro.oma.domain.MemberPermissions;
import net.videgro.oma.managers.ChannelManager;
import net.videgro.oma.managers.FunctionManager;
import net.videgro.oma.managers.MemberManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

/**
 * 
 * @author Vincent de Groot
 * 
 *         <ol>
 *         <li>Get e-mail list of members in a cie, channel, all approved
 *         members</li>
 *         <li>Get a list of birthdays of the members including e-mail addresses
 *         </li>
 *         </ol>
 * 
 *         /replicate.do?filterName=functionbyname&filterValue=Actcie
 *         /replicate.do?filterName=channelbyname&filterValue=Birthday
 * 
 *         * Get: [format] qmail (View qmail format) Default POSTFIX-format
 */
public class ReplicateController implements Controller {

    public static final String FORMAT_QMAIL = "QMAIL";

    public static final String FORMAT_BIRTHDAYS = "BIRTHDAYS";

    protected final Log logger = LogFactory.getLog(getClass());

    private MemberManager memberMan;

    private FunctionManager functionManager;

    private ChannelManager channelManager;

    /**
	 * Get e-mail addresses of members in a cie or a birthdays list
	 * 
	 * Parameters parsed via internal OMA data object:
	 * <ul>
	 * <li>filterName - functionbyname | channelbyname</li>
	 * <li>filterValue - NULL | name of function | name of channel</li>
	 * <li>format - BIRTHDAYS | QMAIL | POSTFIX (default)</li>
	 * </ul>
	 * Returns list of mailadresses OR list of birthdays. Each address on new
	 * line suffixed with a comma (,)
	 */
    @SuppressWarnings("unchecked")
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("Returning REPLICATE view");
        RequestParameters rp = new RequestParameters();
        rp.parse(request);
        Map myModel = new HashMap();
        myModel.put("now", (new java.util.Date()).toString());
        String view = "";
        if (request.getParameter("filterValue") == null) {
            if (rp.getFilterNames()[0].equals(MemberFilter.FILTER_CHANNEL_BY_NAME)) {
                myModel.put("list", channelManager.getChannelList(true));
            } else {
                myModel.put("list", functionManager.getFunctionList(true));
            }
            view = "replicate-list";
        } else {
            ArrayList<Member> memberListWithDetails = getMemberManager().getMemberListWithDetails(rp.getFilterNames(), rp.getFilterValues(), MemberPermissions.USER_ID_ADMIN_INTERN);
            if (rp.getFormat().equals(FORMAT_BIRTHDAYS)) {
                view = "replicate-birthdays";
                Collections.sort(memberListWithDetails, new BirthdayComparator(true));
                myModel.put("members", memberListWithDetails);
            } else if (rp.getFormat().equals(FORMAT_QMAIL)) {
                view = "replicate-qmail";
                myModel.put("members", memberListWithDetails);
            } else {
                view = "replicate-postfix";
                myModel.put("members", memberListWithDetails);
            }
        }
        return new ModelAndView(view, "model", myModel);
    }

    public void setMemberManager(MemberManager mm) {
        memberMan = mm;
    }

    public MemberManager getMemberManager() {
        return memberMan;
    }

    public void setFunctionManager(FunctionManager functionManager) {
        this.functionManager = functionManager;
    }

    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }
}
