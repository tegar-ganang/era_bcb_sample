package support.translate;

import java.awt.datatransfer.StringSelection;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Scanner;
import support.translate.parser.ASTDefinition;
import support.translate.parser.ASTDefinitionClass;
import support.translate.parser.Node;
import support.translate.parser.ParseException;
import support.translate.parser.Translator;
import support.translate.parser.Node.NodeType;

/**
 * Find all language files in the set of languages; add new keys as needed to match the template.
 * @author Sam
 *
 */
public class UpdateLanguages {

    public static void main(String[] args) throws ParseException, FileNotFoundException, IOException {
        InputStream input = new BufferedInputStream(UpdateLanguages.class.getResourceAsStream("definition_template"));
        Translator t = new Translator(input, "UTF8");
        Node template = Translator.Start();
        File langs = new File("support/support/translate/languages");
        for (File f : langs.listFiles()) {
            if (f.getName().endsWith(".lng")) {
                input = new BufferedInputStream(new FileInputStream(f));
                try {
                    Translator.ReInit(input, "UTF8");
                } catch (java.lang.NullPointerException e) {
                    new Translator(input, "UTF8");
                }
                Node newFile = Translator.Start();
                ArrayList<Addition> additions = new ArrayList<Addition>();
                syncKeys(template, newFile, additions);
                ArrayList<String> fileLines = new ArrayList<String>();
                Scanner scanner = new Scanner(new BufferedReader(new FileReader(f)));
                while (scanner.hasNextLine()) {
                    fileLines.add(scanner.nextLine());
                }
                int offset = 0;
                for (Addition a : additions) {
                    System.out.println("Key added " + a + " to " + f.getName());
                    if (a.afterLine < 0 || a.afterLine >= fileLines.size()) {
                        fileLines.add(a.getAddition(0));
                    } else {
                        fileLines.add(a.afterLine + (offset++) + 1, a.getAddition(0));
                    }
                }
                f.delete();
                Writer writer = new BufferedWriter(new FileWriter(f));
                for (String s : fileLines) writer.write(s + "\n");
                writer.close();
                System.out.println("Language " + f.getName() + " had " + additions.size() + " additions");
            }
        }
        File defFile = new File(langs, "language.lng");
        defFile.delete();
        defFile.createNewFile();
        InputStream copyStream = new BufferedInputStream(UpdateLanguages.class.getResourceAsStream("definition_template"));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(defFile));
        int c = 0;
        while ((c = copyStream.read()) >= 0) out.write(c);
        out.close();
        System.out.println("Languages updated.");
    }

    private static void syncKeys(Node template, Node compare, ArrayList<Addition> additions) {
        for (int i = 0; i < template.jjtGetNumChildren(); i++) {
            Node child = template.jjtGetChild(i);
            if (child.getNodeType() == NodeType.Definition || child.getNodeType() == NodeType.DefinitionClass) {
                boolean matched = false;
                Node match = null;
                for (int j = 0; j < compare.jjtGetNumChildren(); j++) {
                    Node compChild = compare.jjtGetChild(j);
                    if (compChild.equals(child)) {
                        matched = true;
                        match = compChild;
                        break;
                    }
                }
                if (!matched) {
                    int line = 0;
                    if (compare instanceof ASTDefinitionClass) {
                        line = ((ASTDefinitionClass) compare).getEndLine();
                    } else {
                        line = -1;
                    }
                    if (child.getNodeType() == NodeType.Definition) {
                        ASTDefinition def = (ASTDefinition) child;
                        additions.add(new KeyAddition(def.getKey(), line));
                    } else if (child.getNodeType() == NodeType.DefinitionClass) {
                        ASTDefinitionClass def = (ASTDefinitionClass) child;
                        ClassAddition classAdd = new ClassAddition(def.getName(), line);
                        additions.add(classAdd);
                        addClass(classAdd, def, 0);
                    }
                } else if (child.getNodeType() == NodeType.DefinitionClass) {
                    syncKeys(child, match, additions);
                }
            }
        }
    }

    private static void addClass(ClassAddition parent, ASTDefinitionClass node, int line) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            Node child = node.jjtGetChild(i);
            if (child instanceof ASTDefinition) {
                ASTDefinition def = (ASTDefinition) child;
                parent.addChild(new KeyAddition(def.getKey(), line));
            } else if (child instanceof ASTDefinitionClass) {
                ASTDefinitionClass defClass = (ASTDefinitionClass) child;
                ClassAddition classAdd = new ClassAddition(defClass.getName(), line);
                parent.addChild(classAdd);
                addClass(classAdd, defClass, line);
            }
        }
    }

    private abstract static class Addition {

        public int afterLine;

        private Addition(int atLine) {
            super();
            this.afterLine = atLine;
        }

        public abstract String getAddition(int indent);
    }

    private static class ClassAddition extends Addition {

        public String name;

        private ArrayList<Addition> children = new ArrayList<Addition>();

        public ArrayList<Addition> getChildren() {
            return children;
        }

        private ClassAddition(String name, int atLine) {
            super(atLine);
            this.name = name;
        }

        public void addChild(Addition c) {
            children.add(c);
        }

        @Override
        public String getAddition(int indent) {
            String ind = "";
            for (int i = 0; i < indent; i++) ind += "\t";
            String base = ind + name + "{\n\n";
            for (Addition a : children) {
                base += a.getAddition(indent + 1) + "\n";
            }
            base += ind + "}\n";
            return base;
        }

        @Override
        public String toString() {
            return "ClassAddition " + this.name;
        }
    }

    private static class KeyAddition extends Addition {

        public String key;

        private KeyAddition(String key, int atLine) {
            super(atLine);
            this.key = key;
        }

        @Override
        public String getAddition(int indent) {
            String ind = "";
            for (int i = 0; i < indent; i++) ind += "\t";
            return ind + key + "~\n";
        }

        @Override
        public String toString() {
            return "DefAddition " + this.key;
        }
    }
}
