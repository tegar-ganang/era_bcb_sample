package com.wkinney.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.wkinney.client.Membership;
import com.wkinney.client.SVNAdminService;

/**
 * @author wkinney
 *
 */
public class SVNAdminServiceImpl extends RemoteServiceServlet implements SVNAdminService {

    /**
     *
     */
    private static final long serialVersionUID = 6730497416085709800L;

    protected final Log LOG = LogFactory.getLog(getClass());

    private Set<String> memberList = null;

    private Map<String, List<String>> groupMembersMap = null;

    private Map<String, List<String>> projectAccessMap = null;

    private boolean loaded;

    public SVNAdminServiceImpl() {
        super();
        loadData(true);
    }

    @Override
    public List<String> getUserList() {
        LOG.debug("getUserList() - " + System.currentTimeMillis());
        return new ArrayList<String>(this.memberList);
    }

    @Override
    public Map<String, List<String>> getGroupMembersMap() {
        LOG.debug("getGroupMembersMap() - " + System.currentTimeMillis());
        return this.groupMembersMap;
    }

    @Override
    public Map<String, List<String>> getProjectAccessMap() {
        LOG.debug("getProjectAccessMap() - " + System.currentTimeMillis());
        return this.projectAccessMap;
    }

    private void loadData() {
        loadData(false);
    }

    private void loadData(boolean forceReload) {
        if (forceReload || !this.loaded) {
            LOG.debug("Loading configuration...");
            synchronized (SVNAdminServiceImpl.class) {
                try {
                    loadUserMemberList();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("error loading user member list data, cause: " + e.getMessage(), e);
                }
                try {
                    loadUserGroupAndDirectoryAccess();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("error loading user group or directory/project data, cause: " + e.getMessage(), e);
                }
                this.loaded = true;
            }
        }
    }

    private void loadUserMemberList() throws IOException {
        String svnAuthFile = SVNAdminServiceProperties.getHtpasswdUserAuthFile();
        InputStream is = null;
        try {
            final Set<String> tempMemberList = new HashSet<String>();
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(svnAuthFile);
            if (is == null) {
                is = getClass().getResourceAsStream(svnAuthFile);
                if (is == null) {
                    File authFile = new File(svnAuthFile);
                    if (!authFile.exists()) {
                        throw new RuntimeException("could not locate svn auth file: " + svnAuthFile);
                    }
                    if (!authFile.canRead() || !authFile.canWrite()) {
                        throw new RuntimeException("could not read/write svn auth file: " + svnAuthFile);
                    }
                    is = new FileInputStream(authFile);
                    if (is == null) {
                        throw new RuntimeException("could not locate svn auth file: " + svnAuthFile);
                    }
                }
            }
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            int indexOfSemiColon = -1;
            String username = null;
            String line = br.readLine();
            while (line != null) {
                indexOfSemiColon = line.indexOf(':');
                username = line.substring(0, indexOfSemiColon);
                tempMemberList.add(username);
                line = br.readLine();
            }
            this.memberList = tempMemberList;
        } finally {
            try {
                is.close();
                is = null;
            } catch (Exception ignore) {
            }
        }
    }

    private void loadUserGroupAndDirectoryAccess() throws IOException {
        String svnDirAccessFile = SVNAdminServiceProperties.getUserDirectoryAccessFile();
        InputStream is = null;
        try {
            final Set<String> groupList = new HashSet<String>();
            List<String> membersForGroupList = null;
            final Map<String, List<String>> groupMemberMap = new HashMap<String, List<String>>();
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(svnDirAccessFile);
            if (is == null) {
                is = getClass().getResourceAsStream(svnDirAccessFile);
                if (is == null) {
                    File svnDirAccess = new File(svnDirAccessFile);
                    if (!svnDirAccess.exists()) {
                        throw new RuntimeException("could not locate svn directory access file: " + svnDirAccessFile);
                    }
                    if (!svnDirAccess.canRead() || !svnDirAccess.canWrite()) {
                        throw new RuntimeException("could not read/write svn directory access file: " + svnDirAccessFile);
                    }
                    is = new FileInputStream(svnDirAccess);
                    if (is == null) {
                        throw new RuntimeException("could not locate svn directory access file: " + svnDirAccessFile);
                    }
                }
            }
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            int indexOfEqual = -1;
            String groupName = null;
            boolean groupsFound = false;
            String tempLine = null;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (groupsFound == false && line.trim().equalsIgnoreCase("[groups]")) {
                    groupsFound = true;
                    continue;
                }
                if (groupsFound) {
                    indexOfEqual = line.indexOf('=');
                    if (indexOfEqual != -1) {
                        groupName = line.substring(0, indexOfEqual);
                        groupName = groupName.trim();
                        groupList.add(groupName);
                        membersForGroupList = new ArrayList<String>();
                        String membersCSV = line.substring(indexOfEqual + 1, line.length());
                        StringTokenizer st = new StringTokenizer(membersCSV.trim(), ",");
                        while (st.hasMoreTokens()) {
                            String member = st.nextToken();
                            if (!membersForGroupList.contains(member.trim())) {
                                membersForGroupList.add(member.trim());
                            }
                        }
                        LOG.debug("adding members : " + membersForGroupList + ", for groupName: " + groupName);
                        groupMemberMap.put(groupName, membersForGroupList);
                    } else if (line.trim().equals("")) {
                        continue;
                    } else if (line.indexOf('[') != -1) {
                        tempLine = line;
                        break;
                    }
                }
            }
            Map<String, List<String>> projectAccessMap = new HashMap<String, List<String>>();
            List<String> groupPathsList = new ArrayList<String>();
            String path = null;
            boolean pathFound = false;
            for (String line = tempLine; line != null; line = br.readLine()) {
                line = line.trim();
                if (line.startsWith("[/")) {
                    if (pathFound) {
                        projectAccessMap.put(path, groupPathsList);
                    }
                    groupPathsList = new ArrayList<String>();
                    path = line.substring(1, line.indexOf(']'));
                    LOG.debug("path found: " + path);
                    pathFound = true;
                    continue;
                } else if (!line.equals("")) {
                    groupPathsList.add(line);
                }
            }
            if (pathFound) {
                projectAccessMap.put(path, groupPathsList);
            }
            this.groupMembersMap = groupMemberMap;
            this.projectAccessMap = projectAccessMap;
        } finally {
            try {
                is.close();
                is = null;
            } catch (Exception ignore) {
            }
        }
    }

    /**
     *
     * @see com.wkinney.client.SVNAdminService#addUser()
     */
    public void addUser(String username, String password) {
        if (username == null || username.trim().equals("")) {
            throw new IllegalArgumentException("username cannot be null or empty string");
        }
        if (password == null || password.trim().equals("")) {
            throw new IllegalArgumentException("password cannot be null or empty string");
        }
        username = username.trim();
        if (this.memberList != null && this.memberList.size() > 0) {
            for (String member : memberList) {
                if (member.equalsIgnoreCase(username)) {
                    throw new IllegalArgumentException("username: " + username + " is already a member");
                }
            }
        }
        try {
            addUserOnly(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("error adding username: " + username + ", cause: " + e.getMessage(), e);
        }
        loadData(true);
    }

    /**
     *
     * @see com.wkinney.client.SVNAdminService#editUser()
     */
    public void editUser(String oldUsername, String username, String password) {
        if (oldUsername == null || oldUsername.trim().equals("")) {
            throw new IllegalArgumentException("oldUsername cannot be null or empty string");
        }
        if (username == null || username.trim().equals("")) {
            throw new IllegalArgumentException("username cannot be null or empty string");
        }
        if (password == null || password.trim().equals("")) {
            throw new IllegalArgumentException("password cannot be null or empty string");
        }
        oldUsername = oldUsername.trim();
        username = username.trim();
        boolean foundUser = false;
        if (this.memberList != null && this.memberList.size() > 0) {
            for (String member : memberList) {
                if (member.equalsIgnoreCase(oldUsername)) {
                    foundUser = true;
                    break;
                }
            }
        }
        if (!foundUser) {
            throw new RuntimeException("Could not find user to delete, username: " + oldUsername);
        }
        if (!oldUsername.equalsIgnoreCase(username)) {
            boolean foundUserAgain = false;
            if (this.memberList != null && this.memberList.size() > 0) {
                for (String member : memberList) {
                    if (member.equalsIgnoreCase(username) && !member.equalsIgnoreCase(oldUsername)) {
                        foundUserAgain = true;
                        break;
                    }
                }
            }
            if (foundUserAgain) {
                throw new RuntimeException("New username already exists: " + username);
            }
        }
        try {
            deleteUserOnly(oldUsername);
            loadUserMemberList();
            addUser(username, password);
            updateAccessUser(oldUsername, username);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("error updating username: " + username + ", cause: " + e.getMessage(), e);
        }
        saveData();
        loadData(true);
    }

    private void updateAccessUser(String oldUsername, String newUserName) {
        if (oldUsername.equals(newUserName)) {
            LOG.debug("skipping update access user since username didn't change");
            return;
        }
        for (Iterator<String> i = this.groupMembersMap.keySet().iterator(); i.hasNext(); ) {
            String group = i.next();
            List<String> userList = this.groupMembersMap.get(group);
            if (userList.contains(oldUsername)) {
                userList.remove(oldUsername);
                userList.add(newUserName);
            }
            this.groupMembersMap.put(group, userList);
        }
    }

    @Override
    public void addMembership(String groupName, List<String> userList) {
        if (groupName == null || groupName.trim().equals("")) {
            throw new IllegalArgumentException("group name cannot be null or empty string");
        }
        if (userList == null || userList.size() == 0) {
            throw new IllegalArgumentException("user list cannot be null or empty");
        }
        groupName = groupName.trim();
        if (this.groupMembersMap == null) {
            throw new IllegalStateException("group member map has not been initialized");
        }
        if (!this.groupMembersMap.containsKey(groupName)) {
            addGroup(groupName);
        }
        if (!this.memberList.containsAll(userList)) {
            throw new RuntimeException("member list to add: " + userList + " contains 1 or more users who do not exist");
        }
        List<String> memberList = this.groupMembersMap.get(groupName);
        if (memberList == null) {
            memberList = new ArrayList();
        }
        LOG.debug("adding members : " + userList + ", to group: " + groupName);
        for (String user : userList) {
            if (!memberList.contains(user)) {
                memberList.add(user);
            }
        }
        this.groupMembersMap.put(groupName, memberList);
        saveData();
        loadData(true);
    }

    public void removeMembership(List<Membership> memberships) {
        if (memberships == null || memberships.size() == 0) {
            throw new IllegalArgumentException("memberships is null or empty");
        }
        Set<Membership> tempMemberships = new HashSet<Membership>(memberships);
        Map<String, Set<String>> memberListMap = new HashMap<String, Set<String>>(tempMemberships.size());
        String group = null;
        String member = null;
        for (Iterator<Membership> i = tempMemberships.iterator(); i.hasNext(); ) {
            Membership m = i.next();
            group = m.getGroup();
            member = m.getMember();
            if (group == null || group.trim().equals("")) {
                throw new IllegalArgumentException("group name cannot be null or empty string");
            }
            if (member == null || member.trim().equals("")) {
                throw new IllegalArgumentException("member name cannot be null or empty string");
            }
            Set<String> memberList = null;
            if (memberListMap.containsKey(group)) {
                memberList = memberListMap.get(group);
            } else {
                memberList = new HashSet<String>();
            }
            memberList.add(member);
            memberListMap.put(group, memberList);
        }
        for (Iterator<String> it = memberListMap.keySet().iterator(); it.hasNext(); ) {
            String groupName = it.next();
            Set<String> userList = memberListMap.get(groupName);
            if (userList == null || userList.size() == 0) {
                throw new IllegalArgumentException("user list cannot be null or empty");
            }
            groupName = groupName.trim();
            if (this.groupMembersMap == null) {
                throw new IllegalStateException("group member map has not been initialized");
            }
            if (!this.groupMembersMap.containsKey(groupName)) {
                throw new RuntimeException("group does not exist: " + groupName);
            }
            List<String> memberList = this.groupMembersMap.get(groupName);
            if (memberList == null || memberList.size() == 0) {
                throw new RuntimeException("cannot remove membership group current member list is empty. group: " + groupName);
            }
            LOG.debug("removing members : " + userList + ", from group: " + groupName);
            for (Iterator i = userList.iterator(); i.hasNext(); ) {
                String memberToRemove = (String) i.next();
                if (!memberList.contains(memberToRemove)) {
                    throw new RuntimeException("member to remove: " + memberToRemove + " does not currently belong to the group: " + groupName);
                }
            }
            for (Iterator i = userList.iterator(); i.hasNext(); ) {
                String memberToRemove = (String) i.next();
                memberList.remove(memberToRemove);
            }
            this.groupMembersMap.put(groupName, memberList);
        }
        saveData();
        loadData(true);
    }

    public void saveDataToFile() throws IOException {
        final String saveFile = SVNAdminServiceProperties.getUserDirectoryAccessFile();
        if (saveFile == null || saveFile.trim().equals("")) {
            throw new RuntimeException("htpasswd access file is null or empty string");
        }
        BufferedWriter bw = null;
        try {
            File authFile = new File(saveFile);
            if (!authFile.exists()) {
                LOG.debug("htpasswd access file: " + saveFile + " does not exist, creating new");
            }
            if (!authFile.canWrite()) {
                throw new RuntimeException("cannot write htpasswd access file: " + saveFile + ", no write privledges");
            }
            FileWriter fw = new FileWriter(saveFile);
            bw = new BufferedWriter(fw);
            bw.write("[groups]");
            bw.newLine();
            for (Iterator<String> i = this.groupMembersMap.keySet().iterator(); i.hasNext(); ) {
                String groupName = i.next();
                List<String> memberList = this.groupMembersMap.get(groupName);
                String memberString = null;
                for (String member : memberList) {
                    if (memberString == null) {
                        memberString = member;
                    } else {
                        memberString += ", " + member;
                    }
                }
                if (memberString == null) {
                    memberString = "";
                }
                bw.write(groupName + " = " + memberString);
                bw.newLine();
            }
            bw.newLine();
            for (Iterator<String> i = this.projectAccessMap.keySet().iterator(); i.hasNext(); ) {
                String projectPath = i.next();
                List<String> accessStringList = this.projectAccessMap.get(projectPath);
                bw.write("[" + projectPath + "]");
                bw.newLine();
                for (String accessString : accessStringList) {
                    bw.write(accessString);
                    bw.newLine();
                }
                bw.newLine();
            }
            bw.write("");
            bw.newLine();
        } finally {
            bw.close();
        }
    }

    public void updateProjectAccess(String projectPath, Map updatedProjectAccessMap) {
        Map<String, String> tempUpdatedProjectAccessMap = (Map<String, String>) updatedProjectAccessMap;
        if (projectPath == null || projectPath.trim().equals("")) {
            throw new IllegalArgumentException("project path cannot be null or empty string");
        }
        if (tempUpdatedProjectAccessMap == null) {
            throw new IllegalArgumentException("updated project access map cannot be null");
        }
        if (this.projectAccessMap == null) {
            throw new IllegalStateException("project access map has not been initialized");
        }
        projectPath = projectPath.trim();
        emptyAccessForProject(projectPath);
        List<String> accessList = new ArrayList<String>();
        for (Iterator<String> groupIt = tempUpdatedProjectAccessMap.keySet().iterator(); groupIt.hasNext(); ) {
            String groupName = groupIt.next();
            if (!validateGroupName(groupName)) {
                throw new IllegalArgumentException("group name is not valid: " + groupName);
            }
            String access = tempUpdatedProjectAccessMap.get(groupName);
            if (!validateAccess(access)) {
                throw new IllegalArgumentException("access value is not valid (must be r, rw, or w)");
            }
            String accessLine = createAccessLine(groupName, access);
            accessList.add(accessLine);
        }
        LOG.debug("saving accessList: " + accessList + " for projectPath: " + projectPath);
        this.projectAccessMap.put(projectPath, accessList);
        saveData();
        loadData(true);
    }

    public void removeProjectAccess(String projectPath, String groupName, String accessType) {
        if (projectPath == null || projectPath.trim().equals("")) {
            throw new IllegalArgumentException("project path cannot be null or empty string");
        }
        if (groupName == null || groupName.trim().equals("")) {
            throw new IllegalArgumentException("group name cannot be null or empty string");
        }
        if (this.projectAccessMap == null) {
            throw new IllegalStateException("project access map has not been initialized");
        }
        projectPath = projectPath.trim();
        groupName = groupName.trim();
        if (!this.projectAccessMap.containsKey(projectPath)) {
            throw new RuntimeException("could not remove group for project path, project path not found");
        }
        List<String> accessList = this.projectAccessMap.get(projectPath);
        String accessLine = createAccessLine(groupName, accessType);
        if (!accessList.contains(accessLine)) {
            throw new RuntimeException("could not remove group: " + groupName + ", group and access type not found in project path: " + projectPath);
        }
        accessList.remove(accessLine);
        this.projectAccessMap.put(projectPath, accessList);
        saveData();
        loadData(true);
    }

    private boolean validateGroupName(String groupName) {
        if (groupName == null || groupName.trim().equals("") || (!groupName.equals("*") && !this.groupMembersMap.containsKey(groupName))) {
            return false;
        }
        return true;
    }

    private boolean validateAccess(String access) {
        if (access == null || (!access.equals("r") && !access.equals("rw") && !access.equals("w"))) {
            return false;
        }
        return true;
    }

    private void saveData() {
        LOG.debug("Saving configuration...");
        try {
            saveDataToFile();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("error saving data, cause: " + e.getMessage(), e);
        }
    }

    private void emptyAccessForProject(String projectPath) {
        if (projectPath == null || projectPath.trim().equals("")) {
            throw new IllegalArgumentException("project path cannot be null or empty string");
        }
        projectPath = projectPath.trim();
        if (this.projectAccessMap == null) {
            throw new IllegalStateException("project access map has not been initialized");
        }
        LOG.debug("emptying project access list for project path: " + projectPath);
        this.projectAccessMap.put(projectPath, new ArrayList<String>());
    }

    public void addProjectAccess(String projectPath, String groupName, String access) {
        if (projectPath == null || projectPath.trim().equals("")) {
            throw new IllegalArgumentException("project path cannot be null or empty string");
        }
        if (!validateGroupName(groupName)) {
            throw new IllegalArgumentException("group name is not valid: " + groupName);
        }
        if (!validateAccess(access)) {
            throw new IllegalArgumentException("access value is not valid (must be r, rw, or w)");
        }
        if (this.projectAccessMap == null) {
            throw new IllegalStateException("project access map has not been initialized");
        }
        projectPath = projectPath.trim();
        List<String> accessList = this.projectAccessMap.get(projectPath);
        if (accessList == null) {
            accessList = new ArrayList<String>();
        }
        String accessLine = createAccessLine(groupName, access);
        Map<String, String> accessMap = parseProjectRightsString(accessList);
        if (accessMap.containsKey(groupName)) {
            String accessToRemove = accessMap.get(groupName);
            String lineToRemove = createAccessLine(groupName, accessToRemove);
            accessList.remove(lineToRemove);
        }
        accessList.add(accessLine);
        LOG.debug("adding project access for project path: " + projectPath + ". access line: " + accessLine);
        this.projectAccessMap.put(projectPath, accessList);
        saveData();
        loadData(true);
    }

    public String createAccessLine(String groupName, String access) {
        if (groupName == null || groupName.trim().equals("")) {
            throw new IllegalArgumentException("group name cannot be null or empty string");
        }
        if (!validateAccess(access)) {
            throw new IllegalArgumentException("access value is not valid (must be r, rw, or w)");
        }
        String accessLine = null;
        if (groupName.equals("*")) {
            accessLine = groupName + " = " + access;
        } else {
            accessLine = "@" + groupName + " = " + access;
        }
        return accessLine;
    }

    public void addProject(String projectPath) {
        if (projectPath == null || projectPath.trim().equals("")) {
            throw new IllegalArgumentException("project path cannot be null or empty string");
        }
        projectPath = projectPath.trim();
        if (this.projectAccessMap == null) {
            throw new IllegalStateException("project access map has not been initialized");
        }
        if (this.projectAccessMap.containsKey(projectPath)) {
            throw new RuntimeException("project path already exists: " + projectPath);
        }
        LOG.debug("adding project to access map: " + projectPath);
        this.projectAccessMap.put(projectPath, new ArrayList<String>());
        saveData();
        loadData(true);
    }

    /**
     * Wrapper for removeProject(Set)
     *
     */
    public void removeProject(String projectPath) {
        List projectPathList = new ArrayList(1);
        projectPathList.add(projectPath);
        removeProject(projectPathList);
    }

    @Override
    public void removeProject(List projectPathList) {
        if (projectPathList == null || projectPathList.size() == 0) {
            throw new IllegalArgumentException("project path Set cannot be null or empty");
        }
        for (Iterator i = projectPathList.iterator(); i.hasNext(); ) {
            String projectPath = (String) i.next();
            if (projectPath == null || projectPath.trim().equals("")) {
                throw new IllegalArgumentException("project path cannot be null or empty string");
            }
            projectPath = projectPath.trim();
            if (this.projectAccessMap == null) {
                throw new IllegalStateException("project access map has not been initialized");
            }
            if (!this.projectAccessMap.containsKey(projectPath)) {
                throw new RuntimeException("project path does not exist: " + projectPath + ", cannot remove");
            }
            LOG.debug("removing project from access map: " + projectPath);
            this.projectAccessMap.remove(projectPath);
        }
        saveData();
        loadData(true);
    }

    public void addGroup(String groupName) {
        if (groupName == null || groupName.trim().equals("")) {
            throw new IllegalArgumentException("group name cannot be null or empty string");
        }
        groupName = groupName.trim();
        if (this.groupMembersMap == null) {
            throw new IllegalStateException("group member map has not been initialized");
        }
        if (this.groupMembersMap.containsKey(groupName)) {
            throw new RuntimeException("group already exists: " + groupName);
        }
        LOG.debug("adding group to member map: " + groupName);
        this.groupMembersMap.put(groupName, new ArrayList<String>());
        saveData();
        loadData(true);
    }

    public void deleteGroup(String groupName) {
        if (groupName == null || groupName.trim().equals("")) {
            throw new IllegalArgumentException("group name cannot be null or empty string");
        }
        groupName = groupName.trim();
        if (this.groupMembersMap == null) {
            throw new IllegalStateException("group member map has not been initialized");
        }
        if (!this.groupMembersMap.containsKey(groupName)) {
            throw new RuntimeException("group does not exist: " + groupName);
        }
        List<String> memberList = this.groupMembersMap.get(groupName);
        if (memberList != null && memberList.size() > 0) {
            throw new RuntimeException("cannot delete group becuase it has members. group: " + groupName);
        }
        LOG.debug("removing group to member from map: " + groupName);
        this.groupMembersMap.remove(groupName);
        saveData();
        loadData(true);
    }

    /**
     *
     * @see com.wkinney.client.SVNAdminService#deleteUser()
     */
    public void deleteUser(String username) {
        if (username == null || username.trim().equals("")) {
            throw new IllegalArgumentException("username cannot be null or empty string");
        }
        username = username.trim();
        boolean foundUser = false;
        if (this.memberList != null && this.memberList.size() > 0) {
            for (String member : memberList) {
                if (member.equalsIgnoreCase(username)) {
                    foundUser = true;
                    break;
                }
            }
        }
        if (!foundUser) {
            throw new RuntimeException("Could not find user to delete, username: " + username);
        }
        if (this.groupMembersMap == null) {
            throw new IllegalStateException("group member map has not been initialized");
        }
        try {
            deleteUserOnly(username);
            for (Iterator<String> i = this.groupMembersMap.keySet().iterator(); i.hasNext(); ) {
                String groupName = i.next();
                List<String> memberList = this.groupMembersMap.get(groupName);
                if (memberList.contains(username)) {
                    LOG.debug("removing member: " + username + " from group: " + groupName);
                    memberList.remove(username);
                }
            }
            saveData();
            loadData(true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("error deleting username: " + username + ", cause: " + e.getMessage(), e);
        }
    }

    private void addUserOnly(String username, String password) throws IOException, InterruptedException {
        String htpasswdScript = SVNAdminServiceProperties.getHtpasswdScript();
        String htpasswdFlags = SVNAdminServiceProperties.getHtpasswdFlags();
        String htpasswdAuthFile = SVNAdminServiceProperties.getHtpasswdUserAuthFile();
        File authFile = new File(htpasswdAuthFile);
        if (!authFile.exists()) {
            htpasswdFlags = " -c " + htpasswdFlags;
        } else {
            if (!authFile.canRead() || !authFile.canWrite()) {
                throw new RuntimeException("Can't read/write htpasswd authorization file: " + htpasswdAuthFile);
            }
        }
        final String batchModeFlag = " -b ";
        htpasswdFlags = batchModeFlag + htpasswdFlags;
        final String cmd = htpasswdScript + " " + htpasswdFlags + " " + htpasswdAuthFile + " " + username + " " + password;
        String envp[] = new String[1];
        envp[0] = "PATH=" + System.getProperty("java.library.path");
        LOG.debug("executing cmd: " + cmd + ", with PATH: " + envp[0]);
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(cmd, envp);
        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line = null;
        while ((line = input.readLine()) != null) {
            LOG.debug(line);
        }
        int exitVal = pr.waitFor();
        LOG.debug("Exited with code " + exitVal);
    }

    private void deleteUserOnly(String username) throws IOException, InterruptedException {
        String htpasswdScript = SVNAdminServiceProperties.getHtpasswdScript();
        String htpasswdAuthFile = SVNAdminServiceProperties.getHtpasswdUserAuthFile();
        File authFile = new File(htpasswdAuthFile);
        if (!authFile.exists()) {
            throw new RuntimeException("htpasswd authorization file does not exist, could not remove the username: " + username);
        }
        if (!authFile.canRead() || !authFile.canWrite()) {
            throw new RuntimeException("Can't read/write htpasswd authorization file: " + htpasswdAuthFile);
        }
        final String deleteUserFlag = " -D ";
        final String cmd = htpasswdScript + " " + deleteUserFlag + " " + htpasswdAuthFile + " " + username;
        String envp[] = new String[1];
        envp[0] = "PATH=" + System.getProperty("java.library.path");
        LOG.debug("executing cmd: " + cmd + ", with PATH: " + envp[0]);
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(cmd, envp);
        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line = null;
        while ((line = input.readLine()) != null) {
            LOG.debug(line);
        }
        int exitVal = pr.waitFor();
        LOG.debug("Exited with code " + exitVal);
    }

    /**
     * Helper method
     *
     * @param lineList
     * @return
     */
    public Map<String, String> parseProjectRightsString(List lineList) {
        if (lineList == null || lineList.size() == 0) {
            return new HashMap<String, String>(0);
        }
        Map<String, String> tempMap = new HashMap<String, String>();
        for (Iterator i = lineList.iterator(); i.hasNext(); ) {
            String line = (String) i.next();
            String owner = null;
            if (line.startsWith("@")) {
                owner = line.substring(1, line.indexOf('=')).trim();
            } else {
                owner = line.substring(0, line.indexOf('=')).trim();
            }
            String access = null;
            access = line.substring(line.indexOf('=') + 1, line.length());
            access = access.trim();
            tempMap.put(owner, access);
        }
        return tempMap;
    }
}
