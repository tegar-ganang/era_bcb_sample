package org.light.portal.core.portlets;

import static org.light.portal.util.Constants._OBJECT_TYPE_ORG;
import java.util.ArrayList;
import java.util.List;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import org.json.JSONObject;
import org.light.portal.core.permission.model.ObjectRole;
import org.light.portal.core.permission.model.UserObjectRole;
import org.light.portal.portlet.core.impl.LightGenericPortlet;
import org.light.portal.user.model.User;
import org.light.portal.user.model.UserBlock;
import org.light.portal.user.model.UserInvite;
import org.light.portal.user.model.UserProfile;
import org.light.portal.util.HashUtil;
import org.light.portal.util.JSONUtil;
import org.light.portal.util.LabelBean;
import org.light.portal.util.OrganizationThreadLocal;
import org.light.portal.util.StringUtil;

/**
 * 
 * @author Jianmin Liu
 **/
public class AccountPortlet extends LightGenericPortlet {

    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, java.io.IOException {
        String action = request.getParameter("action");
        if ("name".equals(action)) saveName(request); else if ("interests".equals(action)) saveInterests(request); else if ("basic".equals(action)) saveBasic(request); else if ("background".equals(action)) saveBackgroud(request); else if ("password".equals(action)) savePassword(request); else if ("privacy".equals(action)) savePrivacy(request); else if ("channels".equals(action)) saveChannels(request); else if ("block".equals(action)) unBlockUser(request);
    }

    protected void doView(RenderRequest request, RenderResponse response) throws PortletException, java.io.IOException {
        User user = this.getUser(request);
        UserProfile userProfile = this.getUserService().getUserProfileById(user.getId());
        request.setAttribute("user", user);
        request.setAttribute("userProfile", userProfile);
        String part = request.getParameter("action");
        if (part == null) part = "name";
        request.setAttribute("current", part);
        try {
            JSONObject json = new JSONObject();
            json.put("current", part);
            String success = "";
            if (request.getAttribute("success") != null) success = (String) request.getAttribute("success");
            json.put("success", success);
            String error = "";
            if (request.getAttribute("error") != null) error = (String) request.getAttribute("error");
            json.put("error", error);
            if (part.equals("channels")) {
                json.put("view", "myAccountChannels.view");
                List<UserObjectRole> userChannels = this.getUserService().getUserChannels(user.getId(), user.getOrgId());
                List<String> selectedChannels = new ArrayList<String>();
                if (userChannels != null) {
                    for (UserObjectRole ur : userChannels) {
                        selectedChannels.add(ur.getRole().getName());
                    }
                }
                List<LabelBean> channels = OrganizationThreadLocal.getOrg().getChannels();
                for (LabelBean channel : channels) {
                    if (selectedChannels.contains(channel.getId())) channel.setDefaulted(true); else channel.setDefaulted(false);
                }
                request.setAttribute("channels", channels);
                request.setAttribute("totalChannels", channels.size());
                json.put("totalChannels", channels.size());
                json.put("channels", JSONUtil.getLabelBeans(channels));
            } else if (part.equals("invite")) {
                json.put("view", "myAccountInvite.view");
                List<UserInvite> invites = this.getUserService().getUserInvites(this.getUser(request).getId());
                int inviteCount = 0;
                if (invites != null) inviteCount = invites.size();
                request.setAttribute("inviteCount", inviteCount);
                request.setAttribute("invites", invites);
                json.put("inviteCount", inviteCount);
                json.put("invites", JSONUtil.getUserInviteJArray(invites));
            } else if (part.equals("block")) {
                json.put("view", "myAccountBlock.view");
                List<UserBlock> blocks = this.getUserService().getUserBlocks(this.getUser(request).getId());
                int blockCount = 0;
                if (blocks != null) blockCount = blocks.size();
                request.setAttribute("blockCount", blockCount);
                request.setAttribute("blocks", blocks);
                json.put("blockCount", blockCount);
                json.put("blocks", JSONUtil.getUserBlockJArray(blocks));
            } else if (part.equals("password")) {
                json.put("view", "myAccountPassword.view");
            } else {
                if (part.equals("basic")) json.put("view", "myAccountBasic.view"); else if (part.equals("interests")) json.put("view", "myAccountInterests.view"); else if (part.equals("background")) json.put("view", "myAccountBackground.view"); else if (part.equals("privacy")) json.put("view", "myAccountPrivacy.view"); else json.put("view", "myAccountName.view");
                json.put("user", JSONUtil.getUserJSON(user));
                json.put("userProfile", JSONUtil.getUserProfileJSON(userProfile));
            }
            response.getWriter().write(json.toString());
        } catch (Exception e) {
        }
    }

    private void saveName(PortletRequest request) throws PortletException, java.io.IOException {
        String displayName = request.getParameter("displayName");
        if (displayName == null) {
            request.setAttribute("error", "please input your display name.");
            return;
        }
        String firstName = request.getParameter("firstName");
        String middleName = request.getParameter("middleName");
        String lastName = request.getParameter("lastName");
        User user = this.getUser(request);
        user.setDisplayName(displayName);
        this.getUserService().save(user);
        UserProfile userProfile = this.getUserService().getUserProfileById(user.getId());
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUserId(user.getId());
        }
        userProfile.setFirstName(firstName);
        userProfile.setMiddleName(middleName);
        userProfile.setLastName(lastName);
        this.getPortalService().save(userProfile);
        request.setAttribute("success", "You have changed your name successfully.");
    }

    private void saveInterests(PortletRequest request) throws PortletException, java.io.IOException {
        String headline = request.getParameter("headline");
        String aboutMe = request.getParameter("aboutMe");
        String likeToMeet = request.getParameter("likeToMeet");
        String interests = request.getParameter("interests");
        String music = request.getParameter("music");
        String movies = request.getParameter("movies");
        String television = request.getParameter("television");
        String books = request.getParameter("books");
        String heroes = request.getParameter("heroes");
        User user = this.getUser(request);
        UserProfile userProfile = this.getUserService().getUserProfileById(user.getId());
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUserId(user.getId());
        }
        userProfile.setHeadline(headline);
        userProfile.setAboutMe(aboutMe);
        userProfile.setLikeToMeet(likeToMeet);
        userProfile.setInterests(interests);
        userProfile.setMusic(music);
        userProfile.setMovies(movies);
        userProfile.setTelevision(television);
        userProfile.setBooks(books);
        userProfile.setHeroes(heroes);
        this.getPortalService().save(userProfile);
        request.setAttribute("success", "You have changed you interests successfully.");
    }

    private void saveBasic(PortletRequest request) throws PortletException, java.io.IOException {
        String gender = request.getParameter("gender");
        String occupation = request.getParameter("occupation");
        String country = request.getParameter("country");
        String province = request.getParameter("province");
        String city = request.getParameter("city");
        String postalCode = request.getParameter("postalCode");
        String ethnicity = request.getParameter("ethnicity");
        String bodyType = request.getParameter("bodyType");
        String height = request.getParameter("height");
        String registerPurpose = request.getParameter("registerPurpose");
        String email = request.getParameter("email");
        if (email == null) {
            request.setAttribute("error", "please input your Email address.");
            return;
        }
        if (email.indexOf("@") <= 0 || email.indexOf(".") <= 0) {
            request.setAttribute("error", "please input your Email address correctly.");
            return;
        }
        User user = this.getUser(request);
        if (!user.getEmail().equals(email)) {
            user.setEmail(email);
        }
        String birthY = request.getParameter("birthY");
        String birthM = request.getParameter("birthM");
        String birthD = request.getParameter("birthD");
        if (!StringUtil.isEmpty(birthY) && !StringUtil.isEmpty(birthM) && !StringUtil.isEmpty(birthD)) {
            user.setBirth(birthY + "/" + birthM + "/" + birthD);
        }
        user.setGender(gender);
        user.setCountry(country);
        user.setProvince(province);
        user.setCity(city);
        user.setPostalCode(postalCode);
        this.getUserService().save(user);
        UserProfile userProfile = this.getUserService().getUserProfileById(user.getId());
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUserId(user.getId());
        }
        userProfile.setOccupation(occupation);
        userProfile.setEthnicity(Integer.parseInt(ethnicity));
        userProfile.setBodyType(Integer.parseInt(bodyType));
        userProfile.setHeight(Integer.parseInt(height));
        if (registerPurpose != null) {
            int purpose = 0;
            try {
                purpose = Integer.parseInt(registerPurpose);
            } catch (NumberFormatException e) {
            }
            userProfile.setRegisterPurpose(purpose);
        }
        this.getPortalService().save(userProfile);
        request.setAttribute("success", "You have changed your basic information successfully.");
    }

    private void saveBackgroud(PortletRequest request) throws PortletException, java.io.IOException {
        String maritalStatus = request.getParameter("maritalStatus");
        String sexualOrientation = request.getParameter("sexualOrientation");
        String religion = request.getParameter("religion");
        String hometown = request.getParameter("hometown");
        String smoker = request.getParameter("smoker");
        String drinker = request.getParameter("drinker");
        String childrenStatus = request.getParameter("childrenStatus");
        String education = request.getParameter("education");
        String income = request.getParameter("income");
        User user = this.getUser(request);
        UserProfile userProfile = this.getUserService().getUserProfileById(user.getId());
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUserId(user.getId());
        }
        userProfile.setMaritalStatus(Integer.parseInt(maritalStatus));
        userProfile.setSexualOrientation(Integer.parseInt(sexualOrientation));
        userProfile.setReligion(religion);
        userProfile.setHometown(hometown);
        userProfile.setSmoker(Integer.parseInt(smoker));
        userProfile.setDrinker(Integer.parseInt(drinker));
        userProfile.setChildrenStatus(Integer.parseInt(childrenStatus));
        userProfile.setEducation(Integer.parseInt(education));
        userProfile.setIncome(income);
        this.getPortalService().save(userProfile);
        request.setAttribute("success", "You have changed your background information successfully.");
    }

    private void savePassword(PortletRequest request) throws PortletException, java.io.IOException {
        String password = request.getParameter("password");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        User user = this.getUser(request);
        if (password == null) {
            request.setAttribute("error", "please input your current password.");
            return;
        }
        String ePassword = HashUtil.MD5Hashing(password);
        if (!ePassword.equals(user.getPassword())) {
            request.setAttribute("error", "please input your current password correctly.");
            return;
        }
        if (newPassword == null || confirmPassword == null || !newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "please input your new password correctly.");
            return;
        }
        user.setPassword(HashUtil.MD5Hashing(newPassword));
        this.getUserService().save(user);
        request.setAttribute("success", "You have changed password successfully.<br/>");
    }

    private void savePrivacy(PortletRequest request) {
        int notification = 0;
        if (request.getParameter("notification") != null) notification = 1;
        int newsLetter = 0;
        if (request.getParameter("newsLetter") != null) newsLetter = 1;
        int fqNel = 0;
        if (request.getParameter("fqNel") != null) fqNel = 1;
        int commentNeedApprove = 0;
        if (request.getParameter("commentNeedApprove") != null) commentNeedApprove = 1;
        int showBirthToFriend = 0;
        if (request.getParameter("showBirthToFriend") != null) showBirthToFriend = 1;
        int showTitleToFriends = 0;
        if (request.getParameter("showTitleToFriends") != null) showTitleToFriends = 1;
        int blogCommentFriendOnly = 0;
        if (request.getParameter("blogCommentFriendOnly") != null) blogCommentFriendOnly = 1;
        int profileFriendViewOnly = 0;
        if (request.getParameter("profileFriendViewOnly") != null) profileFriendViewOnly = 1;
        int noPicForward = 0;
        if (request.getParameter("noPicForward") != null) noPicForward = 1;
        int myMusicAutoPlay = 0;
        if (request.getParameter("myMusicAutoPlay") != null) myMusicAutoPlay = 1;
        int otherMusucAutoPlay = 0;
        if (request.getParameter("otherMusucAutoPlay") != null) otherMusucAutoPlay = 1;
        int imprivacy = 0;
        if (request.getParameter("imprivacy") != null) imprivacy = Integer.parseInt(request.getParameter("imprivacy"));
        User user = this.getUser(request);
        user.setNotification(notification);
        user.setNewsLetter(newsLetter);
        user.setFqNel(fqNel);
        user.setCommentNeedApprove(commentNeedApprove);
        user.setShowTitleToFriends(showTitleToFriends);
        user.setShowBirthToFriend(showBirthToFriend);
        user.setBlogCommentFriendOnly(blogCommentFriendOnly);
        user.setProfileFriendViewOnly(profileFriendViewOnly);
        user.setNoPicForward(noPicForward);
        user.setMyMusicAutoPlay(myMusicAutoPlay);
        user.setOtherMusucAutoPlay(otherMusucAutoPlay);
        user.setImprivacy(imprivacy);
        this.getUserService().save(user);
        request.setAttribute("success", "You have changed your privacy information successfully.");
    }

    private void saveChannels(PortletRequest request) {
        String channels = request.getParameter("channels");
        User user = this.getUser(request);
        List<UserObjectRole> userChannels = this.getUserService().getUserChannels(user.getId(), user.getOrgId());
        if (userChannels != null) {
            for (UserObjectRole ur : userChannels) {
                if (channels == null || channels.indexOf(ur.getRole().getName()) < 0) this.getPortalService().delete(ur);
            }
        }
        if (channels != null) {
            String[] channel = channels.split(",");
            for (int i = 0; i < channel.length; i++) {
                if (channel[i] != null && !"".equals(channel[i]) && !user.hasRole(channel[i])) {
                    ObjectRole channelRole = this.getUserService().getRoleByName(channel[i], 0L);
                    if (channelRole != null) {
                        UserObjectRole roleChannel = new UserObjectRole(user.getId(), user.getOrgId(), user.getOrgId(), _OBJECT_TYPE_ORG, channelRole);
                        this.getPortalService().save(roleChannel);
                    }
                }
            }
        }
        user = this.getUserService().getUserById(user.getId());
        this.setUser(request, user);
        request.setAttribute("success", "You have changed the subscribed Channels successfully, please click Done button to refresh channels.");
    }

    private void unBlockUser(PortletRequest request) {
        String blockId = request.getParameter("blockId");
        if (blockId == null) {
            request.setAttribute("error", "please select a user first.");
            return;
        }
        UserBlock block = this.getUserService().getUserBlockById(Integer.parseInt(blockId));
        this.getPortalService().delete(block);
        request.setAttribute("success", "This user has been deleted from block list successfully.");
    }
}
