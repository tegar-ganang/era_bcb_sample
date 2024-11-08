package org.qtitools.constructr.assessment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import org.qtitools.qti.node.test.AssessmentTest;
import org.qtitools.qti.node.test.NavigationMode;
import org.qtitools.qti.node.test.SubmissionMode;
import org.qtitools.qti.node.test.TestPart;
import org.qtitools.util.ContentPackage;

public class AssessmentModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private static String TOOL_NAME = "constructr";

    private static String TOOL_VERSION = "1.0";

    List<SectionModel> sections;

    String title;

    public AssessmentModel() {
        sections = new ArrayList<SectionModel>();
    }

    public void addSection(SectionModel m) {
        sections.add(m);
    }

    public void moveSectionUp(SectionModel m) {
        int oldindex = sections.indexOf(m);
        int newindex = oldindex - 1;
        if (newindex < 0) return;
        sections.remove(m);
        sections.add(newindex, m);
    }

    public void moveSectionDown(SectionModel m) {
        int oldindex = sections.indexOf(m);
        int newindex = oldindex + 1;
        if (newindex >= sections.size()) return;
        sections.remove(m);
        sections.add(newindex, m);
    }

    public List<SectionModel> getSections() {
        return sections;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TreeModel convertToTreeModel() {
        TreeModel model = null;
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(this);
        add(rootNode, getSections());
        model = new DefaultTreeModel(rootNode);
        return model;
    }

    private void add(DefaultMutableTreeNode parent, List sub) {
        for (Iterator i = sub.iterator(); i.hasNext(); ) {
            Object o = i.next();
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(o);
            parent.add(child);
            if (o instanceof SectionModel) {
                add(child, (((SectionModel) o).getItems()));
            }
        }
    }

    public AssessmentTest convertToQTI() {
        AssessmentTest test = new AssessmentTest();
        test.setToolName(TOOL_NAME);
        test.setToolVersion(TOOL_VERSION);
        test.setIdentifier("ASSESS-" + java.util.UUID.randomUUID().toString());
        test.setTitle(title);
        TestPart tp = new TestPart(test);
        tp.setIdentifier("TP");
        tp.setNavigationMode(NavigationMode.NONLINEAR);
        tp.setSubmissionMode(SubmissionMode.SIMULTANEOUS);
        test.getTestParts().add(tp);
        int sectionNo = 0;
        for (SectionModel sm : sections) {
            tp.getAssessmentSections().add(sm.convertToQTI(tp, sectionNo));
            sectionNo++;
        }
        return test;
    }

    public File createContentPackage() throws IOException {
        File dir = createTempDir("constructr", null);
        int scount = 0;
        for (SectionModel sm : sections) {
            int icount = 0;
            for (ItemModel im : sm.items) {
                ContentPackage icp = im.item.resolveItem();
                icp.unpack(new File(dir.toString() + File.separator + "S" + scount + "I" + icount));
                File[] items = icp.getItems();
                if (items.length != 1) {
                    System.err.println("Error: invalid item content package - more than one item!");
                    System.err.println(items);
                    System.err.println(items.length);
                    System.err.println(icp.getManifest());
                    return null;
                }
                im.itempath = items[0].getAbsolutePath().replace(dir.getAbsolutePath(), "");
                if (im.itempath.startsWith(File.separator)) im.itempath = im.itempath.substring(1);
                System.out.println(im.itempath);
                icount++;
            }
            scount++;
        }
        System.out.println("Temp CP dir: " + dir);
        FileWriter fw = new FileWriter(new File(dir.toString() + File.separator + "assessment.xml"));
        fw.write(convertToQTI().toXmlString());
        fw.close();
        fw = new FileWriter(new File(dir.toString() + File.separator + "imsmanifest.xml"));
        fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        fw.write("<manifest xmlns=\"http://www.imsglobal.org/xsd/imscp_v1p1\" " + "identifier=\"" + java.util.UUID.randomUUID() + "\" >\n");
        fw.write("\t<resources>\n");
        fw.write("\t\t<resource identifier=\"RES-" + java.util.UUID.randomUUID() + "\" type=\"imsqti_test_xmlv2p1\" href=\"assessment.xml\" />\n");
        for (SectionModel sm : sections) {
            for (ItemModel im : sm.items) {
                fw.write("\t\t<resource identifier=\"RES-" + java.util.UUID.randomUUID() + "\" type=\"imsqti_item_xmlv2p0\" href=\"" + im.itempath + "\" />\n");
            }
        }
        fw.write("\t</resources>\n");
        fw.write("</manifest>\n");
        fw.close();
        File zipfile = new File(dir.toString() + ".zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipfile));
        zipDir(dir, zos, dir);
        zos.close();
        delete(dir);
        return zipfile;
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) delete(files[i]);
        }
        file.delete();
    }

    public void zipDir(File zipDir, ZipOutputStream zos, File root) {
        try {
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[1024];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    zipDir(f, zos, root);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                ZipEntry anEntry = new ZipEntry(f.getPath().replace(root + File.separator, ""));
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File createTempDir(String prefix, File dir) throws IOException {
        File tempFile = File.createTempFile(prefix, "", dir);
        if (!tempFile.delete()) throw new IOException();
        if (!tempFile.mkdir()) throw new IOException();
        return tempFile;
    }

    @Override
    public String toString() {
        return getTitle();
    }

    public void deleteSection(SectionModel sm) {
        sections.remove(sm);
    }
}
