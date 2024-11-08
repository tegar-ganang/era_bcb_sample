package de.filearanger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class FileAranger extends Task {

    private List<File> originalFiles = new ArrayList<File>();

    private String origDir = "";

    private String mainClass = "";

    private String destDir = "";

    private long maxSize = 0;

    private List<List<List<File>>> solutions = new ArrayList<List<List<File>>>();

    List<Long> avgOfTest = new ArrayList<Long>();

    List<Long> maxDiffOfTest = new ArrayList<Long>();

    /**
    * @param args
    */
    public static void main(String[] args) {
        FileAranger sorter = new FileAranger();
        sorter.sortFiles(args[0], args[1], Long.parseLong(args[2]), args[3]);
    }

    public void setDestination(String dir) {
        destDir = dir;
    }

    public void setSource(String dir) {
        origDir = dir;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void setMaxSize(String size) {
        maxSize = Long.parseLong(size);
    }

    public void execute() throws BuildException {
        sortFiles(origDir, destDir, maxSize, mainClass);
    }

    int solutionIndex = -1;

    long solutionDiff = Long.MAX_VALUE;

    public void sortFiles(String origDir, String destDir, long maxSize, String mainClass) {
        fillFileList(new File(origDir), originalFiles);
        if (checkFilesForMax(maxSize, originalFiles)) {
            System.err.println("There are files larger than the specified maximum");
            System.exit(0);
        }
        for (int tests = 0; tests < 30; ++tests) {
            List<File> workingCopy = new ArrayList<File>();
            workingCopy.addAll(originalFiles);
            Collections.shuffle(workingCopy);
            ArrayList<List<File>> fileFolders = new ArrayList<List<File>>();
            List<File> currDir = new ArrayList<File>();
            fileFolders.add(currDir);
            File mainClassFile = getPathOfMainClass(origDir, mainClass);
            for (int i = 0; i < workingCopy.size(); ++i) {
                File currFile = workingCopy.get(i);
                if (currFile.getAbsolutePath().equals(mainClassFile.getAbsolutePath())) {
                    currDir.add(workingCopy.get(i));
                    workingCopy.remove(i);
                    break;
                }
            }
            while (workingCopy.size() != 0) {
                long currSize = sizeOfFolder(currDir);
                int index = getFileOfMaxLength(maxSize - currSize, workingCopy);
                if (index == -1) {
                    currDir = new ArrayList<File>();
                    fileFolders.add(currDir);
                } else {
                    currDir.add(workingCopy.get(index));
                    workingCopy.remove(index);
                }
            }
            solutions.add(fileFolders);
            long avg = 0;
            for (int i = 0; i < fileFolders.size(); ++i) {
                avg += sizeOfFolder(fileFolders.get(i));
            }
            avg /= fileFolders.size();
            long diff = 0;
            for (int i = 0; i < fileFolders.size(); ++i) {
                if (diff < Math.abs(avg - sizeOfFolder(fileFolders.get(i)))) {
                    diff = Math.abs(avg - sizeOfFolder(fileFolders.get(i)));
                }
            }
            if (diff < solutionDiff) {
                solutionDiff = diff;
                solutionIndex = tests;
            }
        }
        int dirCounter = 0;
        for (List<File> currfolder : solutions.get(solutionIndex)) {
            System.err.println("Folder : " + sizeOfFolder(currfolder) + " Dest = " + destDir + " orig Dir " + origDir);
            String dirPath = destDir + File.separator + "D" + dirCounter;
            File newDir = new File(dirPath);
            newDir.mkdir();
            for (File currFile : currfolder) {
                String cutOffOrigPath = currFile.getAbsolutePath().substring(origDir.length() + 1);
                String destFileName = dirPath + File.separator + cutOffOrigPath;
                File destFile = new File(destFileName);
                int namelength = destFile.getName().length();
                String dirname = destFileName.substring(0, destFileName.length() - namelength - 1);
                File dir = new File(dirname);
                dir.mkdirs();
                copyFile(currFile, destFile);
            }
            dirCounter++;
        }
    }

    private File getPathOfMainClass(String origDir, String mainClass) {
        String path = origDir + File.separator + mainClass.replace('.', File.separatorChar) + ".class";
        File mainClassFile = new File(origDir + File.separator + mainClass.replace('.', File.separatorChar) + ".class");
        if (!mainClassFile.exists()) System.err.println("MAIN CLASS DOES NOT EXIST IN " + path);
        return mainClassFile;
    }

    private void fillFileList(File directory, List<File> listOfAllFiles) {
        File[] files = directory.listFiles();
        for (File currfile : files) {
            if (currfile.isFile()) listOfAllFiles.add(currfile); else if (currfile.isDirectory()) fillFileList(currfile, listOfAllFiles);
        }
    }

    private long sizeOfFolder(List<File> folder) {
        long size = 0;
        for (File currFile : folder) {
            size += currFile.length();
        }
        return size;
    }

    private int getFileOfMaxLength(long length, List<File> files) {
        int index = -1;
        for (int i = 0; i < files.size(); ++i) {
            File currFile = files.get(i);
            if (currFile.length() <= length) {
                index = i;
                break;
            }
        }
        return index;
    }

    private boolean checkFilesForMax(long size, List<File> files) {
        boolean exceeds = false;
        for (int i = 0; i < files.size(); ++i) {
            File currFile = files.get(i);
            if (currFile.length() > size) {
                exceeds = true;
                break;
            }
        }
        return exceeds;
    }

    private void copyFile(File orig, File dest) {
        byte[] buffer = new byte[1024];
        try {
            FileInputStream fis = new FileInputStream(orig);
            FileOutputStream fos = new FileOutputStream(dest, true);
            int readBytes = 0;
            do {
                readBytes = fis.read(buffer);
                if (readBytes > 0) fos.write(buffer, 0, readBytes);
            } while (readBytes > 0);
            fos.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
