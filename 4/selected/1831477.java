package coopnetclient.utils.launcher.launchinfos;

import coopnetclient.Globals;
import coopnetclient.utils.RoomData;
import coopnetclient.utils.gamedatabase.GameDatabase;
import coopnetclient.utils.gamedatabase.GameSetting;
import coopnetclient.utils.launcher.TempGameSettings;
import java.io.File;

public class ParameterLaunchInfo extends LaunchInfo {

    private String binaryPath;

    private String parameters;

    public ParameterLaunchInfo(RoomData roomData) {
        super(roomData);
        binaryPath = GameDatabase.getLaunchPathWithExe(roomData.getChannel(), roomData.getModName());
        if (roomData.isHost()) {
            parameters = " " + GameDatabase.getHostPattern(roomData.getChannel(), roomData.getModName());
        } else {
            parameters = " " + GameDatabase.getJoinPattern(roomData.getChannel(), roomData.getModName());
        }
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public String getInstallPath() {
        return GameDatabase.getInstallPath(roomData.getChannel());
    }

    public String getParameters() {
        String ret = parameters;
        ret = ret.replace("{HOSTIP}", roomData.getIP());
        if (GameDatabase.getNoSpacesFlag(roomData.getChannel(), roomData.getModName())) {
            ret = ret.replace("{NAME}", Globals.getThisPlayerInGameName().replace(" ", "_"));
        } else {
            ret = ret.replace("{NAME}", Globals.getThisPlayerInGameName());
        }
        ret = ret.replace("{ROOMNAME}", roomData.getRoomName());
        if (roomData.getPassword() != null && roomData.getPassword().length() > 0) {
            String tmp;
            if (roomData.isHost()) {
                tmp = GameDatabase.getHostPasswordPattern(roomData.getChannel(), roomData.getModName());
            } else {
                tmp = GameDatabase.getJoinPasswordPattern(roomData.getChannel(), roomData.getModName());
            }
            tmp = tmp.replace("{PASSWORD}", roomData.getPassword());
            ret = ret.replace("{PASSWORD}", tmp);
        } else {
            ret = ret.replace("{PASSWORD}", "");
        }
        if (TempGameSettings.getMap() != null) {
            ret = ret.replace("{MAP}", TempGameSettings.getMap());
        }
        for (GameSetting gs : TempGameSettings.getGameSettings()) {
            ret = ret.replace("{" + gs.getKeyWord() + "}", gs.getRealValue());
        }
        String params = GameDatabase.getAdditionalParameters(GameDatabase.getIDofGame(roomData.getChannel()));
        if (params != null && params.length() > 0) {
            ret += " " + params;
        }
        return ret;
    }

    @Override
    public String getBinaryName() {
        return binaryPath.substring(binaryPath.lastIndexOf(File.separatorChar) + 1);
    }
}
