package at.priv.hofer.itunes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import javax.swing.JOptionPane;
import org.apache.commons.lang.StringEscapeUtils;
import uk.ac.shef.wit.simmetrics.similaritymetrics.BlockDistance;
import uk.ac.shef.wit.simmetrics.similaritymetrics.CosineSimilarity;
import uk.ac.shef.wit.simmetrics.similaritymetrics.EuclideanDistance;
import uk.ac.shef.wit.simmetrics.similaritymetrics.OverlapCoefficient;
import at.priv.hofer.tools.HTMLEntities;
import at.priv.hofer.tools.java.IProgressMonitor;

public class Repair {

    private TreeMap<String, File> allMusicFiles;

    private String[] allMusicFileNames;

    private TreeMap<File, Boolean> allFoundMusicFiles;

    private class FoundItem {

        File f;

        Float distance;

        public String toString() {
            return f.getName();
        }
    }

    public Repair() {
        allFoundMusicFiles = new TreeMap<File, Boolean>();
        allMusicFiles = new TreeMap<String, File>();
    }

    /**
	 * @param args
	 * @throws IOException
	 * @throws URISyntaxException
	 */
    public void repair(File musicDirectory, File iTunesLib, IProgressMonitor monitor) throws IOException {
        File iTunesXML = new File(iTunesLib.getParent() + File.separator + "iTunes Music Library.xml");
        monitor.setTaskName("Searching '" + musicDirectory + "' directory for mp3 files");
        init(musicDirectory);
        allMusicFileNames = allMusicFiles.keySet().toArray(new String[0]);
        Arrays.sort(allMusicFileNames);
        monitor.setTaskName("Found " + allMusicFiles.size() + " mp3s in '" + musicDirectory + "'");
        File tempFile = File.createTempFile("itunesrepair", ".xml", iTunesLib.getParentFile());
        int changed = repairLibrary(iTunesXML, tempFile, monitor);
        if (changed > 0) {
            monitor.setTaskName("Making backup for '" + iTunesLib.getName() + "' and '" + iTunesXML.getName() + "' in folder backup_" + "'" + new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()) + "'");
            backupFile(iTunesXML);
            backupFile(iTunesLib);
            monitor.setTaskName("Your iTunes library is NOT up-to-date");
            monitor.setTaskName("Your iTunes library is NOT up-to-date");
            monitor.setTaskName("  -> " + changed + " files were re-linked");
            monitor.setTaskName("  -> iTunes library will be re-created");
            if (iTunesLib.exists() && iTunesLib.isFile()) {
                destroyFile(iTunesLib);
            }
            if (new File(iTunesXML.getAbsolutePath() + ".old").exists()) {
                new File(iTunesXML.getAbsolutePath() + ".old").delete();
            }
            iTunesXML.renameTo(new File(iTunesXML.getAbsolutePath() + ".old"));
            tempFile.renameTo(new File(iTunesXML.getAbsolutePath()));
            monitor.setTaskName("Backups of your iTunes library were made in folder backup_" + "'" + new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()) + "'");
            monitor.setTaskName("finished");
        } else {
            JOptionPane.showMessageDialog(null, "No changes necessary, your iTunes library is up-to-date");
            tempFile.delete();
            monitor.setTaskName("finished");
        }
        showUnlinkedFiles(musicDirectory, monitor);
    }

    private void destroyFile(File f) throws IOException {
        f.delete();
        f.createNewFile();
    }

    private int repairLibrary(File input, File output, IProgressMonitor monitor) throws IOException {
        int changed = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));
        String strLine;
        while ((strLine = br.readLine()) != null) {
            if (strLine.indexOf("<key>Location</key>") >= 0) {
                String path = strLine.substring(strLine.indexOf("<string>") + "<string>".length(), strLine.lastIndexOf("</string>"));
                path = StringEscapeUtils.unescapeXml(path);
                path = path.replaceAll(" ", "%20");
                path = path.replace("//localhost", "");
                try {
                    File f = null;
                    try {
                        f = new File(new URL(path).toURI());
                    } catch (IllegalArgumentException e) {
                        String name = path.substring(path.lastIndexOf("/") + 1);
                        name = URLDecoder.decode(name, "UTF8");
                        f = new File(name);
                    }
                    if (!f.exists()) {
                        File foundFile = findFile(f, monitor);
                        if (foundFile != null) {
                            bw.write(strLine.substring(0, strLine.indexOf("<string>") + "<string>".length()));
                            String newFileName = foundFile.toURI().toASCIIString();
                            newFileName = newFileName.replace("file:/", "file://localhost/");
                            newFileName = newFileName.replace("&", "&#38;");
                            bw.write(newFileName);
                            bw.write(strLine.substring(strLine.indexOf("</string>")));
                            bw.write("\n");
                            changed++;
                            allFoundMusicFiles.put(foundFile, true);
                        } else {
                            bw.write(strLine + "\n");
                        }
                    } else {
                        allFoundMusicFiles.put(f, true);
                        bw.write(strLine + "\n");
                    }
                } catch (URISyntaxException e) {
                    bw.write(strLine + "\n");
                }
            } else {
                bw.write(strLine + "\n");
            }
        }
        bw.flush();
        bw.close();
        br.close();
        return changed;
    }

    private File findFile(File f, IProgressMonitor monitor) {
        String name = f.getName();
        int pos = Arrays.binarySearch(allMusicFileNames, name);
        if (pos >= 0) {
            File foundFile = allMusicFiles.get(name);
            monitor.setTaskName(f.getAbsolutePath() + " has been moved to " + foundFile.getAbsolutePath());
            return foundFile;
        }
        BlockDistance bd = new BlockDistance();
        CosineSimilarity cs = new CosineSimilarity();
        EuclideanDistance ed = new EuclideanDistance();
        OverlapCoefficient oc = new OverlapCoefficient();
        List<FoundItem> found = new LinkedList<FoundItem>();
        for (String s : allMusicFileNames) {
            float cmpVal1 = bd.getSimilarity(s, name);
            float cmpVal2 = cs.getSimilarity(s, name);
            float cmpVal3 = ed.getSimilarity(s, name);
            float cmpVal4 = oc.getSimilarity(s, name);
            if (cmpVal1 > 0.6 && cmpVal2 > 0.6 && cmpVal3 > 0.6 && cmpVal4 > 0.6) {
                FoundItem fi = new FoundItem();
                fi.f = allMusicFiles.get(s);
                fi.distance = cmpVal1 + cmpVal2 + cmpVal3 + cmpVal4;
                found.add(fi);
            }
        }
        if (found.size() == 0) {
            monitor.setTaskName(f.getAbsolutePath() + " not found any more. Was it deleted?");
            return null;
        } else if (found.size() == 1) {
            File foundFile = found.get(0).f;
            int n = JOptionPane.showConfirmDialog(null, "Was\n" + f.getName() + "\nrenamed to " + foundFile.getName() + "?", "", JOptionPane.YES_NO_OPTION);
            if (n == JOptionPane.NO_OPTION) {
                monitor.setTaskName(f.getAbsolutePath() + " cancelled");
                return null;
            }
            monitor.setTaskName(f.getAbsolutePath() + " re-linked to " + foundFile.getAbsolutePath());
            return foundFile;
        } else {
            Collections.sort(found, new Comparator<FoundItem>() {

                @Override
                public int compare(FoundItem o1, FoundItem o2) {
                    return o1.distance.compareTo(o2.distance);
                }
            });
            FoundItem value = (FoundItem) JOptionPane.showInputDialog(null, "Please select the matching new file name for\n" + f.getName() + "\n\n", "Select File", JOptionPane.QUESTION_MESSAGE, null, found.toArray(), found.get(0));
            if (value == null) {
                monitor.setTaskName("cancelled selection of " + f.getAbsolutePath());
                return null;
            } else {
                File foundFile = value.f;
                monitor.setTaskName(f.getAbsolutePath() + " re-linked to " + foundFile.getAbsolutePath());
                return foundFile;
            }
        }
    }

    private void init(File musicFolder) {
        File[] files = musicFolder.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        });
        for (File f : files) {
            allMusicFiles.put(f.getName(), f);
        }
        File[] dirs = musicFolder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        for (File d : dirs) {
            init(d);
        }
    }

    private void showUnlinkedFiles(File musicFolder, IProgressMonitor monitor) {
        File[] allStoredFiles = allMusicFiles.values().toArray(new File[0]);
        List<File> allUnlinkedFiles = new ArrayList<File>();
        Arrays.sort(allStoredFiles);
        for (File file : allStoredFiles) {
            Boolean found = allFoundMusicFiles.get(file);
            if (found == null || !found.booleanValue()) {
                allUnlinkedFiles.add(file);
            }
        }
        if (allUnlinkedFiles.size() > 0) {
            Object[] options = { "Yes, move", "Yes, copy", "No, thanks", "Show the list of files" };
            boolean chosen = false;
            int n = -1;
            while (!chosen) {
                chosen = true;
                n = JOptionPane.showOptionDialog(null, "There were found " + allUnlinkedFiles.size() + " files in your music\ndirectory, which currently are not in your iTunes library.\nShall these files be copied/moved to " + musicFolder.getAbsolutePath() + File.separator + "UNLINKED?\nThen you can easily import these files into iTunes", "Unlinked files", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[3]);
                if (n == 3) {
                    chosen = false;
                    Object[] names = new String[allUnlinkedFiles.size() > 20 ? allUnlinkedFiles.size() : 20];
                    for (int i = 0; i < allUnlinkedFiles.size(); i++) {
                        names[i] = allUnlinkedFiles.get(i).getName();
                    }
                    JOptionPane.showInputDialog(null, allUnlinkedFiles.size() + " Unlinked files", "Unlinked files", JOptionPane.PLAIN_MESSAGE, null, names, null);
                }
            }
            File unlinkedDir = new File(musicFolder.getAbsolutePath() + File.separator + "UNLINKED");
            if (n == 0) {
                unlinkedDir.mkdirs();
                for (File file : allUnlinkedFiles) {
                    Boolean found = allFoundMusicFiles.get(file);
                    if (found == null || !found.booleanValue()) {
                        monitor.setTaskName("moving -> " + file.getName());
                        file.renameTo(new File(unlinkedDir, file.getName()));
                    }
                }
            } else if (n == 1) {
                unlinkedDir.mkdirs();
                for (File file : allUnlinkedFiles) {
                    Boolean found = allFoundMusicFiles.get(file);
                    if (found == null || !found.booleanValue()) {
                        monitor.setTaskName("copying -> " + file.getName());
                        try {
                            copyFile(file, new File(unlinkedDir.getAbsolutePath() + File.separator + file.getName()));
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }
    }

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    private void backupFile(File f) {
        File backupFile = new File(f.getParent() + File.separator + "backup_" + new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()) + File.separator + f.getName());
        if (!backupFile.getParentFile().exists()) {
            backupFile.getParentFile().mkdirs();
        }
        try {
            copyFile(f, backupFile);
        } catch (IOException e) {
        }
    }
}
