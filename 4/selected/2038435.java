package org.fao.fenix.FromDom5ToDom4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

public class Translate {

    private String srcRoot;

    private String destRoot = "src/main/java/org/fao/fenix/domain4";

    private String namePack;

    private List blackList;

    public void setNamepack(String namepack) {
        this.namePack = namepack;
    }

    public String getNamePack() {
        return this.namePack;
    }

    public void setDom5Path() {
        String absolutepath = new File(destRoot).getAbsolutePath();
        String workspace = absolutepath.substring(0, absolutepath.length() - destRoot.length() - 14);
        this.destRoot = absolutepath + "/";
        this.srcRoot = workspace + "fenix-domain/src/main/java/org/fao/fenix/domain/";
    }

    public void copyD5InD4(File src) throws IOException {
        if (src.isDirectory()) {
            File listdir[] = src.listFiles();
            for (int i = 0; i < listdir.length; i++) {
                if (this.blackList.contains(listdir[i].getName())) {
                    continue;
                }
                this.copyD5InD4(new File(src + "/" + listdir[i].getName()));
            }
        } else {
            String relativepath = src.toString().substring(srcRoot.length());
            File copydest = new File(destRoot + "client/" + relativepath.substring(0, relativepath.length() - src.getName().length()));
            File modfile = new File(destRoot + "client/" + relativepath.substring(0, relativepath.length()));
            Pattern p = Pattern.compile("[a-zA-Z0-9]+.java");
            Matcher m = p.matcher(src.getName());
            boolean b = m.matches();
            if (b) {
                createNewFile(src, modfile);
                System.out.println(relativepath.substring(0, relativepath.length()));
            } else {
                FileUtils.copyFileToDirectory(src, copydest);
            }
        }
    }

    private void createNewFile(File original, File copy) throws IOException {
        List contentfile = FileUtils.readLines(original, null);
        File createDir = new File(copy.getAbsolutePath().substring(0, copy.getAbsolutePath().length() - copy.getName().length()));
        String namepackage = original.toString().substring(srcRoot.length());
        namepackage = namepackage.substring(0, namepackage.length() - 5);
        namepackage = namepackage.replace("/", ".");
        this.setNamepack(namepackage);
        createDir.mkdirs();
        this.changePackageName(contentfile);
        if (!this.serializableParser(contentfile)) this.addSerializable(contentfile);
        this.deleteAnnotations(contentfile);
        this.deleteImport(contentfile);
        this.searchGeneric(contentfile);
        this.organizerImport(contentfile);
        FileUtils.writeLines(copy, null, contentfile);
    }

    public void deleteImport(List f) {
        Pattern p1 = Pattern.compile("^import javax.persistence.*");
        Matcher m1;
        boolean b1;
        Pattern p2 = Pattern.compile("^import org.hibernate.*");
        Matcher m2;
        boolean b2;
        for (int i = 0; i < f.size(); i++) {
            m1 = p1.matcher((String) f.get(i));
            b1 = m1.matches();
            m2 = p2.matcher((String) f.get(i));
            b2 = m2.matches();
            if (b1 || b2) {
                f.remove(i);
                i--;
            }
        }
    }

    private void organizerImport(List f) {
        Pattern p = Pattern.compile("^import org.fao.fenix.domain.*");
        Matcher m;
        boolean b;
        for (int i = 0; i < f.size(); i++) {
            m = p.matcher((String) f.get(i));
            b = m.matches();
            if (b) {
                String tmp = ((String) f.get(i)).substring(28);
                f.add(i++, "import org.fao.fenix.domain4.client." + tmp);
                f.remove(i);
                i--;
            }
        }
    }

    private String deleteGeneric(String line) {
        while (line.contains("<")) {
            int count = 1;
            int c = line.indexOf('<');
            c++;
            while (line.charAt(c) != '>') {
                if (line.charAt(c) == '<') count++;
                c++;
            }
            int i = 0;
            while (i <= line.length()) {
                if (line.charAt(i) == '>') count--;
                if (count == 0) break;
                i++;
            }
            line = line.substring(0, line.indexOf('<')) + line.substring(i + 1);
        }
        return line;
    }

    public void searchGeneric(List nf) {
        Pattern p = Pattern.compile("^.*<??([a-zA-Z])+(, *([a-zA-Z]+))*>.*");
        Matcher m;
        boolean b;
        for (int i = 0; i < nf.size(); i++) {
            m = p.matcher((String) nf.get(i));
            b = m.matches();
            if (b) {
                String wg = deleteGeneric((String) nf.get(i));
                nf.add(i++, wg);
                nf.remove(i);
                i--;
            }
        }
    }

    public void deleteAnnotations(List nf) {
        Iterator iter = nf.iterator();
        Pattern p = Pattern.compile("^[ \t]*@.+");
        Matcher m = null;
        boolean b;
        while (iter.hasNext()) {
            String checkel = (String) iter.next();
            m = p.matcher(checkel);
            b = m.matches();
            if (b) {
                iter.remove();
            }
        }
    }

    /**
	 * Check the existence of import java.io.Serializable; in a List of String.
	 * 
	 * @param list
	 *            The List of String to check.
	 * @return True if exists, false otherwise.
	 */
    public boolean serializableParser(List list) {
        boolean ret = false;
        for (int i = 0; i < list.size(); i++) {
            if (((String) list.get(i)).equals("import java.io.Serializable;")) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    /**
	 * Change the package name adding "4.client" in the path.
	 * 
	 * @param list
	 *            The List of String to check.
	 * 
	 */
    public void changePackageName(List list) {
        for (int i = 0; i < list.size(); i++) {
            if (((String) list.get(i)).substring(0, 7).equals("package")) {
                String app = (String) list.get(i);
                for (int j = 0; j < app.length() - 6; j++) if (app.substring(j, j + 6).equals("domain")) {
                    String four = app.substring(0, j + 6) + "4.client" + app.substring(j + 6, app.length());
                    list.add(i++, four);
                    list.remove(i);
                    break;
                }
                break;
            }
        }
    }

    /**
	 * This method add the import and implements Serializable to a class from a
	 * List of String.
	 * 
	 * @param list
	 *            The List tof String to modify.
	 * 
	 */
    public void addSerializable(List list) {
        Pattern p = Pattern.compile("^public class.+");
        Matcher m = null;
        boolean b;
        for (int i = 0; i < list.size(); i++) {
            m = p.matcher((String) list.get(i));
            b = m.matches();
            if (b) {
                String app = "";
                if (((String) list.get(i)).endsWith("{")) {
                    app = ((String) list.get(i)).substring(0, ((String) list.get(i)).length() - 1);
                    app += " implements Serializable {";
                    list.add(i++, app);
                    list.remove(i);
                    list.add(i - 1, "import java.io.Serializable;");
                    break;
                }
            }
        }
    }

    public Translate() throws IOException {
        File bl = new File("src/main/java/org/fao/fenix/FromDom5ToDom4/blackList.txt");
        this.blackList = new ArrayList();
        List l = FileUtils.readLines(bl, null);
        Pattern p = Pattern.compile("^#.*#.*");
        Matcher m;
        boolean b;
        for (int i = 3; i < l.size(); i++) {
            m = p.matcher((String) l.get(i));
            b = m.matches();
            if (b) {
                String dir = (String) l.get(i);
                dir = dir.substring(1);
                dir = dir.substring(0, dir.indexOf('#'));
                this.blackList.add(dir);
            }
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) throws IOException {
        Translate t = new Translate();
        t.setDom5Path();
        System.out.println("Starting translation From Domain5 To Domain4");
        t.copyD5InD4(new File(t.srcRoot));
        System.out.println("Done");
    }
}
