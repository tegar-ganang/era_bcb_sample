package bioroot.antibody;

import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import bioroot.*;
import bioroot.strain.StrainBean;
import java.io.*;
import java.util.*;
import util.gen.*;

/**Servlet to modify antibodys using antibodyBeans referenced from a Session object.
 * @author nix
 */
public class AntibodyModify extends HttpServlet {

    private static Logger log = Logger.getLogger(AntibodyModify.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        UserBean userBean = Util.fetchUserBean(request, response);
        DBUtil bioRoot = null;
        String message = "";
        String redirectPage = "WEB-INF/Jsps/Antibody/ModifyAntibody.jsp";
        HashMap nameValue = HTML.parseMultiPartRequest(request);
        AntibodyBean antibodyBean = null;
        AntibodyBean[] antibodyBeans = null;
        boolean antibodiesPresent = nameValue.containsKey("antibodiesPresent");
        if (antibodiesPresent) {
            antibodyBeans = (AntibodyBean[]) session.getAttribute("antibodyBeansToEdit");
            if (antibodyBeans != null) antibodyBean = antibodyBeans[0];
            redirectPage = "WEB-INF/Jsps/Antibody/ModifyAntibodies.jsp";
        } else antibodyBean = (AntibodyBean) session.getAttribute("antibodyBean");
        if (antibodyBean == null) antibodyBean = new AntibodyBean();
        antibodyBean.setMessages("");
        boolean editAntibody = nameValue.containsKey("editAntibodyBean");
        boolean deleteAntibody = nameValue.containsKey("deleteAntibodyBean");
        boolean updateAntibody = nameValue.containsKey("updateAntibodyBean");
        boolean resetAntibody = nameValue.containsKey("resetAntibodyBean");
        boolean removePlasmid = nameValue.containsKey("removePlasmid");
        boolean addPlasmid = nameValue.containsKey("addPlasmid");
        boolean removeStrain = nameValue.containsKey("removeStrain");
        boolean addStrain = nameValue.containsKey("addStrain");
        boolean addAnotherPrep = nameValue.containsKey("addAnotherPrep");
        int deleteAntibodyPrep = AntibodyBase.deleteAntibodyPrepIndex(antibodyBean, bioRoot, nameValue);
        int addUseDilutionTo = AntibodyBase.newUseDilutionIndex(antibodyBean, bioRoot, nameValue);
        String deleteUseDilution = AntibodyBase.deleteUseDilutionIndex(antibodyBean, bioRoot, nameValue);
        boolean next = nameValue.containsKey("next");
        String goTo = (String) nameValue.get("goTo");
        if (Misc.isNotEmpty(goTo)) {
            bioRoot = new DBUtil("orgbioro_bioroot");
            NewAntibody.loadAntibodyBean(nameValue, bioRoot, antibodyBean, request);
            if (goTo.equals("newPlasmid")) {
                redirectPage = "WEB-INF/Jsps/Plasmid/NewPlasmid.jsp";
            } else if (goTo.equals("newOrganism")) {
                redirectPage = "WEB-INF/Jsps/Util/Organism.jsp";
            } else if (goTo.equals("newMarker")) {
                redirectPage = "WEB-INF/Jsps/Util/Marker.jsp";
            } else if (goTo.equals("newStrain")) {
                redirectPage = "WEB-INF/Jsps/Strain/NewStrain.jsp";
            } else if (goTo.equals("newGene")) {
                redirectPage = "WEB-INF/Jsps/Util/Gene.jsp";
            }
        } else if (next) {
            session.removeAttribute("antibodyModifyMessage");
            subtractAndSetAntibodyBeans(antibodyBeans, session);
        } else if (resetAntibody) {
            if (antibodiesPresent) redirectPage = "AntibodySpreadSheet"; else redirectPage = "WEB-INF/Jsps/Antibody/NewAntibody.jsp";
            session.removeAttribute("antibodyBean");
            session.removeAttribute("antibodyBeansToEdit");
        } else if (editAntibody) {
            redirectPage = "WEB-INF/Jsps/Antibody/ModifyAntibody.jsp";
        } else if (addAnotherPrep) {
            NewAntibody.loadAntibodyBean(nameValue, bioRoot, antibodyBean, request);
            NewAntibody.addAnAntibodyPrep(antibodyBean, bioRoot);
        } else if (addUseDilutionTo != -1) {
            NewAntibody.loadAntibodyBean(nameValue, bioRoot, antibodyBean, request);
            NewAntibody.addUseDilution(antibodyBean, bioRoot, addUseDilutionTo);
        } else if (userBean.isGuestOrFriend() || (userBean.getLabGroupId() != antibodyBean.getLabGroupId())) {
            message = "Sorry, you lack the requisit permissions needed to modify this antibody.";
            session.setAttribute("antibodyModifyMessage", message);
        } else {
            bioRoot = new DBUtil("orgbioro_bioroot");
            NewAntibody.loadAntibodyBean(nameValue, bioRoot, antibodyBean, request);
            if (removePlasmid) {
                AntibodyBase.removePlasmid(antibodyBean, nameValue, bioRoot);
            } else if (addPlasmid) {
                AntibodyBase.addPlasmid(antibodyBean, nameValue, bioRoot, userBean);
            } else if (removeStrain) {
                AntibodyBase.removeStrain(antibodyBean, nameValue, bioRoot);
            } else if (addStrain) {
                AntibodyBase.addStrain(antibodyBean, nameValue, bioRoot, userBean);
            } else if (updateAntibody) {
                if ((userBean.isSuperUser() && userBean.getLabGroupId() == antibodyBean.getLabGroupId()) || userBean.getId() == antibodyBean.getOwnerId()) {
                    message = updateAntibody(antibodyBean, userBean, bioRoot);
                    if (Misc.isEmpty(message)) {
                        message = updateAntibodyPreps(antibodyBean, bioRoot);
                        if (Misc.isEmpty(message)) {
                            session.removeAttribute("antibodyBeans");
                            if (antibodiesPresent) {
                                if (subtractAndSetAntibodyBeans(antibodyBeans, session)) {
                                    session.removeAttribute("antibodyBeansToEdit");
                                    session.removeAttribute("antibodyModifyMessage");
                                    session.setAttribute("antibodySpreadSheetMessage", "Updated!");
                                    redirectPage = "AntibodySpreadSheet";
                                } else {
                                    session.setAttribute("antibodyModifyMessage", "Updated");
                                }
                            } else {
                                message = "Updated!";
                                redirectPage = "WEB-INF/Jsps/Antibody/NewAntibody.jsp";
                            }
                        } else {
                            message = "An error occured while attempting to update an antibody prep. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
                            log.error(message);
                        }
                    } else {
                        message = "An error occured while attempting to update an antibody. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
                        log.error(message);
                    }
                } else {
                    boolean updated = updateCommonAccessFields(antibodyBean.getAntibodyPreps(bioRoot), bioRoot);
                    if (updated) {
                        session.removeAttribute("antibodyBeans");
                        if (antibodiesPresent) {
                            if (subtractAndSetAntibodyBeans(antibodyBeans, session)) {
                                session.removeAttribute("antibodyBeansToEdit");
                                session.removeAttribute("antibodyModifyMessage");
                                session.setAttribute("antibodySpreadSheetMessage", "Updated common access fields.");
                                redirectPage = "AntibodySpreadSheet";
                            } else session.setAttribute("antibodyModifyMessage", "Updated common access fields.");
                        } else {
                            message = "Updated common access fields!";
                            redirectPage = "WEB-INF/Jsps/Antibody/NewAntibody.jsp";
                        }
                    } else {
                        message = "An error occured while attempting to update an antibody's common access fields. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
                        log.error(message);
                    }
                }
                antibodyBean.setMessages(message);
            } else if (deleteAntibody) {
                message = deleteAntibody(antibodyBean, userBean, bioRoot);
                if (Misc.isEmpty(message)) {
                    session.removeAttribute("antibodyBeans");
                    antibodyBean = new AntibodyBean();
                    antibodyBean.setMessages("Deleted!");
                    session.setAttribute("antibodyBean", antibodyBean);
                } else {
                    antibodyBean.setMessages(message);
                }
                redirectPage = "WEB-INF/Jsps/Antibody/NewAntibody.jsp";
            } else if (deleteAntibodyPrep != -1) {
                AntibodyPrep[] preps = antibodyBean.getAntibodyPreps(bioRoot);
                if ((userBean.isSuperUser() && antibodyBean.getLabGroupId() == userBean.getLabGroupId()) || antibodyBean.getOwnerId() == userBean.getId()) {
                    String sql = "DELETE FROM AntibodyPrep WHERE id=" + preps[deleteAntibodyPrep].getId();
                    if (bioRoot.executeSQLUpdate(sql)) {
                        if (Misc.isNotEmpty(preps[deleteAntibodyPrep].getFileName())) {
                            File tmp = new File(Util.filepath + "Upload/AntibodyFiles/" + preps[deleteAntibodyPrep].getFileName());
                            if (tmp.delete() == false) {
                                message = "An error occured while attempting to delete an antibody prep's associated file. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
                                log.error(message);
                            }
                        }
                        ArrayList al = Misc.objectArrayToArrayList(preps);
                        al.remove(deleteAntibodyPrep);
                        preps = new AntibodyPrep[al.size()];
                        al.toArray(preps);
                        antibodyBean.setAntibodyPreps(preps);
                    }
                } else {
                    message = "You lack the necessary privileges for deletion.  Contact the antibody owner or a lab super user.";
                }
                antibodyBean.setMessages(message);
            } else if (deleteUseDilution != null) {
                if ((userBean.isSuperUser() && antibodyBean.getLabGroupId() == userBean.getLabGroupId()) || antibodyBean.getOwnerId() == userBean.getId()) {
                    String[] indexes = deleteUseDilution.split("\\.");
                    int prepIndex = Integer.parseInt(indexes[0]);
                    int useIndex = Integer.parseInt(indexes[1]);
                    AntibodyPrep prep = antibodyBean.getAntibodyPreps(bioRoot)[prepIndex];
                    UseDilution[] uses = prep.getUseDilutions(bioRoot);
                    String sql = "DELETE FROM UseDilution WHERE id=" + uses[useIndex].getId();
                    if (bioRoot.executeSQLUpdate(sql)) {
                        ArrayList al = Misc.objectArrayToArrayList(uses);
                        al.remove(useIndex);
                        uses = new UseDilution[al.size()];
                        al.toArray(uses);
                        prep.setUseDilutions(uses);
                    }
                } else {
                    message = "You lack the necessary privileges for deletion.  Contact the antibody owner or a lab super user.";
                }
                antibodyBean.setMessages(message);
            }
        }
        if (bioRoot != null) bioRoot.closeConnection();
        RequestDispatcher dispatcher = request.getRequestDispatcher(redirectPage);
        dispatcher.forward(request, response);
    }

    /**Attempts to delete an antibody, if sucessful, returns a "" String, otherwise an error message.
	 * Also deletes the associated antibody preps, use dilutions, strain and plasmid references. 
	 * All associated files are deleted too.
	 * If you change the wording of the error messages, update them in the AntibodySpreadSheet yesDelete section.*/
    public static String deleteAntibody(AntibodyBean ab, UserBean userBean, DBUtil bioRoot) {
        String message = "";
        String sql = "DELETE FROM Antibody WHERE id=" + ab.getId() + " AND (ownerId=" + userBean.getId();
        if ((userBean.isSuperUser() && ab.getLabGroupId() == userBean.getLabGroupId())) sql = sql + " or labGroupId=" + userBean.getLabGroupId();
        sql += ")";
        if (bioRoot.executeSQLUpdate(sql)) {
            if (bioRoot.getRowsEffected() == 0) return "You lack the necessary privileges for deletion.  Contact the antibody owner or a lab super user."; else {
                if (Misc.isNotEmpty(ab.getFileName())) {
                    File tmp = new File(Util.filepath + "Upload/AntibodyFiles/" + ab.getFileName());
                    if (tmp.delete() == false) {
                        message = "An error occured while attempting to delete an antibody's associated files. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
                        log.error(message);
                        return message;
                    }
                }
                AntibodyPrep[] preps = ab.getAntibodyPreps(bioRoot);
                ArrayList ids = new ArrayList();
                for (int i = 0; i < preps.length; i++) {
                    if (Misc.isNotEmpty(preps[i].getFileName())) {
                        File tmp = new File(Util.filepath + "Upload/AntibodyFiles/" + preps[i].getFileName());
                        if (tmp.delete() == false) {
                            message = "An error occured while attempting to delete an antibody prep's associated file. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
                            log.error(message);
                            return message;
                        }
                    }
                    ids.add(preps[i].getId() + " ");
                }
                String idsList = Misc.stringArrayListToString(ids, ",");
                if (Misc.isNotEmpty(idsList)) {
                    sql = "DELETE FROM AntibodyPrep WHERE id IN (" + idsList + ")";
                    if (bioRoot.executeSQLUpdate(sql) == false) {
                        message = "An error occured while attempting to delete an antibody's preps. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
                        log.error(message + "\n" + sql);
                        return message;
                    }
                    sql = "DELETE FROM UseDilution WHERE id IN (" + idsList + ")";
                    if (bioRoot.executeSQLUpdate(sql) == false) {
                        message = "An error occured while attempting to delete an antibody prep's use dilutions. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
                        log.error(message + "\n" + sql);
                        return message;
                    }
                }
                sql = "DELETE FROM AntibodyStrain WHERE antibodyId = " + ab.getId();
                bioRoot.executeSQLUpdate(sql);
                sql = "DELETE FROM AntibodyPlasmid WHERE antibodyId = " + ab.getId();
                bioRoot.executeSQLUpdate(sql);
            }
        } else {
            message = "An error occured while attempting to delete a antibody. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
            log.error(message + "\n" + sql);
        }
        return message;
    }

    /**Just update the commonly accessible fields in each antibodyPrep as well as the useDilutions.*/
    public static boolean updateCommonAccessFields(AntibodyPrep[] preps, DBUtil bioRoot) {
        for (int i = 0; i < preps.length; i++) {
            if (preps[i].updateCommonAccessFields(bioRoot) == false) return false;
            UseDilution[] useDilutions = preps[i].getUseDilutions(bioRoot);
            for (int j = 0; j < useDilutions.length; j++) {
                if (useDilutions[j].updateOld(bioRoot) == false) return false;
            }
        }
        return true;
    }

    /**Attempts to update the AntibodyPreps and UseDilutions in the database.
	 * If the update is sucessful, the message will be "", otherwise an error message will be returned.*/
    public static String updateAntibodyPreps(AntibodyBean bean, DBUtil bioRoot) {
        AntibodyPrep[] preps = bean.getAntibodyPreps(bioRoot);
        for (int i = 0; i < preps.length; i++) {
            boolean updated = true;
            if (preps[i].getId() == 0) {
                preps[i].setAntibodyId(bean.getId());
                if (Misc.isNotEmpty(preps[i].getName())) updated = preps[i].submitNew(bioRoot);
            } else {
                updated = preps[i].updateOld(bioRoot);
                if (Misc.isNotEmpty(preps[i].getOldFileName())) {
                    File tmp = new File(Util.filepath + "Upload/AntibodyFiles/" + preps[i].getOldFileName());
                    tmp.delete();
                }
            }
            if (updated == false) {
                File tmp = new File(Util.filepath + "Upload/AntibodyFiles/" + preps[i].getFileName());
                tmp.delete();
                return "An error occured while attempting to update an antibody prep. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
            }
            UseDilution[] uses = preps[i].getUseDilutions(bioRoot);
            for (int j = 0; j < uses.length; j++) {
                if (uses[j].getId() != 0) uses[j].updateOld(bioRoot); else {
                    if (Misc.isNotEmpty(uses[j].getAntibodyUse())) {
                        uses[j].submitNew(bioRoot, preps[i].getId());
                    }
                }
            }
        }
        return "";
    }

    /**Attempts to update a AntibodyBean in the database, only checks that the text is unique if it has changed.
	 * If the update is sucessful, the message will be "", otherwise an error message will be returned.*/
    public static String updateAntibody(AntibodyBean bean, UserBean userBean, DBUtil bioRoot) {
        if (bean.isNameChanged()) {
            log.error("Updating Antibody...Name has changed!");
            if (Misc.isEmpty(bean.getName())) {
                return "You must provide a antibody text.";
            }
            if (bean.isAntibodyNameUnique(bioRoot) == false) {
                log.error("Name is not unique!");
                return "That antibody text is in use, choose another.";
            }
        }
        if (Misc.isNotEmpty(bean.getFileName()) && bean.getFileName().startsWith("temp")) {
            File tmp = new File(Util.filepath + "Upload/AntibodyFiles/" + bean.getFileName());
            File real = new File((Util.filepath + "Upload/AntibodyFiles/" + bean.getFileName().substring(4)));
            tmp.renameTo(real);
            bean.setFileName(real.getName());
        }
        if (Misc.isNotEmpty(bean.getOrganismName(bioRoot))) {
            bean.setOrganismId(bioRoot.getOrganismId(bean.getOrganismName(bioRoot)));
        } else bean.setOrganismId(0);
        if (Misc.isNotEmpty(bean.getGeneName(bioRoot))) {
            bean.setGeneId(bioRoot.getGeneId(bean.getGeneName(bioRoot), userBean.getLabGroupId()));
        } else bean.setGeneId(0);
        if (bean.updateOld(bioRoot, userBean)) {
            bean.setMessages("Saved");
            bean.setComplete(true);
            bean.setNameChanged(false);
            if (Misc.isNotEmpty(bean.getOldFileName())) {
                File tmp = new File(Util.filepath + "Upload/AntibodyFiles/" + bean.getOldFileName());
                tmp.delete();
            }
            return "";
        } else {
            File tmp = new File(Util.filepath + "Upload/AntibodyFiles/" + bean.getFileName());
            tmp.delete();
            return "An error occured while attempting to update a antibody. Please " + "contact BioRoot ASAP! (TimeStamp: " + new java.util.Date() + ")<br>";
        }
    }

    /**Subtracts a bean from the AntibodyBean[] and resets it in antibodyBeansToEdit*/
    public static boolean subtractAndSetAntibodyBeans(AntibodyBean[] beans, HttpSession session) {
        boolean lastBean = false;
        int num = beans.length - 1;
        if (num != 0) {
            AntibodyBean[] minusOneBeans = new AntibodyBean[num];
            for (int i = 0; i < num; i++) {
                minusOneBeans[i] = beans[i + 1];
            }
            session.setAttribute("antibodyBeansToEdit", minusOneBeans);
        } else lastBean = true;
        return lastBean;
    }
}
