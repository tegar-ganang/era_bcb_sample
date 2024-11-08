package coopnetclient.utils.launcher.launchinfos;

import coopnetclient.utils.RegistryReader;
import coopnetclient.utils.RoomData;
import coopnetclient.utils.gamedatabase.GameDatabase;

public class DirectPlayLaunchInfo extends LaunchInfo {

    private String gameGUID;

    private String installPath;

    private String binaryPath;

    private String parameters;

    public DirectPlayLaunchInfo(RoomData roomData) {
        super(roomData);
        this.gameGUID = GameDatabase.getGuid(roomData.getChannel(), roomData.getModName());
    }

    public String getGameGUID() {
        return gameGUID;
    }

    public boolean isSearchEnabled() {
        return roomData.isDoSearch();
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public String getInstallPath() {
        return installPath;
    }

    public String getParameters() {
        return parameters;
    }

    @Override
    public String getBinaryName() {
        String regPath = GameDatabase.getRegEntry(roomData.getChannel(), roomData.getModName()).get(0);
        return RegistryReader.read(regPath.substring(0, regPath.lastIndexOf("\\") + 1) + "File");
    }
}
