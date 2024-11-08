package se.kawaiiriver;

import java.io.*;
import java.util.*;

public class Profile {

    private File profileFile;

    private static File getProfilePath() {
        return new File(Config.getUserSettingsPath(), "profiles");
    }

    private Profile(File profileFile) {
        this.profileFile = profileFile;
        if (!profileFile.canRead()) {
            System.out.println("Can't read " + profileFile);
        }
    }

    public static Profile getProfile(String profileName) {
        return new Profile(new File(getProfilePath(), profileName + ".profile"));
    }

    public static Profile[] getAllProfiles() {
        File profileDirectory = getProfilePath();
        profileDirectory.mkdirs();
        if (!profileDirectory.isDirectory()) {
            throw new RuntimeException(profileDirectory + " is not a valid directory.");
        }
        String[] profilesStrings = profileDirectory.list(new ProfileFilter());
        Profile[] profiles = new Profile[profilesStrings.length];
        for (int i = 0; i < profiles.length; i++) profiles[i] = new Profile(new File(profileDirectory, profilesStrings[i]));
        return profiles;
    }

    private String readOption(String option) {
        String returnSetting = "";
        try {
            BufferedReader input = new BufferedReader(new FileReader(profileFile));
            String line;
            while ((line = input.readLine()) != null) if (line.indexOf(option + "=") > -1) returnSetting = line.substring(line.indexOf("=") + 1);
            input.close();
        } catch (IOException e) {
            System.out.println("error reading option");
        }
        return returnSetting;
    }

    public String getProfileName() {
        String profileFileName = profileFile.getName();
        return profileFileName.substring(0, profileFileName.indexOf('.'));
    }

    public String getBrand() {
        return readOption("brand");
    }

    public String getDevice() {
        return readOption("device");
    }

    public int getMaxVideoBitrate() {
        return Integer.parseInt(readOption("maxVideoBitrate"));
    }

    public int getMaxAudioBitrate() {
        return Integer.parseInt(readOption("maxAudioBitrate"));
    }

    public Dimensions[] getDimensions() {
        String[] dimensionsTokens = readOption("dimensions").split(" ");
        Dimensions[] dimensions = new Dimensions[dimensionsTokens.length];
        for (int i = 0; i < dimensionsTokens.length; i++) dimensions[i] = new Dimensions(dimensionsTokens[i]);
        return dimensions;
    }

    public double getMaxFrameRate() {
        return Double.parseDouble(readOption("maxFrameRate"));
    }

    public int getMaxLength() {
        try {
            return Integer.parseInt(readOption("maxLength"));
        } catch (Exception e) {
            return 0;
        }
    }

    public String getWrapperFormat() {
        String wrapperFormat = readOption("wrapperFormat");
        if (wrapperFormat.equals("")) return "avi";
        return wrapperFormat;
    }

    public String getVideoFormat() {
        String videoFormat = readOption("videoFormat");
        if (videoFormat.equals("")) return "mpeg4";
        return videoFormat;
    }

    public String getAudioFormat() {
        String audioFormat = readOption("audioFormat");
        if (audioFormat.equals("")) return "mp3";
        return audioFormat;
    }

    private static void writeFile(String resourcePath, File newProf) {
        byte[] buffer = new byte[4096];
        InputStream is = Profile.class.getResourceAsStream("profiles/" + resourcePath);
        if (is == null) {
            throw new RuntimeException("Could not find resource profiles/" + resourcePath);
        }
        OutputStream os = null;
        try {
            if (newProf.createNewFile()) {
                os = new FileOutputStream(newProf);
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            } else {
                Logger.logMessage("Could not output " + resourcePath, Logger.ERROR);
            }
        } catch (IOException e) {
            Logger.logMessage("Could not output " + resourcePath + " - " + e, Logger.ERROR);
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
            }
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
        }
    }

    public static void createConfDirectory() {
        File dir = getProfilePath();
        if (!dir.exists()) {
            dir.mkdirs();
            if (dir.isDirectory()) {
                String[] profiles = { "h300.profile", "pmp.profile", "x5.profile", "zvm.profile", "u10.profile" };
                for (int i = 0; i < profiles.length; i++) {
                    writeFile(profiles[i], new File(dir, profiles[i]));
                }
            }
        }
        if (!dir.isDirectory()) {
            throw new RuntimeException("Could not create dir: " + dir);
        } else {
            Logger.logMessage("Opened " + dir + " ok.", Logger.INFO);
        }
    }
}
