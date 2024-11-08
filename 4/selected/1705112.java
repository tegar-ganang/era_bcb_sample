package netposa.network;

import java.io.UnsupportedEncodingException;
import netposa.entity.AhssEntity;
import netposa.entity.AllPointEntity;
import netposa.entity.AbstractBaseEntity;
import netposa.entity.ChannelEntity;
import netposa.entity.DownAddrEntity;
import netposa.entity.GoPlayerEntity;
import netposa.entity.HistoryFileEntity;
import netposa.entity.HistoryGroupEntity;
import netposa.entity.LoginEntity;
import netposa.entity.LogoutEntity;
import netposa.entity.PermissionEntity;
import netposa.entity.PlanChannelEntity;
import netposa.entity.PlanEnableEntity;
import netposa.entity.PlanGroupEntity;
import netposa.entity.PreviewImageEntity;
import netposa.entity.RtspEntity;
import netposa.entity.SendPTZControlEntity;
import netposa.entity.StartTransferEntity;
import netposa.entity.SuddenChannelEntity;
import netposa.entity.TcsEntity;
import netposa.entity.UpAddrEntity;
import netposa.entity.UserFromEntity;
import netposa.entity.UserGroupEntity;
import netposa.npm.UserBaseInfo;

public class NetOperationV10001 extends BaseNetOperation {

    private String tmpurl;

    public NetOperationV10001() {
    }

    @Override
    public AbstractBaseEntity login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        String Param = username + ":" + password;
        String encodeParam;
        LoginEntity entity = new LoginEntity();
        try {
            encodeParam = Base64.encodeToString(Param.getBytes("GB2312"), Base64.NO_WRAP);
            tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Login.php?" + encodeParam;
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity logout() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Logout.php";
        LogoutEntity entity = new LogoutEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getChannel(String addr) {
        if (addr == null) {
            tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Channel.php";
        } else {
            String encodeParam;
            try {
                encodeParam = Base64.encodeToString(addr.getBytes("GB2312"), Base64.NO_WRAP);
                tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Channel.php?" + encodeParam;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        ChannelEntity entity = new ChannelEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getAhss() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetAhss.php";
        AhssEntity entity = new AhssEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getRtsp() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Getrtspserver.php";
        RtspEntity entity = new RtspEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getTcs() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Gettcs.php";
        TcsEntity entity = new TcsEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity startTransfer(String channelcode, String type, String networktype) {
        if (channelcode == null || type == null || networktype == null) {
            return null;
        }
        String Param = channelcode + ":" + type + ":" + networktype;
        try {
            String encodeParam = Base64.encodeToString(Param.getBytes("GB2312"), Base64.NO_WRAP);
            tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Starttransfer.php?" + encodeParam;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StartTransferEntity entity = new StartTransferEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getUserFrom() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Userfrom.php";
        UserFromEntity entity = new UserFromEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getDownAddr(String addr) {
        if (addr == null) {
            return null;
        }
        try {
            String encodeParam = Base64.encodeToString(addr.getBytes("GB2312"), Base64.NO_WRAP);
            tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Getdownaddr.php?" + encodeParam;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        DownAddrEntity entity = new DownAddrEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getUpAddr(String addr) {
        if (addr == null) {
            return null;
        }
        String encodeParam;
        try {
            encodeParam = Base64.encodeToString(addr.getBytes("GB2312"), Base64.NO_WRAP);
            tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Getupaddr.php?" + encodeParam;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        UpAddrEntity entity = new UpAddrEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity sendPTZControl(String code, String pmd, String par) {
        if (code == null || pmd == null || par == null) {
            return null;
        }
        String Param = code + ":" + pmd + ":" + par;
        try {
            String encodeParam = Base64.encodeToString(Param.getBytes("GB2312"), Base64.NO_WRAP);
            tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_Ptzcontrol.php?" + encodeParam;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        SendPTZControlEntity entity = new SendPTZControlEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getAllPoint() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetAllPoint.php";
        AllPointEntity entity = new AllPointEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getHistoryGroup() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetHistoryGroup.php";
        HistoryGroupEntity entity = new HistoryGroupEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getHistoryFile(String historygroup) {
        if (historygroup == null) {
            return null;
        }
        try {
            String param = historygroup + ":rtsp";
            String encodeParam = Base64.encodeToString(param.getBytes("GB2312"), Base64.NO_WRAP);
            tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetHistoryFile.php?" + encodeParam;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HistoryFileEntity entity = new HistoryFileEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getUserGroup() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetUserGroup.php";
        UserGroupEntity entity = new UserGroupEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getPermissionId(String usergroup) {
        if (usergroup == null) {
            return null;
        }
        String encodeParam;
        try {
            encodeParam = Base64.encodeToString(usergroup.getBytes("GB2312"), Base64.NO_WRAP);
            tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetPermissionId.php?" + encodeParam;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        PermissionEntity entity = new PermissionEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getSuddenChannel() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetSuddenChannel.php";
        SuddenChannelEntity entity = new SuddenChannelEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getPlanGroup() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetPlanGroup.php";
        PlanGroupEntity entity = new PlanGroupEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getPlanChannel(String plangroup) {
        if (plangroup == null) {
            return null;
        }
        PlanChannelEntity entity = new PlanChannelEntity();
        String encodeParam;
        try {
            encodeParam = Base64.encodeToString(plangroup.getBytes("GB2312"), Base64.NO_WRAP);
            tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetPlanChannel.php?" + encodeParam;
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity goPlayer(String url) {
        if (url == null) {
            return null;
        }
        GoPlayerEntity entity = new GoPlayerEntity();
        try {
            entity.parseV10001(new String(sendRequest(url), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public BaseNetOperation creatNew() {
        return new NetOperationV10001();
    }

    @Override
    public AbstractBaseEntity getPlanEnable() {
        tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_GetPlanEnable.php";
        PlanEnableEntity entity = new PlanEnableEntity();
        try {
            entity.parseV10001(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getPreview(String code, String width, String height, String scale, String viewtime, String isopen) {
        if (code == null || width == null || height == null || viewtime == null) {
            return null;
        }
        String Param = code + ":" + width + ":" + height + ":" + scale + ":" + viewtime + ":" + isopen;
        try {
            String encodeParam = Base64.encodeToString(Param.getBytes("GB2312"), Base64.NO_WRAP);
            tmpurl = "http://" + UserBaseInfo.mURI + "/SDK/SDK_PicProcess.php?" + encodeParam;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        PreviewImageEntity entity = new PreviewImageEntity();
        entity.setData(sendRequest(tmpurl));
        entity.parseV10001("null");
        return entity;
    }
}
