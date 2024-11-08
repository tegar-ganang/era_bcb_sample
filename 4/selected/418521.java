package hailmary.teamimport;

import hailmary.bloodbowl.Team;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Observable;

public class TeamFilesRepository extends Observable {

    static final String BASE_DIR = "./files/";

    private File teamDir;

    public TeamFilesRepository(Team team) {
        File baseDir = new File(TeamFilesRepository.BASE_DIR);
        String teamDirPath = baseDir.getPath() + File.separatorChar + TeamFilesRepository.generateDirName(team);
        this.teamDir = new File(teamDirPath);
        if (!teamDir.exists()) {
            teamDir.mkdirs();
        }
    }

    public File getFile(URL remoteFile) throws FileNotFoundException, IOException {
        String remoteFileName = remoteFile.getPath().substring(remoteFile.getPath().lastIndexOf("/"));
        String teamFilePath = this.teamDir.getPath() + File.separatorChar + remoteFileName;
        File teamFile = new File(teamFilePath);
        if (!teamFile.exists()) {
            this.downloadFile(remoteFile, teamFile);
        }
        return teamFile;
    }

    private void downloadFile(URL remoteFile, File localFile) throws FileNotFoundException, IOException {
        URLConnection remoteConnection = remoteFile.openConnection();
        BufferedInputStream in = new BufferedInputStream(remoteConnection.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(new DataOutputStream(new FileOutputStream(localFile)));
        String filename = remoteFile.toString();
        FileSize fileSize = new FileSize(remoteConnection.getContentLength());
        FileSize readSize;
        int readBytes = 0;
        int readByte;
        while ((readByte = in.read()) != -1) {
            out.write(readByte);
            readBytes++;
            if ((readBytes % 1024) == 0) {
                readSize = new FileSize(readBytes);
                String status = "Downloading " + filename + ": " + readSize.getReadable() + " of " + fileSize.getReadable();
                ProgressUpdateMessage message = new ProgressUpdateMessage(status);
                this.setChanged();
                this.notifyObservers(message);
            }
        }
        out.close();
        in.close();
    }

    private static String generateDirName(Team team) {
        String teamDirName;
        if (team.getName() != "") {
            teamDirName = team.getName();
        } else {
            teamDirName = team.getSource().toString();
        }
        teamDirName = teamDirName.replaceAll("[\\s/:]", "_");
        return teamDirName;
    }
}
