package org.uoa.eolus.template;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import org.apache.commons.io.FileUtils;
import org.uoa.eolus.DirectoryException;
import org.uoa.eolus.InternalErrorException;
import org.uoa.nefeli.utils.HostInfo;

public class Nest {

    private String repo;

    private String tempDir;

    private String kernel;

    private String initrd;

    private Map<String, List<HostInfo>> sitesinfo = new HashMap<String, List<HostInfo>>();

    private Random rand = new Random();

    public Nest(String repo, String tempDir, String kernel, String initrd) {
        this.repo = repo;
        (new File(repo)).mkdirs();
        this.tempDir = tempDir;
        this.kernel = kernel;
        this.initrd = initrd;
    }

    public List<String> getSiteConfigurations(String vMtemplateOwner, String vMclass) {
        System.out.println("Looking for Site depoyment description templates");
        File dir = new File(repo + "/" + vMtemplateOwner + "/" + vMclass);
        System.out.println("Looking in directory: " + repo + "/" + vMtemplateOwner + "/" + vMclass);
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith("Site-");
            }
        };
        return Arrays.asList(dir.list(filter));
    }

    public File scheduleVM() {
        return null;
    }

    /**
	 * Build up a OpenNebula specific template description File
	 * @param VMTemplateClass The VM class to be used (currently this ignored)
	 * @param ID The ID of the VM to be created
	 * @param CPU CPU requirements
	 * @param memSize Memory requirements
	 * @param hostName The hostname of the physical system where we want the VM to be deployed 
	 * @return The file containing the OpenNebula template description 
	 * @throws DirectoryException 
	 */
    public File matchVM(String user, String VMTemplateClass, String VMname, String hostName, int cores, int memSize, String[] nets) throws DirectoryException {
        File template = new File(tempDir + "/templateVM-" + VMname);
        String imagesPath = repo + "/" + VMTemplateClass;
        File dir = new File(imagesPath);
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".img") && !name.startsWith("swap") && !name.equalsIgnoreCase("disk.img");
            }
        };
        String[] extraImages = dir.list(filter);
        try {
            FileWriter fstream = new FileWriter(template);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("" + "NAME   = " + VMname + " \n" + "CPU    = " + "0.1" + " \n" + "MEMORY = " + memSize + " \n" + "OS     = [ \n" + " kernel   = \"/boot/" + kernel + "\", \n" + " initrd   = \"/boot/" + initrd + "\", \n" + " root     = \"xvda2 ro\" ] \n" + "DISK   = [ \n" + " type     = \"swap\", \n" + " size     = " + memSize + ", \n" + " target   = \"xvda1\"] \n" + "DISK   = [" + " source   = \"" + repo + "/" + user + "/" + VMTemplateClass + "/disk.img\", \n" + " target   = \"xvda2\", \n " + " readonly = \"no\" ] \n");
            out.write("RAW = [ type=\"xen\", data=\"vcpus=" + cores + "\" ]");
            if (extraImages != null) {
                for (int i = 0; i < extraImages.length; i++) {
                    try {
                        int j = getCounter(extraImages[i]);
                        out.write("DISK   = [" + " source   = \"" + repo + "/" + user + "/" + VMTemplateClass + "/" + extraImages[i] + "\", \n" + " target   = \"xvda" + j + "\", \n " + " readonly = \"no\" ] \n");
                    } catch (Exception x) {
                        System.out.println("No disk index number extracted from " + extraImages[i]);
                    }
                }
            }
            if (hostName != null) {
                String host = hostName.substring(0, hostName.indexOf('.'));
                out.write("REQUIREMENTS = \"HOSTNAME = \\\"" + host + "*\\\"\" \n");
            }
            for (int i = 0; i < nets.length; i++) {
                out.write("NIC    = [ NETWORK = \"" + nets[i] + "\" ] \n");
            }
            out.close();
            fstream.close();
            return template;
        } catch (Exception x) {
            throw new DirectoryException("Connot write ONE template file.", x);
        }
    }

    private int getCounter(String filename) throws Exception {
        int index = filename.lastIndexOf(".");
        index--;
        String strCounter = filename.substring(index, index + 1);
        return Integer.parseInt(strCounter);
    }

    public File matchVM(String user, String mclass, String mname, int cores, int memSize, String[] nets) throws DirectoryException {
        return matchVM(user, mclass, mname, null, cores, memSize, nets);
    }

    public void copyUserTemplate(String user, String template, String target, boolean move) throws DirectoryException {
        String cmd = "mv";
        if (move) cmd = "mv " + repo + "/" + user + "/" + template + " " + repo + "/" + user + "/" + target + "; exit; \n"; else cmd = "cp -r " + repo + "/" + user + "/" + template + " " + repo + "/" + user + "/." + target + "; mv " + repo + "/" + user + "/." + target + " " + repo + "/" + user + "/" + target + "; exit; \n";
        System.out.println("CMD: " + cmd);
        Runtime run = Runtime.getRuntime();
        try {
            Process child = run.exec("/bin/bash");
            BufferedWriter outCommand = new BufferedWriter(new OutputStreamWriter(child.getOutputStream()));
            outCommand.write(cmd);
            outCommand.flush();
            child.waitFor();
            if (child.exitValue() != 0) throw new InternalErrorException("Copy process failed."); else {
                System.out.println("Returning.");
            }
        } catch (Exception e) {
            throw new DirectoryException("Cannot execute rm command.", e);
        }
    }

    public void copyUserToUserTemplate(String fromuser, String touser, String template, String target, boolean move) throws DirectoryException {
        String cmd = "mv";
        if (move) cmd = "mv " + repo + "/" + fromuser + "/" + template + " " + repo + "/" + touser + "/" + target + "; exit; \n"; else cmd = "cp -r " + repo + "/" + fromuser + "/" + template + " " + repo + "/" + touser + "/." + target + "; mv " + repo + "/" + touser + "/." + target + " " + repo + "/" + touser + "/" + target + "; exit; \n";
        System.out.println("CMD: " + cmd);
        Runtime run = Runtime.getRuntime();
        try {
            Process child = run.exec("/bin/bash");
            BufferedWriter outCommand = new BufferedWriter(new OutputStreamWriter(child.getOutputStream()));
            outCommand.write(cmd);
            outCommand.flush();
        } catch (Exception e) {
            throw new DirectoryException("Cannot execute cp/mv command.", e);
        }
    }

    public void removeUserTemplate(String user, String template) throws DirectoryException {
        String cmd = "rm -rf " + repo + "/" + user + "/" + template;
        System.out.println("CMD: " + cmd);
        Runtime run = Runtime.getRuntime();
        try {
            Process child = run.exec(cmd);
        } catch (Exception e) {
            throw new DirectoryException("Cannot execute rm command.", e);
        }
    }

    public String[] getAllUserTemplates(String user) {
        File dir = new File(repo + "/" + user);
        System.out.println("Looking in directory: " + repo + "/" + user);
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return !name.startsWith(".");
            }
        };
        return dir.list(filter);
    }

    public void createUser(String user) throws DirectoryException {
        boolean mkdir = (new File(repo + "/" + user)).mkdir();
        if (!mkdir) {
            System.out.println("Template directory creation failed.");
            File d = new File(repo + "/" + user);
            if ((d == null) || (!d.isDirectory())) {
                throw new DirectoryException("Template directory creation failed");
            }
        }
    }

    public void removeUser(String user) throws DirectoryException {
        deleteDir(new File(repo + "/" + user));
    }

    public void deleteDir(File dir) throws DirectoryException {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                try {
                    deleteDir(new File(dir, children[i]));
                } catch (DirectoryException x) {
                    System.out.println("Cannot remove directory");
                }
            }
        }
        boolean rmdir = dir.delete();
        if (!rmdir) throw new DirectoryException("Cannot remove template directory " + dir.getName() + ".");
    }

    public String[] getAllUsersOfRepo() {
        File dir = new File(repo);
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return !name.startsWith(".");
            }
        };
        return dir.list(filter);
    }

    public File scheduleVMtoHost(String vMtemplateOwner, String vMclass, String vMname, String sitedesc, String host, Integer cores, Integer memSize, String[] nets) throws IOException, DirectoryException {
        File desc = new File(repo + "/" + vMtemplateOwner + "/" + vMclass + "/" + sitedesc);
        String vmdepdesc = FileUtils.readFileToString(desc);
        File template = new File(tempDir + "/templateVM-" + vMname);
        try {
            FileWriter fstream = new FileWriter(template);
            BufferedWriter out = new BufferedWriter(fstream);
            vmdepdesc = vmdepdesc.replaceFirst("%%VMNAME%%", vMname);
            vmdepdesc = vmdepdesc.replace("%%MEMSIZE%%", "" + memSize);
            vmdepdesc = vmdepdesc.replace("%%REPOSITORY%%", repo + "/" + vMtemplateOwner + "/" + vMclass);
            vmdepdesc = vmdepdesc.replace("%%CORES%%", "" + cores);
            vmdepdesc += "\nREQUIREMENTS = \"HOSTNAME = \\\"" + host + "*\\\"\" \n";
            for (int i = 0; i < nets.length; i++) {
                vmdepdesc += "NIC    = [ NETWORK = \"" + nets[i] + "\" ] \n";
            }
            out.write(vmdepdesc);
            out.close();
            fstream.close();
            return template;
        } catch (Exception x) {
            throw new DirectoryException("Connot write ONE template file.", x);
        }
    }

    public void startNewVMScheduling() {
        sitesinfo.clear();
    }

    public void loadHostInfo(String s, List<HostInfo> hi) {
        System.out.println("Candidate site: " + s);
        sitesinfo.put(s, hi);
    }

    public File newVMScheduling(String vMtemplateOwner, String vMclass, String vMname, Integer cores, Integer memSize, String[] nets) throws IOException, DirectoryException, InternalErrorException {
        while (sitesinfo.size() != 0) {
            int siteindex = Math.abs(rand.nextInt()) % sitesinfo.size();
            Iterator<Entry<String, List<HostInfo>>> it = sitesinfo.entrySet().iterator();
            Entry<String, List<HostInfo>> currentsite = null;
            for (int i = 0; i < siteindex + 1; i++) {
                currentsite = it.next();
            }
            String sitename = currentsite.getKey();
            sitesinfo.remove(sitename);
            List<HostInfo> hostinfo = currentsite.getValue();
            while (hostinfo.size() != 0) {
                int hostindex = Math.abs(rand.nextInt()) % hostinfo.size();
                HostInfo hi = hostinfo.get(hostindex);
                if (isEnough(hi, cores, memSize)) {
                    return scheduleVMtoHost(vMtemplateOwner, vMclass, vMname, sitename, hi.name, cores, memSize, nets);
                } else {
                    hostinfo.remove(hostindex);
                }
            }
        }
        throw new InternalErrorException("Connot schedule VM.");
    }

    private boolean isEnough(HostInfo hi, Integer cores, Integer memSize) {
        System.out.println("Comparing hi.Mem_free > memSize " + hi.Mem_free + " > " + memSize);
        if (hi.Mem_free > memSize * 1024) return true;
        return false;
    }
}
