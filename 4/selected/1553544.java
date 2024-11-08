package net.emotivecloud.vrmm.vtm.script;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.emotivecloud.commons.Domain;
import net.emotivecloud.vrmm.vtm.VtMException;
import net.emotivecloud.vrmm.vtm.commons.ExecuteScript;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.upc.ac.www.rfl.DiskInformationDocument;
import edu.upc.ac.www.rfl.DiskInformationType;
import edu.upc.ac.www.rfl.InstallationDescriptionDocument;
import edu.upc.ac.www.rfl.InstallationDescriptionType;
import edu.upc.ac.www.rfl.KernelType;
import edu.upc.ac.www.rfl.MachineReferenceDocument;
import edu.upc.ac.www.rfl.MachineReferenceType;
import edu.upc.ac.www.rfl.PackageFormatType;
import edu.upc.ac.www.rfl.PackagesListType;
import edu.upc.ac.www.rfl.ReleaseType;
import edu.upc.ac.www.rfl.RequirementsDocument;
import edu.upc.ac.www.rfl.RequirementsType;
import edu.upc.ac.www.rfl.SatisfiedRequirementsDocument;
import edu.upc.ac.www.rfl.SatisfiedRequirementsType;

public class ScriptManager {

    private Log log = LogFactory.getLog(ScriptManager.class);

    private String path = "/alex/ResourceManager";

    private String rmScript = "bash " + path + "/scripts/rm.sh";

    private String checkerScript = "bash " + path + "/scripts/Checker.sh";

    private String imageInstallerScript = "bash " + path + "/scripts/ImageInstaller.sh";

    private final int NUM_INFO = 3;

    private final int IMAGE = 0;

    private final int KERNEL = 1;

    private final int INITRD = 2;

    private final int RELEASE = 0;

    private final int PACKAGEFORMAT = 1;

    public ScriptManager() {
        URL url = ScriptManager.class.getResource("ScriptManager.class");
        if (url != null) {
            path = url.toString();
            if (path.startsWith("jar:")) {
                path = path.replaceFirst("jar:", "");
                if (path.startsWith("file:")) path = path.replaceFirst("file:", "");
                String jarPath = path.substring(0, path.indexOf("!"));
                rmScript = "bash " + unJar(jarPath, "bin/rm.sh");
                checkerScript = "bash " + unJar(jarPath, "bin/Checker.sh");
                imageInstallerScript = "bash " + unJar(jarPath, "bin/ImageInstaller.sh");
                unJar(jarPath, "bin/megacache.sh");
                unJar(jarPath, "bin/deb");
                unJar(jarPath, "bin/functions");
                unJarStart(jarPath, "bin/rmextension");
                log.info("Using VtM script:            " + rmScript);
                log.info("Using Checker script:        " + checkerScript);
                log.info("Using ImageInstaller script: " + imageInstallerScript);
            } else if (path.startsWith("file:")) {
                path = path.replaceFirst("file:", "");
                path = path.substring(0, path.lastIndexOf("VtM/") + 4);
                rmScript = "bash " + path + "bin/rm.sh";
                checkerScript = "bash " + path + "bin/Checker.sh";
                imageInstallerScript = "bash " + path + "bin/ImageInstaller.sh";
                log.info("Using VtM script:            " + rmScript);
                log.info("Using Checker script:        " + checkerScript);
                log.info("Using ImageInstaller script: " + imageInstallerScript);
            } else {
                log.error("Unknown path " + url + ": using " + rmScript);
            }
        } else {
            log.error("Searching VtM script. Using default: " + rmScript);
            log.error("Searching Checker script. Using default: " + checkerScript);
            log.error("Searching ImageInstaller script. Using default: " + imageInstallerScript);
        }
    }

    public String construct(Domain domain, int numCPU) throws VtMException {
        return this.construct(domain, numCPU, domain.getMemory());
    }

    /**
	 * Create a new domain.
	 * @param domain Domain that will be started.
	 * @return If the domain has been successfully created.
	 */
    public String construct(Domain domain, int cpu, int memory) throws VtMException {
        String command;
        ExecuteScript es = new ExecuteScript();
        command = rmScript + " construct " + domain.getName();
        command += " --id " + domain.getId();
        command += " --mem " + memory;
        command += " --cpu " + cpu;
        command += " --ip " + domain.getIp();
        if (!domain.getIp().equals("0.0.0.0")) command += " --gw " + domain.getGateway();
        if (domain.getHomeSize() > 0) command += " --home " + domain.getHomeSize();
        if (domain.getSwapSize() > 0) command += " --swap " + domain.getSwapSize();
        if (domain.getInitrdPath() != null) command += " --ramdisk " + domain.getInitrdPath();
        if (domain.getKernelPath() != null) command += " --kernel " + domain.getKernelPath();
        if (domain.getDiskPath() != null) command += " --disk " + domain.getDiskPath();
        if (domain.getExtension() != null && !domain.getExtension().equals("")) command += " --extension " + domain.getExtension();
        if (domain.isCheckpointable()) command += " --aufs";
        es.executeCommand(command);
        MachineReferenceDocument mrd = MachineReferenceDocument.Factory.newInstance();
        MachineReferenceType mrt = mrd.addNewMachineReference();
        mrt.setName(domain.getName());
        mrt.setID(domain.getId());
        mrt.setMachineIP(domain.getIp());
        mrt.setMemory(domain.getMemory());
        return getPath();
    }

    /**
	 * Create a new domain and run this.
	 * @param domain Domain that will be started and ran.
	 * @return If the domain has been successfully created and ran.
	 */
    public String create(Domain domain, int numCPU) throws VtMException {
        return this.create(domain, numCPU, domain.getMemory());
    }

    public String create(Domain domain, int cpu, int memory) throws VtMException {
        String command;
        ExecuteScript es = new ExecuteScript();
        command = rmScript + " create " + domain.getName();
        command += " --id " + domain.getId();
        command += " --mem " + memory;
        command += " --cpu " + cpu;
        command += " --ip " + domain.getIp();
        if (!domain.getIp().equals("0.0.0.0")) command += " --gw " + domain.getGateway();
        if (domain.getHomeSize() > 0) command += " --home " + domain.getHomeSize();
        if (domain.getSwapSize() > 0) command += " --swap " + domain.getSwapSize();
        if (domain.getInitrdPath() != null) command += " --ramdisk " + domain.getInitrdPath();
        if (domain.getKernelPath() != null) command += " --kernel " + domain.getKernelPath();
        if (domain.getDiskPath() != null) command += " --disk " + domain.getDiskPath();
        if (domain.getExtension() != null && !domain.getExtension().equals("")) command += " --extension " + domain.getExtension();
        if (domain.isCheckpointable()) command += " --aufs";
        es.executeCommand(command);
        MachineReferenceDocument mrd = MachineReferenceDocument.Factory.newInstance();
        MachineReferenceType mrt = mrd.addNewMachineReference();
        mrt.setName(domain.getName());
        mrt.setID(domain.getId());
        mrt.setMachineIP(domain.getIp());
        mrt.setMemory(domain.getMemory());
        return getPath();
    }

    public void recover(String name) throws VtMException {
        this.unifyPath(name);
        ExecuteScript es = new ExecuteScript();
        es.executeCommand(rmScript + " recover " + name);
    }

    /**
	 * Unify paths for migrated domains.
	 * @param hostname
	 * @param name
	 * @throws VtMException
	 */
    public void unifyPath(String hostname, String name) throws VtMException {
        String command = rmScript + " link " + hostname + " " + name;
        ExecuteScript es = new ExecuteScript();
        es.executeCommand(command);
    }

    /**
	 * Unify paths for checkpointed domains.
	 * @param name
	 * @throws VtMException
	 */
    public void unifyPath(String name) throws VtMException {
        String command = rmScript + " linklocal " + name;
        ExecuteScript es = new ExecuteScript();
        es.executeCommand(command);
    }

    public List<String> getExtensions() {
        LinkedList<String> ret = new LinkedList<String>();
        String command = rmScript + " extensions";
        try {
            ExecuteScript es = new ExecuteScript();
            String out = es.executeCommand(command);
            String[] extensions = out.split("\n");
            for (int i = 0; i < extensions.length; i++) {
                if (!extensions[i].equals("")) ret.add(extensions[i]);
            }
        } catch (VtMException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
	 * Check Pool for its cached contents
	 * @param Requirements Requirements Document
	 * @return Satisfied Requirement Document
	 * @throws Exception
	 * @param Requirements
	 * @return
	 * @throws Exception
	 */
    public SatisfiedRequirementsDocument checkRequirements(RequirementsDocument Requirements) throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String script = checkerScript;
        String command;
        RequirementsType rt = Requirements.getRequirements();
        SatisfiedRequirementsDocument srd = SatisfiedRequirementsDocument.Factory.newInstance();
        SatisfiedRequirementsType srt = srd.addNewSatisfiedRequirements();
        srt.setName(rt.getName());
        String name = rt.getName();
        command = script + " prepare --name " + name;
        String URL = es.mutexOperation(command);
        srt.setMachineHome(URL);
        command = script + " check --kernel ";
        if (rt.isSetKernel()) {
            KernelType kt = rt.getKernel();
            if (kt.isSetKernelVersion()) {
                command = command + kt.getKernelVersion();
            }
            if (kt.isSetConstraint()) {
                command = command + " --constraint " + kt.getConstraint().toString();
            }
        }
        command = command + " --name " + name;
        String kernel = es.mutexOperation(command);
        String[] sKernel = kernel.split("\n");
        if (sKernel.length != 0) {
            KernelType skt = srt.addNewKernel();
            skt.setKernelVersion(kernel);
        }
        command = script + " check --release ";
        if (rt.isSetRelease()) {
            ReleaseType rrt = rt.getRelease();
            if (rrt.isSetCodename()) {
                command = command + rrt.getCodename();
            }
            command = command + " --format " + rrt.getPackageFormat().toString();
        }
        command = command + " --name " + name;
        String info = es.mutexOperation(command);
        String[] Info = info.split("\n");
        if (Info.length != 0) {
            ReleaseType releaset = srt.addNewRelease();
            releaset.setCodename(Info[RELEASE]);
            releaset.setPackageFormat(PackageFormatType.Enum.forString(Info[PACKAGEFORMAT]));
        }
        return srd;
    }

    /**
	 * Install image phase: prepare a file system and installs the software requirements
	 * @param installationDescription
	 * @return DiskInformationDocument
	 * @throws Exception
	 */
    public DiskInformationDocument installImage(InstallationDescriptionDocument installationDescription) throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String command = imageInstallerScript;
        InstallationDescriptionType idt = installationDescription.getInstallationDescription();
        command = command + " install --name";
        String name = idt.getName();
        command = command + " " + name;
        command = command + " --release";
        command = command + " " + idt.getRelease().getCodename();
        command = command + " --format";
        command = command + " " + idt.getRelease().getPackageFormat().toString();
        if (idt.isSetDiskSize()) {
            command = command + " --disk-size";
            command = command + " " + idt.getDiskSize();
        }
        if (idt.isSetPackagesList()) {
            PackagesListType plt = idt.getPackagesList();
            int numpkg = plt.sizeOfPackageArray();
            String lp = new String();
            if (numpkg != 0) lp = plt.getPackageArray(0).getPackageName();
            for (int i = 1; i < numpkg; i++) {
                lp = lp + "," + plt.getPackageArray(i).getPackageName();
            }
            command = command + " --packages";
            command = command + " " + lp;
        }
        String info = es.mutexOperation(command);
        String[] diskInfo = new String[this.NUM_INFO];
        DiskInformationDocument did = DiskInformationDocument.Factory.newInstance();
        DiskInformationType dit = did.addNewDiskInformation();
        diskInfo = info.split("\n");
        System.out.println("[VtMScriptManager]");
        dit.setKernelLocation(diskInfo[this.KERNEL]);
        dit.setDiskLocation(diskInfo[this.IMAGE]);
        if (diskInfo.length > this.INITRD) {
            dit.setInitrdLocation(diskInfo[this.INITRD]);
        }
        return did;
    }

    /**
	 * Starts running an already created domain.
	 * @param domain Domain that will be started.
	 * @return If the domain has been successfully created.
	 */
    public void run(Domain domain) throws VtMException {
        ExecuteScript es = new ExecuteScript();
        es.executeCommand(rmScript + " run " + domain.getName());
    }

    /**
	 * Destroys a running domain.
	 * @param name Name of the domain that will be destroyed.
	 * @return If the domain has been successfully destroyed.
	 */
    public void destroy(String name) throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String cmd = rmScript + " destroy " + name;
        es.executeCommand(cmd);
    }

    public void destroy(String name, String id) throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String cmd = rmScript + " destroy " + name;
        if (id != null) cmd += " --id " + id;
        es.executeCommand(cmd);
    }

    /**
	 * Shutdown a running domain.
	 * @param name Name of the domain that will be shutdowned.
	 * @return If the domain has been successfully shutdowned.
	 */
    public void shutdown(String name) throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String cmd = rmScript + " shutdown " + name;
        es.executeCommand(cmd);
    }

    public void shutdown(String name, String id) throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String cmd = rmScript + " shutdown " + name;
        if (id != null) cmd += " --id " + id;
        es.executeCommand(cmd);
    }

    /**
	 * Save a domain.
	 * @param name Name of the domain to save.
	 * @return If the domain has been successfully saved.
	 */
    public void save(String name) throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String cmd = rmScript + " save " + name;
        es.executeCommand(cmd);
    }

    public void save(String name, String id) throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String cmd = rmScript + " save " + name;
        if (id != null) cmd += " --id " + id;
        es.executeCommand(cmd);
    }

    /**
	 * Create base system image files and compile (only compile in KVM Virtual Machines)
	 * @param NO.
	 * @return If the image has been successfully created.
	 */
    public void base() throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String cmd = rmScript + " base ";
        es.executeCommand(cmd);
    }

    public void onlykernel() throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String cmd = rmScript + " onlykernel ";
        es.executeCommand(cmd);
    }

    public void onlybase() throws VtMException {
        ExecuteScript es = new ExecuteScript();
        String cmd = rmScript + " onlybase ";
        es.executeCommand(cmd);
    }

    /**
	 * Extract a given entry from its JAR file.
	 * @param jarPath
	 * @param jarEntry
	 */
    private String unJar(String jarPath, String jarEntry) {
        String path;
        if (jarPath.lastIndexOf("lib/") >= 0) path = jarPath.substring(0, jarPath.lastIndexOf("lib/")); else path = jarPath.substring(0, jarPath.lastIndexOf("/"));
        String relPath = jarEntry.substring(0, jarEntry.lastIndexOf("/"));
        try {
            new File(path + "/" + relPath).mkdirs();
            JarFile jar = new JarFile(jarPath);
            ZipEntry ze = jar.getEntry(jarEntry);
            File bin = new File(path + "/" + jarEntry);
            IOUtils.copy(jar.getInputStream(ze), new FileOutputStream(bin));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path + "/" + jarEntry;
    }

    /**
	 * Extracts all the files from a JAR file that starts with a given string.
	 * @param jarPath
	 * @param jarEntryStart
	 */
    private void unJarStart(String jarPath, String jarEntryStart) {
        String path;
        if (jarPath.lastIndexOf("lib/") >= 0) path = jarPath.substring(0, jarPath.lastIndexOf("lib/")); else path = jarPath.substring(0, jarPath.lastIndexOf("/"));
        String relPath = jarEntryStart.substring(0, jarEntryStart.lastIndexOf("/"));
        try {
            new File(path + "/" + relPath).mkdirs();
            JarFile jar = new JarFile(jarPath);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String jarEntry = entry.getName();
                if (jarEntry.startsWith(jarEntryStart)) {
                    ZipEntry ze = jar.getEntry(jarEntry);
                    File bin = new File(path + "/" + jarEntry);
                    IOUtils.copy(jar.getInputStream(ze), new FileOutputStream(bin));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPath() throws VtMException {
        File archivo = null;
        FileReader fr = null;
        BufferedReader br = null;
        String line = "";
        String getPath = "";
        try {
            archivo = new File("/tmp/Emotive_ScriptManager.xml");
            fr = new FileReader(archivo);
            br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) getPath = line;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fr) {
                    fr.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return getPath;
    }
}
