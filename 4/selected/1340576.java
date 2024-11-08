package com.genITeam.ria.actions;

import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.BasicConfigurator;
import com.genITeam.ria.bl.ChatRoomBL;
import com.genITeam.ria.exception.NGFException;
import com.genITeam.ria.vo.ChatMessageVo;

/**
 * @author 05030045
 *
 */
public class ChatRoomAction extends AbstractAction {

    public ChatRoomAction() {
        BasicConfigurator.configure();
    }

    public void handleAddUser(HttpServletRequest request, HttpServletResponse response, ArrayList roomUsers) throws NGFException {
        int roomId = Integer.parseInt(request.getParameter("roomId"));
        int memberId = Integer.parseInt(request.getParameter("memberId"));
        StringTokenizer stk = null;
        boolean ifExists = false;
        for (int i = 0; i < roomUsers.size(); i++) {
            stk = new StringTokenizer((String) roomUsers.get(i), ",");
            if (roomId == Integer.parseInt(stk.nextToken()) && memberId == Integer.parseInt(stk.nextToken())) {
                ifExists = true;
                break;
            }
        }
        String nick = null;
        this.initAction(request, response);
        if (!ifExists) {
            roomUsers.add(roomId + "," + memberId);
            ChatRoomBL roomBL = new ChatRoomBL();
            nick = roomBL.getMember(memberId).getNick();
            this.writeResponse("<nick>" + nick + "</nick>");
        } else this.writeResponse("<message>User Added</message>");
    }

    public void handleRemoveUser(HttpServletRequest request, HttpServletResponse response, ArrayList roomUsers) throws NGFException {
        int roomId = Integer.parseInt(request.getParameter("roomId"));
        int memberId = Integer.parseInt(request.getParameter("memberId"));
        StringTokenizer stk = null;
        for (int i = 0; i < roomUsers.size(); i++) {
            stk = new StringTokenizer((String) roomUsers.get(i), ",");
            if (roomId == Integer.parseInt(stk.nextToken()) && memberId == Integer.parseInt(stk.nextToken())) {
                roomUsers.remove(i);
                break;
            }
        }
        this.initAction(request, response);
        this.writeResponse("<message>Thread Created Successfully</message>");
    }

    public void handleGetUserList(HttpServletRequest request, HttpServletResponse response, ArrayList roomUsers) throws NGFException {
        String roomId = request.getParameter("roomId");
        ChatRoomBL roomBL = null;
        try {
            System.out.println("Getting list for room = " + roomId);
            roomBL = new ChatRoomBL();
            this.initAction(request, response);
            this.writeResponse(roomBL.getUserList(roomUsers, Integer.parseInt(roomId)));
        } catch (Exception e) {
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getMessage() + "</error>");
        }
    }

    public void handleAddMessage(HttpServletRequest request, HttpServletResponse response, ArrayList chatMessages) throws NGFException {
        String roomId = request.getParameter("roomId");
        String memberId = request.getParameter("memberId");
        String message = request.getParameter("message");
        int messageId = chatMessages.size() + 1;
        System.out.println("Adding message no " + messageId);
        chatMessages.add(new ChatMessageVo(Integer.parseInt(roomId), Integer.parseInt(memberId), messageId, message));
        this.initAction(request, response);
        this.writeResponse("<message>Thread Created Successfully</message>");
    }

    public void handleGetMessages(HttpServletRequest request, HttpServletResponse response, ArrayList chatMessages) throws NGFException {
        String roomId = request.getParameter("roomId");
        String memberId = request.getParameter("memberId");
        String messageId = request.getParameter("messageId");
        ChatRoomBL roomBL = null;
        try {
            System.out.println("Getting messages for " + roomId + " " + messageId);
            roomBL = new ChatRoomBL();
            this.initAction(request, response);
            this.writeResponse(roomBL.getMessages(chatMessages, Integer.parseInt(roomId), Integer.parseInt(messageId), Integer.parseInt(memberId)));
        } catch (Exception e) {
            this.initAction(request, response);
            this.writeResponse("<error>" + e.getMessage() + "</error>");
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
    }
}
