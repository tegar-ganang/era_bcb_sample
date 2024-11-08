package br.gov.demoiselle.eclipse.util.utility.classwriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import br.gov.demoiselle.eclipse.util.Activator;
import br.gov.demoiselle.eclipse.util.utility.FileUtil;

public class RoundTrip {

    public static final String FWK_METHOD_BEGIN_BLOCK = "/*@fwk-method-begin*/";

    public static final String FWK_METHOD_END_BLOCK = "/*@fwk-method-end*/";

    public static final String FWK_METHODS_BEGIN_BLOCK = "/*@fwk-methods-begin*/";

    public static final String FWK_METHODS_END_BLOCK = "/*@fwk-methods-end*/";

    public static ClassHelper merge(String srcPath, ClassHelper clazz, boolean isInterface) {
        try {
            List<MethodRTHelper> methodsRT = getMethods(srcPath, isInterface);
            if (methodsRT != null) {
                for (MethodRTHelper method : methodsRT) {
                    if (method.getName().startsWith("get") || method.getName().startsWith("")) {
                        MethodHelper methodClass = findMethod(clazz.getMethods(), method.getName(), method.getArgs());
                        if (methodClass != null) {
                            methodClass.setBody(method.getBody());
                            methodClass.setComment(method.getComment());
                        } else {
                            if (clazz.getStringMethods() == null) {
                                clazz.setStringMethods(new ArrayList<String>());
                            }
                            if (!isInterface) {
                                clazz.getStringMethods().add(method.toString());
                            } else {
                                clazz.getStringMethods().add(method.toString() + ";");
                            }
                        }
                    }
                }
            }
            String source = FileUtil.readFile(srcPath);
            mergeAST(clazz, source);
        } catch (Exception e) {
            Activator.log(e);
        }
        return clazz;
    }

    public static FieldHelper findField(List<FieldHelper> fields, String name) {
        if (fields != null) {
            for (FieldHelper fieldHelper : fields) {
                if (fieldHelper.getName().trim().equals(name.trim())) {
                    return fieldHelper;
                }
            }
        }
        return null;
    }

    public static MethodHelper findMethod(List<MethodHelper> methods, String name, List<String> params) {
        if (methods != null) {
            for (MethodHelper method : methods) {
                boolean find = true;
                if (method.getName().equals(name.trim())) {
                    if (params != null && method.getParameters() != null) {
                        if (params.size() == method.getParameters().size()) {
                            for (int i = 0; i < params.size(); i++) {
                                if (!params.get(i).equals(method.getParameters().get(i).getType().getFullName()) && !params.get(i).equals(method.getParameters().get(i).getType().getName())) {
                                    find = false;
                                }
                            }
                        } else {
                            find = false;
                        }
                    } else if ((params == null && (method.getParameters() != null && method.getParameters().size() > 0)) || (method.getParameters() == null && (params != null && params.size() > 0))) {
                        find = false;
                    }
                } else {
                    find = false;
                }
                if (find) {
                    return method;
                }
            }
        }
        return null;
    }

    public static MethodRTHelper findMethodRT(List<MethodRTHelper> rtMethods, String name, List<ParameterHelper> params) {
        for (MethodRTHelper method : rtMethods) {
            boolean find = true;
            if (method.getName().equals(name)) {
                if (params != null && method.getArgs() != null) {
                    if (params.size() == method.getArgs().size()) {
                        for (int i = 0; i < params.size(); i++) {
                            if (!params.get(i).getType().getFullName().equals(method.getArgs()) && !params.get(i).getType().getName().equals(method.getArgs())) {
                                find = false;
                            }
                        }
                    } else {
                        find = false;
                    }
                } else if ((params == null && (method.getArgs() != null && method.getArgs().size() >= 0)) || (method.getArgs() == null && (params != null && params.size() >= 0))) {
                    find = false;
                }
            }
            if (find) {
                return method;
            }
        }
        return null;
    }

    public static List<MethodRTHelper> getMethods(String fileName, boolean isInterface) {
        StringBuffer body = null;
        boolean copyLine = false;
        boolean finishMethods = false;
        try {
            File fileExists = new File(fileName);
            if (fileExists.exists()) {
                FileReader file = new FileReader(fileName);
                BufferedReader input = new BufferedReader(file);
                try {
                    String line = null;
                    while ((line = input.readLine()) != null && !finishMethods) {
                        if (line.trim().contains(FWK_METHODS_BEGIN_BLOCK)) {
                            copyLine = true;
                            body = new StringBuffer();
                        } else if (line.trim().contains(FWK_METHODS_END_BLOCK)) {
                            body.append(line.substring(0, line.indexOf(FWK_METHODS_END_BLOCK)));
                            finishMethods = true;
                        } else if (copyLine) {
                            body.append(line).append(System.getProperty("line.separator"));
                        }
                    }
                } finally {
                    input.close();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (body != null) {
            return getMethodsHelper(body.toString(), isInterface);
        } else {
            return null;
        }
    }

    public static List<MethodRTHelper> getMethodsHelper(String bodies, boolean isInterface) {
        List<MethodRTHelper> result = new ArrayList<MethodRTHelper>();
        int pilhaBody = 0;
        boolean isComment = false;
        boolean isCommentBody = false;
        boolean isCommentLine = false;
        boolean isBody = false;
        boolean isAnnotation = false;
        String ultimoChar = "";
        String palavra = "";
        String signature = "";
        String comment = "";
        String annotation = "";
        if (bodies != null && bodies.length() > 0) {
            for (int i = 0; i < bodies.length(); i++) {
                String character = String.valueOf(bodies.charAt(i));
                if (isAnnotation) {
                    if (character.equals("\n")) {
                        isAnnotation = false;
                        annotation = palavra;
                        palavra = "";
                    } else {
                        palavra = palavra + character;
                    }
                } else if (!isBody) {
                    if (ultimoChar.concat(character).equals("*/")) {
                        isComment = false;
                        comment = comment.substring(0, comment.length() - 1);
                    } else if (isComment) {
                        comment = comment + character;
                    } else if (ultimoChar.concat(character).equals("/*")) {
                        isComment = true;
                    } else if (character.equals("{")) {
                        signature = palavra;
                        pilhaBody++;
                        isBody = true;
                        palavra = "";
                    } else if (character.equals("@")) {
                        isAnnotation = true;
                        palavra = character;
                    } else if (isInterface && character.equals(";")) {
                        signature = palavra;
                        MethodRTHelper method = new MethodRTHelper();
                        method.setBody(null);
                        if (method.getComment() != null) {
                            method.setComment("/*".concat(method.getComment()).concat(comment).concat("*/"));
                        } else if (!comment.equals("")) {
                            method.setComment("/*".concat(comment).concat("*/"));
                        }
                        if (!signature.equals("")) method.setSignature(signature);
                        if (!annotation.equals("")) method.setAnnotation(annotation);
                        result.add(method);
                        palavra = "";
                    } else if (!isLixo(character)) {
                        palavra = palavra + character;
                    }
                } else {
                    if (!isCommentBody && !isCommentLine) {
                        if (character.equals("}")) {
                            if (pilhaBody == 1) {
                                MethodRTHelper method = new MethodRTHelper();
                                method.setBody(palavra);
                                if (method.getComment() != null) {
                                    method.setComment("/*".concat(method.getComment()).concat(comment).concat("*/"));
                                } else if (!comment.equals("")) {
                                    method.setComment("/*".concat(comment).concat("*/"));
                                }
                                if (!signature.equals("")) method.setSignature(signature);
                                if (!annotation.equals("")) method.setAnnotation(annotation);
                                result.add(method);
                                isBody = false;
                                pilhaBody = 0;
                                palavra = "";
                                signature = "";
                                comment = "";
                                annotation = "";
                            } else {
                                pilhaBody--;
                                palavra = palavra + character;
                            }
                        } else {
                            if (character.equals("{")) {
                                pilhaBody++;
                            }
                            if (ultimoChar.concat(character).equals("/*")) {
                                isCommentBody = true;
                            }
                            if (ultimoChar.concat(character).equals("//")) {
                                isCommentLine = true;
                            }
                            palavra = palavra + character;
                        }
                    } else {
                        if (isCommentBody && ultimoChar.concat(character).equals("*/")) {
                            isCommentBody = false;
                        } else if (isCommentLine && character.equals("\n")) {
                            isCommentLine = false;
                        }
                        palavra = palavra + character;
                    }
                }
                ultimoChar = String.valueOf(bodies.charAt(i));
            }
        }
        for (MethodRTHelper methodRTHelper : result) {
            MethodRTHelper newMethod = extractSignature(methodRTHelper.getSignature());
            methodRTHelper.setArgs(newMethod.getArgs());
            methodRTHelper.setName(newMethod.getName());
        }
        return result;
    }

    public static boolean isLixo(String c) {
        return c.equals("/") || c.equals("\n");
    }

    public static List<String> getMethodsBodies(String fileName) {
        List<String> result = new ArrayList<String>();
        StringBuffer body = null;
        boolean copyLine = false;
        try {
            BufferedReader input = new BufferedReader(new FileReader(fileName));
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    if (line.trim().contains(FWK_METHOD_BEGIN_BLOCK)) {
                        copyLine = true;
                        body = new StringBuffer();
                        body.append(line.substring(line.trim().indexOf(FWK_METHOD_BEGIN_BLOCK), line.length()));
                    } else if (copyLine) {
                        body.append("//").append(line).append(System.getProperty("line.separator"));
                    }
                    if (line.trim().contains(FWK_METHOD_END_BLOCK)) {
                        copyLine = false;
                        result.add(body.toString());
                        body = null;
                    }
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public static MethodRTHelper extractSignature(String signature) {
        MethodRTHelper method = new MethodRTHelper();
        String beforeArgs = signature.substring(0, signature.indexOf('('));
        String args = signature.substring(signature.indexOf('(') + 1, signature.indexOf(')'));
        String name = beforeArgs.substring(beforeArgs.lastIndexOf(' '));
        method.setName(name.trim());
        List<String> sigArgs = new ArrayList<String>();
        String param = "";
        for (int i = 0; i < args.length(); i++) {
            String character = String.valueOf(args.charAt(i));
            if (!character.equals(",")) {
                param = param + character;
            } else {
                sigArgs.add(param.trim());
                param = "";
            }
        }
        if (!param.trim().equals("")) {
            sigArgs.add(param.trim());
        }
        for (int i = 0; i < sigArgs.size(); i++) {
            String typeArg = sigArgs.get(i);
            typeArg = typeArg.trim().substring(0, typeArg.indexOf(' '));
            sigArgs.set(i, typeArg);
        }
        method.setArgs(sigArgs);
        return method;
    }

    public static ClassHelper mergeAST(ClassHelper clazz, String source) {
        char[] arraySource = source.toCharArray();
        CompilationUnit unit = RoundTrip.parse(arraySource);
        for (Object oneImport : unit.imports()) {
            ImportDeclaration node = (ImportDeclaration) oneImport;
            String importName = node.getName().toString();
            ClassRepresentation repImport = new ClassRepresentation(importName);
            if (clazz != null) {
                clazz.addImport(repImport);
            }
        }
        for (Object type : unit.types()) {
            TypeDeclaration typeDec = (TypeDeclaration) type;
            for (FieldDeclaration fieldDec : typeDec.getFields()) {
                VariableDeclarationFragment variable = (VariableDeclarationFragment) fieldDec.fragments().get(0);
                FieldHelper newField = new FieldHelper();
                newField.setModifier(fieldDec.getModifiers());
                if (fieldDec.modifiers() != null && fieldDec.modifiers().size() > 0) {
                    for (Object modifier : fieldDec.modifiers()) {
                        if (modifier instanceof MarkerAnnotation) {
                            MarkerAnnotation markerAnnotation = (MarkerAnnotation) modifier;
                            if (newField.getAnnotation() == null || newField.getAnnotation().trim().length() == 0) {
                                newField.setAnnotation(markerAnnotation.toString());
                            } else {
                                newField.setAnnotation(newField.getAnnotation().concat(markerAnnotation.toString()));
                            }
                        }
                    }
                }
                newField.setName(variable.getName().toString());
                ClassRepresentation fieldType = new ClassRepresentation();
                fieldType.setName(fieldDec.getType().toString());
                newField.setType(fieldType);
                if (variable.getInitializer() != null) {
                    newField.setValue(variable.getInitializer().toString());
                }
                int index = clazz.findField(newField.getName());
                if (index >= 0) {
                    clazz.getFields().set(index, newField);
                } else {
                    clazz.addField(newField);
                }
            }
        }
        return clazz;
    }

    public static CompilationUnit parse(char[] unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        return (CompilationUnit) parser.createAST(null);
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        String file = FileUtil.readFile("/home/08673707773/Ambiente_Demoiselle/workspaceFramework10/wizard/commons/src/main/java/br/gov/demoiselle/plugin/commons/util/classwriter/RoundTrip.java");
        RoundTrip.mergeAST(null, file);
    }
}
