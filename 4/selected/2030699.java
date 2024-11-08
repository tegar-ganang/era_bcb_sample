package com.ienjinia.vc.env;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Environment {

    private String ienjiniaSaves;

    private String ienjiniaGames;

    private String ienjiniaProjects;

    public Environment() throws IOException {
        String home = System.getProperty("user.home");
        String ienjiniaHome = home + File.separator + "ienjinia";
        File file = new File(ienjiniaHome);
        if (!file.exists()) file.mkdir();
        ienjiniaSaves = ienjiniaHome + File.separator + "saves";
        file = new File(ienjiniaSaves);
        if (!file.exists()) file.mkdir();
        ienjiniaGames = ienjiniaHome + File.separator + "games";
        file = new File(ienjiniaGames);
        if (!file.exists()) file.mkdir();
        ienjiniaProjects = ienjiniaHome + File.separator + "projects";
        file = new File(ienjiniaProjects);
        if (!file.exists()) file.mkdir();
    }

    public String toSavePath(String gameName) {
        return ienjiniaSaves + File.separator + gameName + ".data";
    }

    public String toGamePath(String gameName) {
        return ienjiniaGames + File.separator + gameName + ".cart";
    }

    public String toProjectPath(String projectName) {
        return ienjiniaProjects + File.separator + projectName + ".proj";
    }

    public String toProjectFile(String projectName, String fileName) {
        return toProjectPath(projectName) + File.separator + fileName;
    }

    public String[] getGameNames() {
        File file = new File(ienjiniaGames);
        String[] filenames = file.list();
        List names = new ArrayList();
        for (int i = 0; i < filenames.length; i++) {
            String s = filenames[i];
            if (s.endsWith(".cart")) names.add(s.substring(0, s.lastIndexOf('.')));
        }
        Collections.sort(names);
        return (String[]) names.toArray(new String[names.size()]);
    }

    public boolean gameExists(String gameName) {
        File file = new File(toGamePath(gameName));
        return file.exists();
    }

    public String[] getProjectNames() {
        File file = new File(ienjiniaProjects);
        String[] filenames = file.list();
        List names = new ArrayList();
        for (int i = 0; i < filenames.length; i++) {
            String s = filenames[i];
            if (s.endsWith(".proj")) names.add(s.substring(0, s.lastIndexOf('.')));
        }
        Collections.sort(names);
        return (String[]) names.toArray(new String[names.size()]);
    }

    public String[] getProjectFilenames(String projectName, String ext) {
        File file = new File(toProjectPath(projectName));
        String[] filenames = file.list();
        List names = new ArrayList();
        for (int i = 0; i < filenames.length; i++) {
            String s = filenames[i];
            if (s.endsWith(ext)) names.add(s);
        }
        Collections.sort(names);
        return (String[]) names.toArray(new String[names.size()]);
    }

    public String[] getProjectFilenames(String projectName) {
        File file = new File(toProjectPath(projectName));
        String[] filenames = file.list();
        List names = new ArrayList();
        for (int i = 0; i < filenames.length; i++) {
            String s = filenames[i];
            names.add(s);
        }
        Collections.sort(names);
        return (String[]) names.toArray(new String[names.size()]);
    }

    public boolean projectExists(String projectName) {
        File file = new File(toProjectPath(projectName));
        return file.exists();
    }

    public void createProject(String projectName) throws IOException {
        File file = new File(toProjectPath(projectName));
        file.mkdir();
        FileOutputStream fos = new FileOutputStream(toProjectFile(projectName, "main.bsh"));
        fos.close();
    }

    public void extractProjects(String archiveName) throws IOException {
        byte[] buffer = new byte[4096];
        Set newProjects = new HashSet();
        InputStream is = getClass().getResourceAsStream(archiveName);
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            String entryName = entry.getName();
            String projectName = getProjectName(entryName);
            if (entry.isDirectory()) {
                if (!projectExists(projectName)) {
                    createProject(projectName);
                    newProjects.add(projectName);
                }
            } else {
                if (newProjects.contains(projectName)) {
                    String filename = getProjectFileName(entryName);
                    FileOutputStream fos = new FileOutputStream(toProjectFile(projectName, filename));
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    int n;
                    while ((n = zis.read(buffer, 0, 4096)) != -1) bos.write(buffer, 0, n);
                    bos.close();
                }
            }
        }
    }

    public void extractGames(String archiveName) throws IOException {
        byte[] buffer = new byte[4096];
        InputStream is = getClass().getResourceAsStream(archiveName);
        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                String entryName = entry.getName();
                String gameName = getGameName(entryName);
                if (!gameExists(gameName)) {
                    FileOutputStream fos = new FileOutputStream(toGamePath(gameName));
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    int n;
                    while ((n = zis.read(buffer, 0, 4096)) != -1) bos.write(buffer, 0, n);
                    bos.close();
                }
            }
        }
    }

    private String getProjectName(String entryName) {
        return entryName.substring(0, entryName.indexOf(".proj"));
    }

    private String getProjectFileName(String entryName) {
        return entryName.substring(entryName.indexOf(".proj/") + 6);
    }

    private String getGameName(String entryName) {
        return entryName.substring(0, entryName.indexOf(".cart"));
    }
}
