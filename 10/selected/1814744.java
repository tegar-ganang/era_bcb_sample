package org.gridtrust.ppm.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gridtrust.Constants;
import org.gridtrust.IndividualState;
import org.gridtrust.ppm.Array_ServiceProvider;
import org.gridtrust.ppm.Array_UserProfile;
import org.gridtrust.ppm.Certificate;
import org.gridtrust.ppm.Policy;
import org.gridtrust.ppm.ServiceProvider;
import org.gridtrust.ppm.UserProfile;
import org.gridtrust.ppm.VO;
import org.gridtrust.ppm.dao.CertificateDao;
import org.gridtrust.ppm.dao.DaoFactory;
import org.gridtrust.ppm.dao.PolicyDao;
import org.gridtrust.ppm.dao.ServiceProviderDao;
import org.gridtrust.ppm.dao.UserDao;
import org.gridtrust.ppm.dao.VODao;
import org.gridtrust.ppm.dto.CertificateDTO;
import org.gridtrust.ppm.impl.policy.normalizer.PolicyNormalizer;
import org.gridtrust.ppm.impl.policy.normalizer.PolicyNormalizerFactory;
import org.gridtrust.util.Configurator;
import org.gridtrust.util.DBUtil;

public class JDBCVODao extends AbstractJDBCDao implements VODao {

    private static final Log log = LogFactory.getLog(JDBCVODao.class);

    public IndividualState createVO(VO vo) {
        IndividualState createState = new IndividualState();
        createState.setServiceId(vo.getVoId());
        Connection con = null;
        PreparedStatement pStmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);
            VO findVO = findVO(vo.getVoId());
            if (findVO == null) {
                int policyId = Constants.EMPTY_POLICY_ID;
                if (vo.getPolicy() != null) {
                    Properties props = Configurator.load(org.gridtrust.Constants.PPM_PROP_STORE_DIR, org.gridtrust.Constants.PPM_PROP_FILE);
                    boolean isNormalized = Boolean.parseBoolean(props.getProperty(Constants.PPM_IS_NORMALISED));
                    if (isNormalized) {
                        log.info("VO policy will be normalized");
                        int bindType = Integer.parseInt(props.getProperty(Constants.PPM_BIND_TYPE));
                        PolicyNormalizer normalizer = PolicyNormalizerFactory.getPolicyNormalizer(bindType);
                        String policyContent = vo.getPolicy().getPolicyContent();
                        policyContent = normalizer.normalizePolicy(policyContent);
                        vo.getPolicy().setPolicyContent(policyContent);
                    }
                    PolicyDao policyDao = DaoFactory.getDaoFactory().getPolicyDao();
                    policyId = policyDao.createPolicy(vo.getPolicy(), Constants.VO_POLICY);
                }
                String sql = "INSERT INTO VO (VO_ID, DESCRIPTION_URL, DESCRIPTION_CONTENT, ADMIN_ID, POLICY_ID, VO_STATUS) VALUES(?, ?, ?, ?, ?, ?)";
                pStmt = con.prepareStatement(sql);
                pStmt.setString(1, vo.getVoId());
                pStmt.setString(2, vo.getDescriptionURL());
                pStmt.setString(3, vo.getDescription());
                pStmt.setString(4, vo.getAdminId());
                pStmt.setInt(5, policyId);
                pStmt.setInt(6, Constants.ACTIVE);
                int count = pStmt.executeUpdate();
                CertificateDao certDao = DaoFactory.getDaoFactory().getCertificateDao();
                boolean isError = false;
                if (count == 1) {
                    if (vo.getUserProfileList() != null) {
                        UserProfile[] userProfileList = vo.getUserProfileList().getItem();
                        sql = "INSERT INTO USER_ASSIGNMENT(USER_ID, VO_ID, USER_ASSIGNMENT_STATUS) VALUES(?, ?, ?)";
                        for (UserProfile userProfile : userProfileList) {
                            pStmt = con.prepareStatement(sql);
                            pStmt.setString(1, userProfile.getUserId());
                            pStmt.setString(2, vo.getVoId());
                            pStmt.setInt(3, Constants.ACTIVE);
                            pStmt.executeUpdate();
                            if (userProfile.getCertificates() != null) {
                                Certificate certificate = userProfile.getCertificates().getItem(0);
                                CertificateDTO certDTO = new CertificateDTO();
                                certDTO.setCertId(certificate.getCertId());
                                certDTO.setCertContent(certificate.getCertContent());
                                certDTO.setCertOwner(userProfile.getUserId());
                                certDTO.setCertType(Constants.CERT_USER_PARTICIPATE_VOM);
                                certDTO.setCertSubjectedParty(vo.getVoId());
                                IndividualState certState = certDao.createCertificate(certDTO);
                                if (!certState.isState()) {
                                    isError = true;
                                    log.error("User Profile :: " + userProfile.getUserId() + " cert create error :: " + certState.getFailureReason());
                                    createState.setFailureReason("User Profile :: " + userProfile.getUserId() + " cert create error :: " + certState.getFailureReason());
                                    break;
                                }
                            } else {
                                isError = true;
                                log.error("User Profile :: " + userProfile.getUserId() + " cert create error :: " + Constants.EMPTY_CERT_PROVIDED);
                                createState.setFailureReason("User Profile :: " + userProfile.getUserId() + " cert create error :: " + Constants.EMPTY_CERT_PROVIDED);
                                break;
                            }
                        }
                    }
                    if (vo.getServiceProviderList() != null && vo.getServiceProviderList().getItem() != null && !isError) {
                        ServiceProvider[] spList = vo.getServiceProviderList().getItem();
                        sql = "INSERT INTO SP_ASSIGNMENT(SP_ID, VO_ID, SP_ASSIGNMENT_STATUS) VALUES(?, ?, ?)";
                        for (ServiceProvider sp : spList) {
                            pStmt = con.prepareStatement(sql);
                            pStmt.setString(1, sp.getSpId());
                            pStmt.setString(2, vo.getVoId());
                            pStmt.setInt(3, Constants.ACTIVE);
                            pStmt.executeUpdate();
                            if (sp.getCertificates() != null) {
                                Certificate certificate = sp.getCertificates().getItem(0);
                                CertificateDTO certDTO = new CertificateDTO();
                                certDTO.setCertId(certificate.getCertId());
                                certDTO.setCertContent(certificate.getCertContent());
                                certDTO.setCertOwner(sp.getSpId());
                                certDTO.setCertType(Constants.CERT_SP_PARTICIPATE_VOM);
                                certDTO.setCertSubjectedParty(vo.getVoId());
                                IndividualState certState = certDao.createCertificate(certDTO);
                                if (!certState.isState()) {
                                    isError = true;
                                    log.error("Service Provider :: " + sp.getSpId() + " cert create error :: " + createState.getFailureReason());
                                    createState.setFailureReason("Service Provider :: " + sp.getSpId() + " cert create error :: " + createState.getFailureReason());
                                    break;
                                }
                            } else {
                                isError = true;
                                log.error("Service Provider :: " + sp.getSpId() + " cert create error :: " + createState.getFailureReason());
                                createState.setFailureReason("Service Provider :: " + sp.getSpId() + " cert create error :: " + createState.getFailureReason());
                                break;
                            }
                        }
                    }
                    if (!isError) {
                        Certificate voCert = vo.getCertificate();
                        CertificateDTO voCertDTO = new CertificateDTO();
                        if (voCert != null) {
                            voCertDTO.setCertId(voCert.getCertId());
                            voCertDTO.setCertContent(voCert.getCertContent());
                            voCertDTO.setCertOwner(vo.getAdminId());
                            voCertDTO.setCertType(Constants.CERT_USER_ADMIN_VOM);
                            voCertDTO.setCertSubjectedParty(vo.getVoId());
                            IndividualState voCertState = certDao.createCertificate(voCertDTO);
                            if (!voCertState.isState()) {
                                isError = true;
                                createState.setFailureReason("Invalid VO cert " + voCertState.getFailureReason());
                            }
                        } else {
                            isError = true;
                            createState.setFailureReason(Constants.EMPTY_VO_CERT_PROVIDED);
                        }
                    }
                    if (!isError) {
                        con.commit();
                        createState.setState(Constants.INDIVIDUAL_STATE_SUCCESS);
                    }
                } else {
                    createState.setFailureReason(Constants.UNKNOWN_DB_ERROR);
                }
            } else {
                createState.setFailureReason(Constants.DUPLICATE_ENTITY);
            }
        } catch (Exception e) {
            createState.setFailureReason(e.getMessage() + " : " + vo.getVoId());
            log.error("Error creating VO ", e);
        } finally {
            DBUtil.closeStatement(pStmt);
            DBUtil.returnConnection(con);
        }
        return createState;
    }

    public IndividualState deleteVO(String voId) {
        IndividualState deleteState = new IndividualState();
        deleteState.setServiceId(voId);
        Connection con = null;
        PreparedStatement pStmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);
            String sql = "";
            for (int i = 0; i < 3; i++) {
                switch(i) {
                    case 0:
                        sql = "UPDATE VO SET VO_STATUS = ? WHERE VO_ID = ?";
                        break;
                    case 1:
                        sql = "UPDATE USER_ASSIGNMENT SET USER_ASSIGNMENT_STATUS = ? WHERE VO_ID = ?";
                        break;
                    case 2:
                        sql = "UPDATE SP_ASSIGNMENT SET SP_ASSIGNMENT_STATUS = ? WHERE VO_ID = ?";
                        break;
                }
                pStmt = con.prepareStatement(sql);
                pStmt.setInt(1, Constants.DELETED);
                pStmt.setString(2, voId);
                int count = pStmt.executeUpdate();
                if (i == 0) {
                    if (count == 0) {
                        deleteState.setFailureReason(Constants.VO_NOT_FOUND);
                        break;
                    }
                } else if (i == 2) {
                    con.commit();
                    deleteState.setState(true);
                }
            }
        } catch (Exception e) {
            deleteState.setServiceId(e.getMessage() + " : " + voId);
            log.error("Error deleting VO ", e);
        } finally {
            DBUtil.closeStatement(pStmt);
            DBUtil.returnConnection(con);
        }
        return deleteState;
    }

    public IndividualState updateVO(VO vo, boolean deleteUsers, boolean deleteSPs) {
        IndividualState updateState = new IndividualState();
        updateState.setServiceId(vo.getVoId());
        Connection con = null;
        PreparedStatement pStmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);
            Policy voPolicy = vo.getPolicy();
            boolean isError = false;
            int policyId = Constants.EMPTY_POLICY_ID;
            if (voPolicy != null) {
                PolicyDao policyDao = DaoFactory.getDaoFactory().getPolicyDao();
                Properties props = Configurator.load(org.gridtrust.Constants.PPM_PROP_STORE_DIR, org.gridtrust.Constants.PPM_PROP_FILE);
                boolean isNormalized = Boolean.parseBoolean(props.getProperty(Constants.PPM_IS_NORMALISED));
                if (isNormalized) {
                    log.info("VO policy will be normalized");
                    int bindType = Integer.parseInt(props.getProperty(Constants.PPM_BIND_TYPE));
                    PolicyNormalizer normalizer = PolicyNormalizerFactory.getPolicyNormalizer(bindType);
                    String policyContent = voPolicy.getPolicyContent();
                    policyContent = normalizer.normalizePolicy(policyContent);
                    voPolicy.setPolicyContent(policyContent);
                }
                if (voPolicy.getPolicyId() != null) {
                    policyId = policyDao.updatePolicy(voPolicy, Constants.VO_POLICY);
                } else {
                    policyId = policyDao.createPolicy(voPolicy, Constants.VO_POLICY);
                }
            }
            String sql = "UPDATE VO SET DESCRIPTION_URL = ?, DESCRIPTION_CONTENT = ?, ADMIN_ID = ?, POLICY_ID = ? WHERE VO_ID = ? AND VO_STATUS = ?";
            pStmt = con.prepareStatement(sql);
            pStmt.setString(1, vo.getDescriptionURL());
            pStmt.setString(2, vo.getDescription());
            pStmt.setString(3, vo.getAdminId());
            pStmt.setInt(4, policyId);
            pStmt.setString(5, vo.getVoId());
            pStmt.setInt(6, Constants.ACTIVE);
            int count = pStmt.executeUpdate();
            if (count != 0) {
                if (vo.getUserProfileList() != null && vo.getUserProfileList().getItem() != null) {
                    if (deleteUsers) {
                        sql = "UPDATE USER_ASSIGNMENT SET USER_ASSIGNMENT_STATUS = ? WHERE VO_ID = ? AND USER_ID = ?";
                        for (UserProfile user : vo.getUserProfileList().getItem()) {
                            pStmt = con.prepareStatement(sql);
                            pStmt.setInt(1, Constants.DELETED);
                            pStmt.setString(2, vo.getVoId());
                            pStmt.setString(3, user.getUserId());
                            pStmt.executeUpdate();
                        }
                    } else {
                        sql = "INSERT INTO USER_ASSIGNMENT(VO_ID, USER_ID, USER_ASSIGNMENT_STATUS) VALUES(?, ?, ?)";
                        CertificateDao certDao = DaoFactory.getDaoFactory().getCertificateDao();
                        UserProfile[] userList = vo.getUserProfileList().getItem();
                        for (UserProfile user : userList) {
                            pStmt = con.prepareStatement(sql);
                            pStmt.setString(1, vo.getVoId());
                            pStmt.setString(2, user.getUserId());
                            pStmt.setInt(3, Constants.ACTIVE);
                            pStmt.executeUpdate();
                            if (user.getCertificates() != null) {
                                Certificate certificate = user.getCertificates().getItem(0);
                                CertificateDTO certDTO = new CertificateDTO();
                                certDTO.setCertId(certificate.getCertId());
                                certDTO.setCertContent(certificate.getCertContent());
                                certDTO.setCertOwner(user.getUserId());
                                certDTO.setCertType(Constants.CERT_USER_PARTICIPATE_VOM);
                                certDTO.setCertSubjectedParty(vo.getVoId());
                                IndividualState certState = certDao.createCertificate(certDTO);
                                if (!certState.isState()) {
                                    isError = true;
                                    log.error("User Profile :: " + user.getUserId() + " cert create error :: " + updateState.getFailureReason());
                                    updateState.setFailureReason("User Profile :: " + user.getUserId() + " cert create error :: " + updateState.getFailureReason());
                                    break;
                                }
                            } else {
                                isError = true;
                                updateState.setFailureReason("User Profile :: " + user.getUserId() + " cert create error :: " + Constants.EMPTY_CERT_PROVIDED);
                                break;
                            }
                        }
                    }
                }
                if (vo.getServiceProviderList() != null) {
                    ServiceProvider[] spList = vo.getServiceProviderList().getItem();
                    if (spList != null && !isError) {
                        if (deleteSPs) {
                            sql = "UPDATE SP_ASSIGNMENT SET SP_ASSIGNMENT_STATUS = ? WHERE VO_ID = ? AND SP_ID = ?";
                            for (ServiceProvider sp : spList) {
                                pStmt = con.prepareStatement(sql);
                                pStmt.setInt(1, Constants.DELETED);
                                pStmt.setString(2, vo.getVoId());
                                pStmt.setString(3, sp.getSpId());
                                pStmt.executeUpdate();
                            }
                        } else {
                            sql = "INSERT INTO SP_ASSIGNMENT(VO_ID, SP_ID, SP_ASSIGNMENT_STATUS) VALUES(?, ?, ?)";
                            CertificateDao certDao = DaoFactory.getDaoFactory().getCertificateDao();
                            for (ServiceProvider sp : spList) {
                                pStmt = con.prepareStatement(sql);
                                pStmt.setString(1, vo.getVoId());
                                pStmt.setString(2, sp.getSpId());
                                pStmt.setInt(3, Constants.ACTIVE);
                                pStmt.executeUpdate();
                                if (sp.getCertificates() != null) {
                                    Certificate certificate = sp.getCertificates().getItem(0);
                                    CertificateDTO certDTO = new CertificateDTO();
                                    certDTO.setCertId(certificate.getCertId());
                                    certDTO.setCertContent(certificate.getCertContent());
                                    certDTO.setCertOwner(sp.getSpId());
                                    certDTO.setCertType(Constants.CERT_SP_PARTICIPATE_VOM);
                                    certDTO.setCertSubjectedParty(vo.getVoId());
                                    IndividualState certState = certDao.createCertificate(certDTO);
                                    if (!certState.isState()) {
                                        isError = true;
                                        log.error("Service Provider :: " + sp.getSpId() + " cert create error :: " + certState.getFailureReason());
                                        updateState.setFailureReason("Service Provider :: " + sp.getSpId() + " cert create error :: " + certState.getFailureReason());
                                        break;
                                    }
                                } else {
                                    isError = true;
                                    updateState.setFailureReason("Service Provider :: " + sp.getSpId() + " cert create error :: " + Constants.EMPTY_CERT_PROVIDED);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!isError) {
                    con.commit();
                    updateState.setState(Constants.INDIVIDUAL_STATE_SUCCESS);
                }
            } else {
                updateState.setFailureReason(Constants.VO_NOT_FOUND);
            }
        } catch (Exception e) {
            updateState.setFailureReason(e.getMessage() + " : " + vo.getVoId());
            log.error("Error updating VO ", e);
        } finally {
            DBUtil.closeStatement(pStmt);
            DBUtil.returnConnection(con);
        }
        return updateState;
    }

    public VO findVO(String voId) {
        VO vo = null;
        Connection con = null;
        PreparedStatement pStmt = null;
        try {
            con = getConnection();
            String sql = "SELECT VO_ID, DESCRIPTION_URL, DESCRIPTION_CONTENT, ADMIN_ID FROM VO WHERE VO_ID = ? AND VO_STATUS = ?";
            pStmt = con.prepareStatement(sql);
            pStmt.setString(1, voId);
            pStmt.setInt(2, Constants.ACTIVE);
            ResultSet rs = pStmt.executeQuery();
            CertificateDao certDao = DaoFactory.getDaoFactory().getCertificateDao();
            UserDao userDao = DaoFactory.getDaoFactory().getUserDao();
            ServiceProviderDao spDao = DaoFactory.getDaoFactory().getServiceProviderDao();
            if (rs.next()) {
                vo = new VO();
                vo.setVoId(voId);
                vo.setDescriptionURL(rs.getString("DESCRIPTION_URL"));
                vo.setDescription(rs.getString("DESCRIPTION_CONTENT"));
                String adminId = rs.getString("ADMIN_ID");
                vo.setAdminId(adminId);
                Certificate voCert = certDao.findCertificateByOwnerRelationSubject(adminId, Constants.CERT_USER_ADMIN_VOM, voId);
                vo.setCertificate(voCert);
                sql = "SELECT USER_ID FROM USER_ASSIGNMENT WHERE VO_ID = ? AND USER_ASSIGNMENT_STATUS = ?";
                pStmt = con.prepareStatement(sql);
                pStmt.setString(1, voId);
                pStmt.setInt(2, Constants.ACTIVE);
                ResultSet userRs = pStmt.executeQuery();
                List<UserProfile> userProfileList = new ArrayList<UserProfile>();
                while (userRs.next()) {
                    String userId = userRs.getString("USER_ID");
                    UserProfile user = userDao.findUser(userId);
                    userProfileList.add(user);
                }
                Array_UserProfile userArray = new Array_UserProfile(userProfileList.toArray(new UserProfile[userProfileList.size()]));
                vo.setUserProfileList(userArray);
                sql = "SELECT SP_ID FROM SP_ASSIGNMENT WHERE VO_ID = ? AND SP_ASSIGNMENT_STATUS = ?";
                pStmt = con.prepareStatement(sql);
                pStmt.setString(1, voId);
                pStmt.setInt(2, Constants.ACTIVE);
                ResultSet spRs = pStmt.executeQuery();
                List<ServiceProvider> serviceProviderList = new ArrayList<ServiceProvider>();
                while (spRs.next()) {
                    String spId = spRs.getString("SP_ID");
                    ServiceProvider sp = spDao.findServiceProvider(spId);
                    serviceProviderList.add(sp);
                }
                Array_ServiceProvider spArray = new Array_ServiceProvider(serviceProviderList.toArray(new ServiceProvider[serviceProviderList.size()]));
                vo.setServiceProviderList(spArray);
            }
        } catch (Exception e) {
            log.error("Error finding VO " + voId, e);
        } finally {
            DBUtil.closeStatement(pStmt);
            DBUtil.returnConnection(con);
        }
        return vo;
    }

    public IndividualState cleanVO(String voId) {
        IndividualState cleanState = new IndividualState();
        cleanState.setServiceId(voId);
        Connection con = null;
        PreparedStatement pStmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);
            String sql = "";
            for (int i = 0; i < 2; i++) {
                if (i == 0) {
                    sql = "SELECT USER_ID FROM USER_ASSIGNMENT WHERE VO_ID = ?";
                } else {
                    sql = "SELECT SP_ID FROM SP_ASSIGNMENT WHERE VO_ID = ?";
                }
                pStmt = con.prepareStatement(sql);
                pStmt.setString(1, voId);
                ResultSet rs = pStmt.executeQuery();
                ServiceProviderDao spDao = DaoFactory.getDaoFactory().getServiceProviderDao();
                UserDao userDao = DaoFactory.getDaoFactory().getUserDao();
                List<String> idList = new ArrayList<String>();
                while (rs.next()) {
                    String id = null;
                    if (i == 0) {
                        id = rs.getString("USER_ID");
                    } else {
                        id = rs.getString("SP_ID");
                    }
                    idList.add(id);
                }
                if (i == 0) {
                    userDao.cleanUserProfile(idList.toArray(new String[idList.size()]));
                } else {
                    spDao.cleanServiceProvider(idList.toArray(new String[idList.size()]));
                }
            }
            for (int i = 0; i < 3; i++) {
                switch(i) {
                    case 0:
                        sql = "DELETE FROM USER_ASSIGNMENT WHERE VO_ID = ?";
                        break;
                    case 1:
                        sql = "DELETE FROM SP_ASSIGNMENT WHERE VO_ID = ?";
                        break;
                    case 2:
                        sql = "DELETE FROM VO WHERE VO_ID = ?";
                        break;
                }
                pStmt = con.prepareStatement(sql);
                pStmt.setString(1, voId);
                int count = pStmt.executeUpdate();
                if (i == 2) {
                    if (count != 0) {
                        cleanState.setState(true);
                        con.commit();
                    } else {
                        cleanState.setFailureReason(Constants.VO_NOT_FOUND);
                        con.rollback();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error cleaning VO " + voId, e);
        } finally {
            DBUtil.closeStatement(pStmt);
            DBUtil.returnConnection(con);
        }
        return cleanState;
    }

    public boolean findSPInVO(String voId, String spId) {
        boolean isFound = false;
        Connection con = null;
        PreparedStatement pStmt = null;
        try {
            con = getConnection();
            String sql = "SELECT SP_ID FROM SP_ASSIGNMENT WHERE VO_ID = ? AND SP_ID = ? AND SP_ASSIGNMENT_STATUS = ?";
            pStmt = con.prepareStatement(sql);
            pStmt.setString(1, voId);
            pStmt.setString(2, spId);
            pStmt.setInt(3, Constants.ACTIVE);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) {
                isFound = true;
            }
        } catch (Exception e) {
            log.error("Error finding SP in VO " + voId + " :" + spId, e);
        } finally {
            DBUtil.closeStatement(pStmt);
            DBUtil.returnConnection(con);
        }
        return isFound;
    }

    public boolean findUserInVO(String voId, String userId) {
        boolean isFound = false;
        Connection con = null;
        PreparedStatement pStmt = null;
        try {
            con = getConnection();
            String sql = "SELECT SP_ID FROM USER_ASSIGNMENT WHERE VO_ID = ? AND USER_ID = ? AND USER_ASSIGNMENT_STATUS = ?";
            pStmt = con.prepareStatement(sql);
            pStmt.setString(1, voId);
            pStmt.setString(2, userId);
            pStmt.setInt(3, Constants.ACTIVE);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) {
                isFound = true;
            }
        } catch (Exception e) {
            log.error("Error finding user in VO " + voId + " :" + userId, e);
        } finally {
            DBUtil.closeStatement(pStmt);
            DBUtil.returnConnection(con);
        }
        return isFound;
    }
}
