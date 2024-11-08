package coopnetclient.utils.launcher.launchinfos;

import coopnetclient.Globals;
import coopnetclient.utils.RoomData;
import coopnetclient.utils.settings.Settings;
import coopnetclient.utils.gamedatabase.GameDatabase;
import coopnetclient.utils.gamedatabase.GameSetting;
import coopnetclient.utils.launcher.TempGameSettings;
import java.io.File;

public class DosboxLaunchInfo extends LaunchInfo {

    private String dosboxBinaryPath;

    private String dosboxParameters;

    private String gameBinaryPath;

    public DosboxLaunchInfo(RoomData roomData) {
        super(roomData);
        dosboxBinaryPath = Settings.getDOSBoxExecutable();
        gameBinaryPath = GameDatabase.getLocalExecutablePath(GameDatabase.getIDofGame(roomData.getChannel()));
        dosboxParameters = "-noconsole -c \"mount X -u\" -c \"mount X " + GameDatabase.getLocalInstallPath(GameDatabase.getIDofGame(roomData.getChannel())) + "\" -c \"X:\"";
        if (roomData.isHost()) {
            dosboxParameters += " " + GameDatabase.getHostPattern(roomData.getChannel(), roomData.getModName());
        } else {
            dosboxParameters += " " + GameDatabase.getJoinPattern(roomData.getChannel(), roomData.getModName());
        }
        if (Settings.getDOSBoxFullscreen()) {
            dosboxParameters += " -fullscreen";
        }
        dosboxParameters += " -c \"exit\"";
    }

    public String getGameBinaryPath() {
        return gameBinaryPath;
    }

    public String getDosboxBinaryPath() {
        return dosboxBinaryPath;
    }

    public String getInstallPath() {
        return GameDatabase.getInstallPath(roomData.getChannel());
    }

    public String getDosboxParameters() {
        String ret = dosboxParameters;
        String executableName = new File(gameBinaryPath).getName();
        ret = ret.replace("{GAMEEXE}", executableName);
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
        return gameBinaryPath.substring(gameBinaryPath.lastIndexOf(File.separatorChar) + 1);
    }
}
