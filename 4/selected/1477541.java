package com.laoer.bbscs.fio.imp;

import java.io.IOException;
import java.io.InputStream;
import com.laoer.bbscs.bean.UserInfo;
import com.laoer.bbscs.bean.UserInfoSimple;
import com.laoer.bbscs.fio.UserInfoFileIO;
import com.laoer.bbscs.service.config.SysConfig;
import com.laoer.bbscs.service.config.UserConfig;
import com.laoer.bbscs.comm.*;
import java.io.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.*;
import org.apache.commons.io.*;
import org.apache.commons.logging.*;

public class UserInfoFileIOImp implements UserInfoFileIO {

    private static final Log logger = LogFactory.getLog(UserInfoFileIOImp.class);

    private UserConfig userConfig;

    private SysConfig sysConfig;

    public UserConfig getUserConfig() {
        return userConfig;
    }

    public void setUserConfig(UserConfig userConfig) {
        this.userConfig = userConfig;
    }

    public SysConfig getSysConfig() {
        return sysConfig;
    }

    public void setSysConfig(SysConfig sysConfig) {
        this.sysConfig = sysConfig;
    }

    public void writeUserFile(UserInfo userInfo) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(userInfo.getId());
        sb.append("|");
        sb.append(userInfo.getRegTime().getTime());
        sb.append("|");
        sb.append(userInfo.getLoginTime().getTime());
        sb.append("|");
        sb.append(userInfo.getArticleNum());
        sb.append("|");
        sb.append(userInfo.getArticleEliteNum());
        sb.append("|");
        sb.append(userInfo.getUserTitle());
        sb.append("|");
        sb.append(userInfo.getLifeForce());
        sb.append("|");
        sb.append(userInfo.getCoin());
        sb.append("|");
        sb.append(userInfo.getLiterary());
        sb.append("|");
        sb.append(userInfo.getUserKnow());
        sb.append("|");
        sb.append(userInfo.getExperience());
        sb.append("|");
        sb.append(userInfo.getHavePic());
        sb.append("|");
        if (StringUtils.isBlank(userInfo.getPicFileName())) {
            sb.append("-");
        } else {
            sb.append(userInfo.getPicFileName());
        }
        sb.append("|");
        if (StringUtils.isBlank(userInfo.getUserFrom())) {
            sb.append("-");
        } else {
            sb.append(userInfo.getUserFrom());
        }
        sb.append("|");
        File usrfile = new File(this.getUserConfig().getUserFilePath(userInfo.getId()) + Constant.USER_PROFILE);
        FileUtils.writeStringToFile(usrfile, sb.toString(), Constant.CHARSET);
    }

    public void delUserPicFile(UserInfo userInfo) {
        File f = new File(BBSCSUtil.getUserWebFilePath(userInfo.getId()) + userInfo.getPicFileName());
        if (f.exists()) {
            try {
                FileUtils.forceDelete(f);
            } catch (IOException e) {
                logger.error(e);
            }
        }
        f = new File(BBSCSUtil.getUserWebFilePath(userInfo.getId()) + userInfo.getPicFileName() + Constant.IMG_SMALL_FILEPREFIX);
        if (f.exists()) {
            try {
                FileUtils.forceDelete(f);
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    public void saveUserUpFile(UserInfo userInfo, String distFileName, InputStream instream) throws IOException {
        String fullPicFile = BBSCSUtil.getUserWebFilePath(userInfo.getId()) + distFileName;
        String fullPicFileSmall = BBSCSUtil.getUserWebFilePath(userInfo.getId()) + distFileName + Constant.IMG_SMALL_FILEPREFIX;
        OutputStream bos = new FileOutputStream(fullPicFile);
        IOUtils.copy(instream, bos);
        ImgUtil.reduceImg(fullPicFile, fullPicFileSmall, this.getSysConfig().getFaceWidth(), this.getSysConfig().getFaceHigh(), 0);
    }

    public UserInfoSimple getUserInfoSimple(String userID) {
        File f = new File(this.getUserConfig().getUserFilePath(userID) + Constant.USER_PROFILE);
        String userFileTxt = "";
        try {
            userFileTxt = FileUtils.readFileToString(f, Constant.CHARSET);
        } catch (IOException e) {
            logger.error(e);
        }
        UserInfoSimple uis = new UserInfoSimple();
        String[] ufs = userFileTxt.split("\\|");
        if (ufs.length == 14) {
            uis.setId(ufs[0]);
            uis.setRegTime(NumberUtils.toLong(ufs[1], System.currentTimeMillis()));
            uis.setLoginTime(NumberUtils.toLong(ufs[2], System.currentTimeMillis()));
            uis.setArticleNum(NumberUtils.toInt(ufs[3], 0));
            uis.setArticleEliteNum(NumberUtils.toInt(ufs[4], 0));
            uis.setUserTitle(NumberUtils.toInt(ufs[5], 0));
            uis.setLifeForce(NumberUtils.toInt(ufs[6], 0));
            uis.setCoin(NumberUtils.toInt(ufs[7], 0));
            uis.setLiterary(NumberUtils.toInt(ufs[8], 0));
            uis.setUserKnow(NumberUtils.toInt(ufs[9], 0));
            uis.setExperience(NumberUtils.toInt(ufs[10], 0));
            uis.setHavePic(NumberUtils.toInt(ufs[11], 0));
            uis.setPicFileName(ufs[12]);
            uis.setUserFrom(ufs[13]);
        }
        return uis;
    }
}
