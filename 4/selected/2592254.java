package com.genITeam.ria.actions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.*;
import com.genITeam.ria.bl.ThreadPostBL;
import com.genITeam.ria.exception.NGFException;
import com.genITeam.ria.vo.Posts;

public class ThreadAction extends AbstractAction {

    public ThreadAction() {
        BasicConfigurator.configure();
    }

    Logger logger = Logger.getLogger(ThreadAction.class);

    /**
	 * handleForumThread to get Parameters from Request object and write
	 * resulting response
	 * 
	 * 
	 * @param HttPServletRequest,HttpServletResponse
	 * @return none
	 * @throws NGFException
	 */
    public void handleForumThread(HttpServletRequest request, HttpServletResponse response) throws NGFException {
        String threadid = request.getParameter("threadid");
        ThreadPostBL postBL = null;
        try {
            logger.info("start handleForumThread ");
            postBL = new ThreadPostBL();
            this.initAction(request, response);
            this.writeResponse(postBL.getChilds(threadid));
        } catch (NGFException e) {
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getErrorMessage() + "</error>");
            System.out.print("\n\n\n\nThread Excepton = " + e.getErrorMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("Exception=" + e.toString());
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getMessage() + "</error>");
        }
    }

    /**
	 * handleDeleteThread to get Parameters from Request object and write
	 * resulting response
	 * 
	 * 
	 * @param HttPServletRequest,HttpServletResponse
	 * @return none
	 * @throws NGFException
	 */
    public void handleDeleteThread(HttpServletRequest request, HttpServletResponse response) throws NGFException {
        logger.info("handleDeleteThread");
        String threadid = request.getParameter("threadid");
        System.out.println("handleDeleteThread  thread id = " + threadid);
        Posts posts = new Posts();
        ThreadPostBL threadPostBL = new ThreadPostBL();
        try {
            logger.debug("start try handleDeleteThread = " + threadid);
            posts.setPostId(Integer.parseInt(threadid));
            threadPostBL.deleteThread(posts);
            this.initAction(request, response);
            this.writeResponse("<message>Thread Deletes </message>");
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
	 * handleThreadPosts to get Parameters from Request object and write
	 * resulting response
	 * 
	 * 
	 * @param HttPServletRequest,HttpServletResponse
	 * @return none
	 * @throws NGFException
	 */
    public void handleThreadPosts(HttpServletRequest request, HttpServletResponse response) throws NGFException {
        logger.info("handleThreadPosts");
        String threadid = request.getParameter("threadid");
        ThreadPostBL postBL = null;
        try {
            logger.debug("start try handleThreadPosts = " + threadid);
            postBL = new ThreadPostBL();
            this.initAction(request, response);
            this.writeResponse(postBL.getPost(threadid));
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
	 * handleGetRoomList to get Parameters from Request object and write
	 * resulting response
	 * 
	 * 
	 * @param HttPServletRequest,HttpServletResponse
	 * @return none
	 * @throws NGFException
	 */
    public void handleGetRoomList(HttpServletRequest request, HttpServletResponse response) throws NGFException {
        logger.info("getRoomList");
        ThreadPostBL postBL = null;
        try {
            logger.debug("start handleGetRoomList");
            postBL = new ThreadPostBL();
            this.initAction(request, response);
            this.writeResponse(postBL.getRoomList());
        } catch (NGFException e) {
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getErrorMessage() + "</error>");
            System.out.print("Search Forum Excepton = " + e.getMessage());
        } catch (Exception e) {
            logger.error("Excpetion=" + e.toString());
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getMessage() + "</error>");
        }
    }

    public void handleDeletePosts(HttpServletRequest request, HttpServletResponse response) throws NGFException {
        logger.info("handleDeleteThread");
        String threadid = request.getParameter("threadid");
        System.out.println("handleDeleteThread  post id = " + threadid);
        Posts posts = new Posts();
        ThreadPostBL threadPostBL = new ThreadPostBL();
        try {
            logger.debug("start try handleDeleteThread = " + threadid);
            posts.setPostId(Integer.parseInt(threadid));
            threadPostBL.deletePost(posts);
            this.initAction(request, response);
            this.writeResponse("<message>Thread Deletes </message>");
        } catch (NGFException e) {
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getErrorMessage() + "</error>");
            System.out.print("Search Forum Excepton = " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getMessage() + "</error>");
        }
    }
}
