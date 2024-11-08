package pogo.gene;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *	This class generates Cpp source file for ServerClass files.
 *
 * @author	$Author: pascal_verdier $
 * @version	$Revision: 3.9 $
 */
public class CppStateMachine extends PogoGene implements PogoDefs {

    public CppStateMachine(PogoClass pogo) {
        super(pogo);
    }

    void generateSource(String template, String src_filename) throws FileNotFoundException, SecurityException, IOException, PogoException {
        PogoString readcode;
        if (mustGenerate(src_filename)) {
            readcode = new PogoString(PogoUtil.readFile(template, templateClass, pogo.class_name));
            readcode = PogoUtil.removeLogMessages(readcode);
        } else readcode = new PogoString(PogoUtil.readFile(src_filename));
        for (int i = 0; i < pogo.attributes.size(); i++) {
            Attrib attr = pogo.attributes.attributeAt(i);
            String signature = attr.allowedSignatureMethod(pogo.class_name);
            if (readcode.indexOf(signature) < 0) {
                signature = attr.allowedFullSignatureMethod(pogo.class_name);
                int start = readcode.indexOf("Commands Allowed Methods");
                start = readcode.lastIndexOf("//============", start);
                start--;
                String method = signature;
                method += "\n{\n		//	" + endGeneTag + "\n";
                method += "\n		//	" + startGeneTag + "\n";
                method += "\treturn true;\n}\n";
                readcode.insert(start, method);
            }
        }
        for (int i = 0; i < pogo.commands.size(); i++) {
            Cmd cmd = pogo.commands.cmdAt(i);
            if (cmd.virtual_method) continue;
            String signature = cmd.allowedSignatureMethod(pogo.class_name);
            if (readcode.indexOf(signature) < 0) {
                signature = cmd.allowedFullSignatureMethod(pogo.class_name);
                int start = readcode.indexOf(endNamespace);
                String method = signature;
                method += "\n{\n		//	" + endGeneTag + "\n";
                method += "\n		//	" + startGeneTag + "\n";
                method += "\treturn true;\n}\n";
                readcode.insert(start, method);
            }
        }
        String newcode = updateAllowedMethods(readcode);
        PogoUtil.writeFile(src_filename, newcode);
    }

    private String updateAllowedMethods(PogoString readcode) {
        for (int i = 0; i < pogo.attributes.size(); i++) {
            Attrib attr = pogo.attributes.attributeAt(i);
            String signature = attr.allowedFullSignatureMethod(pogo.class_name);
            int start = readcode.indexOf(signature);
            String method = readcode.extractMethodCore(start);
            String user_part = getAllowedUserPart(method);
            method = signature + "\n{" + method + "}";
            String test = attr.allowedCore();
            String newmethod = signature + "\n";
            if (test != null) {
                newmethod += test + user_part + "\n";
                newmethod += "		return false;\n";
                newmethod += "	}\n	return true;\n}";
                readcode.replace(method, newmethod);
            } else {
                newmethod += "{\n		//	" + user_part + "\n";
                newmethod += "	return true;\n}";
                readcode.replace(method, newmethod);
            }
            readcode.replace(method, newmethod);
        }
        for (int i = 0; i < pogo.commands.size(); i++) {
            Cmd cmd = pogo.commands.cmdAt(i);
            if (cmd.virtual_method) continue;
            String signature = cmd.allowedFullSignatureMethod(pogo.class_name);
            int start = readcode.indexOf(signature);
            String method = readcode.extractMethodCore(start);
            String user_part = getAllowedUserPart(method);
            method = signature + "\n{" + method + "}";
            String test = cmd.allowedCore();
            String newmethod = signature + "\n";
            if (test != null) {
                if (pogo.deviceImpl < 3) user_part = getAllowedUserPart(cmd);
                newmethod += test + user_part + "\n";
                newmethod += "		return false;\n";
                newmethod += "	}\n	return true;\n}";
            } else {
                newmethod += "{\n		//	" + user_part + "\n";
                newmethod += "	return true;\n}";
                readcode.replace(method, newmethod);
            }
            readcode.replace(method, newmethod);
        }
        String newcode = removeDeletedAttrCmdAllowedMethods(readcode);
        removeOldAllowedMethods();
        return newcode;
    }

    private String removeDeletedAttrCmdAllowedMethods(PogoString code) {
        String start_signature = "bool " + pogo.class_name + "::is_";
        int start = 0;
        while ((start = code.indexOf(start_signature, start)) > 0) {
            int end = code.indexOf("_allowed(", start);
            if (end < 0) {
                start++;
                continue;
            }
            String name = code.substring(start + start_signature.length(), end);
            boolean still_exists = false;
            for (int i = 0; i < pogo.commands.size(); i++) if (pogo.commands.cmdAt(i).toString().equals(name)) still_exists = true;
            for (int i = 0; i < pogo.attributes.size(); i++) if (pogo.attributes.attributeAt(i).toString().equals(name)) still_exists = true;
            if (!still_exists) {
                end = code.inMethod(end);
                end = code.outMethod(end);
                start = code.lastIndexOf("//+-------", start);
                String method = code.substring(start, end);
                code.remove(method);
            }
            start = end;
        }
        return code.str;
    }

    private void removeOldAllowedMethods() {
        String filename = pogo.projectFiles.getServerClass();
        try {
            PogoString readcode = new PogoString(PogoUtil.readFile(filename));
            for (int i = 0; i < pogo.commands.size(); i++) {
                Cmd cmd = pogo.commands.cmdAt(i);
                if (cmd.virtual_method) continue;
                String patern = cmd.cmd_class + "::is_allowed";
                int start = readcode.indexOf(patern);
                if (start < 0) continue;
                start = readcode.lastIndexOf("}", start);
                int end = readcode.inMethod(start);
                end = readcode.outMethod(end);
                end = readcode.nextCr(end);
                end = readcode.indexOf("//", end);
                start = readcode.lastIndexOf("//+-------", start);
                String method = readcode.substring(start, end);
                readcode.remove(method);
            }
            PogoUtil.writeFile(filename, readcode.str);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private String getAllowedUserPart(String method) {
        int idx0 = method.indexOf(endGeneTag);
        int idx1 = method.indexOf(startGeneTag);
        idx1 += startGeneTag.length();
        return method.substring(idx0, idx1);
    }

    private String getAllowedUserPart(Cmd cmd) {
        System.out.println("getAllowedUserPart for " + cmd);
        String filename = pogo.projectFiles.getServerClass();
        try {
            PogoString readcode = new PogoString(PogoUtil.readFile(filename));
            String signature = "bool " + cmd.name + "Cmd::is_allowed(Tango::DeviceImpl *device, const CORBA::Any &in_any)";
            int start, end;
            if ((start = readcode.indexOf(signature)) > 0) {
                start = readcode.inMethod(start);
                end = readcode.outMethod(start);
                String method = readcode.substring(start, end);
                return getAllowedUserPart(method);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return endGeneTag + "\n\n" + "		//	" + startGeneTag + "\n";
    }
}
