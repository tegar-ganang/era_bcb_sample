package com.genITeam.ria.actions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import com.genITeam.ria.bl.HomeBL;
import com.genITeam.ria.exception.NGFException;

public class HomeAction extends AbstractAction {

    public HomeAction() {
        BasicConfigurator.configure();
    }

    Logger logger = Logger.getLogger(HomeAction.class);

    /**
	 * handleFavouriteForum to get Parameters from Request object and write
	 * resulting response
	 * 
	 * 
	 * @param HttPServletRequest,HttpServletResponse
	 * @return none
	 * @throws NGFException
	 */
    public void handleFavouriteForum(HttpServletRequest request, HttpServletResponse response) throws NGFException {
        HomeBL homeBL;
        try {
            logger.info("start handleFavouriteForum");
            String memberId = request.getParameter("memberId");
            System.out.println("handleFavouriteForum memberId= " + memberId);
            homeBL = new HomeBL();
            this.initAction(request, response);
            this.writeResponse(homeBL.generateFavouriteForumData(memberId));
        } catch (NGFException e) {
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getErrorMessage() + "</error>");
            System.out.print("Search Forum Excepton = " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception=" + e.toString());
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getMessage() + "</error>");
        }
    }

    /**
	 * handleMyPost to get Parameters from Request object and and write
	 * resulting response
	 * 
	 * 
	 * @param HttPServletRequest,HttpServletResponse
	 * @return none
	 * @throws NGFException
	 */
    public void handleMyPost(HttpServletRequest request, HttpServletResponse response) throws NGFException {
        HomeBL homeBL = new HomeBL();
        try {
            logger.info("start handleMyPosts");
            String memberId = request.getParameter("memberId");
            System.out.println("handleMyPost memberId= " + memberId);
            this.initAction(request, response);
            this.writeResponse(homeBL.generateMemberThreadData(memberId));
        } catch (NGFException e) {
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getErrorMessage() + "</error>");
            System.out.print("Search Forum Excepton = " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception=" + e.toString());
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getMessage() + "</error>");
        }
    }

    /**
	 * handleBuddiesData to get Parameters from Request object and write
	 * resulting response
	 * 
	 * 
	 * @param HttPServletRequest,HttpServletResponse
	 * @return none
	 * @throws NGFException
	 */
    public void handleBuddiesData(HttpServletRequest request, HttpServletResponse response) throws NGFException {
        HomeBL homeBL = new HomeBL();
        try {
            logger.info("start handleBuddiesData");
            String memberId = request.getParameter("memberId");
            System.out.println("handleBuddiesData memberId= " + memberId);
            this.initAction(request, response);
            this.writeResponse(homeBL.generateBuddiesData(memberId));
        } catch (NGFException e) {
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getErrorMessage() + "</error>");
            System.out.print("Search Forum Excepton = " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception=" + e.toString());
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getMessage() + "</error>");
        }
    }
}
