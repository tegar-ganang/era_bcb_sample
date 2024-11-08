package netposa.network;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import netposa.entity.AllPointEntity;
import netposa.entity.AbstractBaseEntity;
import netposa.entity.ChannelEntity;
import netposa.entity.DownAddrEntity;
import netposa.entity.HistoryFileEntity;
import netposa.entity.HistoryGroupEntity;
import netposa.entity.LoginEntity;
import netposa.entity.PermissionEntity;
import netposa.entity.SendPTZControlEntity;
import netposa.entity.StartTransferEntity;
import netposa.entity.UpAddrEntity;
import netposa.entity.UserFromEntity;
import netposa.entity.UserGroupEntity;
import netposa.npm.UserBaseInfo;

public class NetOperationV10000 extends BaseNetOperation {

    private String tmpurl;

    private String mUserName;

    public NetOperationV10000(String username) {
        mUserName = username;
    }

    @Override
    public AbstractBaseEntity login(String username, String password) {
        LoginEntity le = new LoginEntity();
        try {
            tmpurl = "http://" + UserBaseInfo.mURI + "/login.php?username=" + URLEncoder.encode(username, "GB2312") + "&password=" + URLEncoder.encode(password, "GB2312");
            le.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return le;
    }

    @Override
    public AbstractBaseEntity logout() {
        return null;
    }

    @Override
    public AbstractBaseEntity getChannel(String addr) {
        ChannelEntity entity = new ChannelEntity();
        try {
            if (addr == null) {
                tmpurl = "http://" + UserBaseInfo.mURI + "/channel.php";
            } else {
                tmpurl = "http://" + UserBaseInfo.mURI + "/channel.php?address=" + URLEncoder.encode(addr, "GB2312");
            }
            entity.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getAhss() {
        return null;
    }

    @Override
    public AbstractBaseEntity getRtsp() {
        return null;
    }

    @Override
    public AbstractBaseEntity getTcs() {
        return null;
    }

    @Override
    public AbstractBaseEntity startTransfer(String channelcode, String type, String networktype) {
        StartTransferEntity entity = new StartTransferEntity();
        try {
            tmpurl = "http://" + UserBaseInfo.mURI + "/starttransfer.php?code=" + URLEncoder.encode(channelcode, "GB2312") + "&type=" + URLEncoder.encode(type, "GB2312") + "&plc=" + URLEncoder.encode(networktype, "GB2312");
            entity.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getUserFrom() {
        UserFromEntity entity = new UserFromEntity();
        try {
            tmpurl = "http://" + UserBaseInfo.mURI + "/userfrom.php?username=" + URLEncoder.encode(mUserName, "GB2312");
            entity.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getDownAddr(String addr) {
        DownAddrEntity entity = new DownAddrEntity();
        try {
            tmpurl = "http://" + UserBaseInfo.mURI + "/getdownaddr.php?address=" + URLEncoder.encode(addr, "GB2312");
            entity.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getUpAddr(String addr) {
        UpAddrEntity entity = new UpAddrEntity();
        try {
            tmpurl = "http://" + UserBaseInfo.mURI + "/getupaddr.php?address=" + URLEncoder.encode(addr, "GB2312");
            entity.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity sendPTZControl(String code, String pmd, String par) {
        SendPTZControlEntity entity = new SendPTZControlEntity();
        try {
            tmpurl = "http://" + UserBaseInfo.mURI + "/ptzcontrol.php?code=" + URLEncoder.encode(code, "GB2312") + "&pmd=" + URLEncoder.encode(pmd, "GB2312") + "&param=" + URLEncoder.encode(par, "GB2312");
            entity.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getAllPoint() {
        AllPointEntity entity = new AllPointEntity();
        tmpurl = "http://" + UserBaseInfo.mURI + "/GetAllPoint.php";
        try {
            entity.parseV10000(new String(sendRequest(tmpurl), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getHistoryGroup() {
        HistoryGroupEntity entity = new HistoryGroupEntity();
        tmpurl = "http://" + UserBaseInfo.mURI + "/GetHistoryGroup.php";
        try {
            entity.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getHistoryFile(String historygroup) {
        HistoryFileEntity entity = new HistoryFileEntity();
        try {
            tmpurl = "http://" + UserBaseInfo.mURI + "/GetHistoryFile.php?gname=" + URLEncoder.encode(historygroup, "GB2312");
            entity.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getUserGroup() {
        UserGroupEntity entity = new UserGroupEntity();
        try {
            tmpurl = "http://" + UserBaseInfo.mURI + "/GetUserGroup.php?username=" + URLEncoder.encode(mUserName, "GB2312");
            entity.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public AbstractBaseEntity getPermissionId(String usergroup) {
        PermissionEntity entity = new PermissionEntity();
        try {
            tmpurl = "http://" + UserBaseInfo.mURI + "/GetPermissionId.php?ugroup=" + URLEncoder.encode(usergroup, "GB2312");
            entity.parseV10000(new String(sendRequest(tmpurl), "GB2312"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    @Override
    public BaseNetOperation creatNew() {
        return new NetOperationV10000(mUserName);
    }
}
