package net.simplemodel.core.parser.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.simplemodel.core.SimpleModelStatus;
import net.simplemodel.core.SimplemodelCore;
import net.simplemodel.core.ast.SMAttributeDeclaration;
import net.simplemodel.core.ast.SMAttributeReference;
import net.simplemodel.core.ast.SMDotExpression;
import net.simplemodel.core.ast.SMFieldDeclaration;
import net.simplemodel.core.ast.SMImportStatement;
import net.simplemodel.core.ast.SMIndexDeclaration;
import net.simplemodel.core.ast.SMKeyStatement;
import net.simplemodel.core.ast.SMModuleDeclaration;
import net.simplemodel.core.ast.SMOppositeStatement;
import net.simplemodel.core.ast.SMTypeDeclaration;
import net.simplemodel.core.ast.SMTypeReference;
import net.simplemodel.core.parser.ISimplemodelParseFailureHandler;
import net.simplemodel.core.parser.ISimplemodelParser;
import net.simplemodel.core.parser.SMParseUtil;
import net.simplemodel.core.parser.SimplemodelParseFailureException;
import net.simplemodel.core.parser.SyntaxError;
import net.simplemodel.core.parser.SyntaxErrorConstants;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.dltk.ast.declarations.FieldDeclaration;
import org.eclipse.dltk.ast.declarations.TypeDeclaration;
import org.eclipse.dltk.ast.expressions.Expression;

public class SimplemodelParserImpl implements ISimplemodelParser {

    private void addSupertypesFields(Map<String, List<SMTypeDeclaration>> typeMap, Map<String, Map<String, List<FieldDeclaration>>> fieldMap, String typeName, Set<String> processed) {
        if (!processed.add(typeName)) return;
        List<SMTypeDeclaration> candidates = typeMap.get(typeName);
        if (candidates == null || candidates.size() != 1) return;
        SMTypeDeclaration typeDeclaration = candidates.get(0);
        for (Object o : typeDeclaration.getSuperClasses().getChilds()) {
            SMTypeReference typeReference = (SMTypeReference) o;
            addSupertypesFields(typeMap, fieldMap, typeDeclaration.getName(), processed);
            Map<String, List<FieldDeclaration>> superFields = fieldMap.get(typeReference.getName());
            if (superFields != null) {
                Map<String, List<FieldDeclaration>> current = fieldMap.get(typeDeclaration.getName());
                if (current == null) {
                    current = new TreeMap<String, List<FieldDeclaration>>();
                    fieldMap.put(typeDeclaration.getName(), current);
                }
                for (Map.Entry<String, List<FieldDeclaration>> entry : superFields.entrySet()) {
                    List<FieldDeclaration> fields = current.get(entry.getKey());
                    if (fields == null) {
                        fields = new LinkedList<FieldDeclaration>();
                        current.put(entry.getKey(), fields);
                    }
                    fields.addAll(entry.getValue());
                }
            }
        }
    }

    private Set<SMTypeDeclaration> findAllSuperTypes(Map<String, List<SMTypeDeclaration>> typeMap, TypeDeclaration td) {
        Deque<SMTypeDeclaration> stack = new LinkedList<SMTypeDeclaration>();
        for (Object o : td.getSuperClasses().getChilds()) {
            SMTypeReference typeReference = (SMTypeReference) o;
            List<SMTypeDeclaration> superTypeDeclaration = typeMap.get(typeReference.getName());
            if (superTypeDeclaration != null && superTypeDeclaration.size() == 1) stack.push(superTypeDeclaration.get(0));
        }
        Set<SMTypeDeclaration> result = new HashSet<SMTypeDeclaration>();
        while (!stack.isEmpty()) {
            SMTypeDeclaration current = stack.pop();
            if (result.add(current)) {
                for (Object o : current.getSuperClasses().getChilds()) {
                    SMTypeReference typeReference = (SMTypeReference) o;
                    List<SMTypeDeclaration> superTypeDeclaration = typeMap.get(typeReference.getName());
                    if (superTypeDeclaration != null && superTypeDeclaration.size() == 1) stack.push(superTypeDeclaration.get(0));
                }
            }
        }
        return result;
    }

    private Set<SMTypeDeclaration> findConstructorPassedTypes(Map<String, List<SMTypeDeclaration>> typeMap, SMTypeDeclaration td) {
        Deque<SMTypeDeclaration> stack = new LinkedList<SMTypeDeclaration>();
        for (SMTypeReference typeReference : td.getImplementationInstantiationArgumentTypes()) {
            List<SMTypeDeclaration> superTypeDeclaration = typeMap.get(typeReference.getName());
            if (superTypeDeclaration != null && superTypeDeclaration.size() == 1) stack.push(superTypeDeclaration.get(0));
        }
        Set<SMTypeDeclaration> result = new HashSet<SMTypeDeclaration>();
        while (!stack.isEmpty()) {
            SMTypeDeclaration current = stack.pop();
            if (result.add(current)) {
                for (SMTypeReference typeReference : current.getImplementationInstantiationArgumentTypes()) {
                    List<SMTypeDeclaration> superTypeDeclaration = typeMap.get(typeReference.getName());
                    if (superTypeDeclaration != null && superTypeDeclaration.size() == 1) stack.push(superTypeDeclaration.get(0));
                }
            }
        }
        return result;
    }

    private boolean isValidJavaIdentifier(String name) {
        if (name == null || name.length() < 1) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public SMModuleDeclaration parse(InputStream in, String charset, final ISimplemodelParseFailureHandler failureHandler) throws SimplemodelParseFailureException {
        String sourceCode = null;
        try {
            StringBuilder sb = new StringBuilder();
            in = readFully(in, charset, sb);
            sourceCode = sb.toString();
            final AtomicBoolean anyError = new AtomicBoolean();
            anyError.set(false);
            SimplemodelLexer lex = new SimplemodelLexer(new ANTLRInputStream(in, charset)) {

                @Override
                public void reportError(RecognitionException e) {
                    anyError.set(true);
                    failureHandler.reportError(new SyntaxError(e, getErrorMessage(e, this.getTokenNames())));
                    super.reportError(e);
                }
            };
            CommonTokenStream tokens = new CommonTokenStream(lex);
            SimplemodelParser g = new SimplemodelParser(tokens) {

                @Override
                public void reportError(RecognitionException e) {
                    anyError.set(true);
                    failureHandler.reportError(new SyntaxError(e, getErrorMessage(e, this.getTokenNames())));
                    super.reportError(e);
                }
            };
            SMModuleDeclaration moduleDeclaration = g.moduleDeclaration().node;
            moduleDeclaration.smBuild();
            if (!anyError.get()) postValidate(moduleDeclaration, failureHandler);
            moduleDeclaration.setSourceCode(sourceCode);
            moduleDeclaration.setValid(!anyError.get());
            return moduleDeclaration;
        } catch (RuntimeException e) {
            throw new SimplemodelParseFailureException(new SimpleModelStatus(IStatus.ERROR, SimplemodelCore.PLUGIN_ID, "Internal Parse Error", e, sourceCode), Collections.<SyntaxError>emptyList());
        } catch (IOException e) {
            throw new SimplemodelParseFailureException(new SimpleModelStatus(IStatus.ERROR, SimplemodelCore.PLUGIN_ID, "Unable to read source", e, sourceCode), Collections.<SyntaxError>emptyList());
        } catch (RecognitionException e) {
            throw new SimplemodelParseFailureException(new SimpleModelStatus(IStatus.ERROR, SimplemodelCore.PLUGIN_ID, "Internal Parse Error", e, sourceCode), Collections.<SyntaxError>emptyList());
        } finally {
            failureHandler.endOfParse();
        }
    }

    @Override
    public SMModuleDeclaration parse(String in, String charset, ISimplemodelParseFailureHandler failureHandler) throws SimplemodelParseFailureException, UnsupportedEncodingException {
        return parse(new ByteArrayInputStream(in.getBytes(charset)), charset, failureHandler);
    }

    private void postValidate(SMModuleDeclaration moduleDeclaration, ISimplemodelParseFailureHandler failureHandler) {
        Map<String, List<SMTypeDeclaration>> typeMap = new TreeMap<String, List<SMTypeDeclaration>>();
        Map<String, Map<String, List<FieldDeclaration>>> fieldMap = new TreeMap<String, Map<String, List<FieldDeclaration>>>();
        Map<String, List<SMImportStatement>> imports = new TreeMap<String, List<SMImportStatement>>();
        for (SMImportStatement importStatement : moduleDeclaration.getImportStatements()) {
            List<SMImportStatement> imps = imports.get(importStatement.getClassName());
            if (imps == null) {
                imps = new LinkedList<SMImportStatement>();
                imports.put(importStatement.getClassName(), imps);
            }
            imps.add(importStatement);
        }
        for (TypeDeclaration td : moduleDeclaration.getTypes()) {
            SMTypeDeclaration typeDeclaration = (SMTypeDeclaration) td;
            List<SMTypeDeclaration> types = typeMap.get(typeDeclaration.getName());
            if (types == null) {
                types = new LinkedList<SMTypeDeclaration>();
                typeMap.put(typeDeclaration.getName(), types);
            }
            types.add(typeDeclaration);
            for (FieldDeclaration field : typeDeclaration.getVariables()) {
                Map<String, List<FieldDeclaration>> type = fieldMap.get(typeDeclaration.getName());
                if (type == null) {
                    type = new TreeMap<String, List<FieldDeclaration>>();
                    fieldMap.put(typeDeclaration.getName(), type);
                }
                List<FieldDeclaration> fields = type.get(field.getName());
                if (fields == null) {
                    fields = new LinkedList<FieldDeclaration>();
                    type.put(field.getName(), fields);
                }
                fields.add(field);
            }
        }
        for (SMImportStatement importStatement : moduleDeclaration.getImportStatements()) {
            if (imports.get(importStatement.getClassName()).size() > 1) {
                failureHandler.reportError(new SyntaxError(importStatement.sourceStart(), importStatement.sourceEnd() + 1, SyntaxErrorConstants.EC_DUPLICATED_IMPORT, String.format("Import \"%s\" was duplicated.", importStatement.getName())));
            }
        }
        Set<String> processed = new HashSet<String>();
        for (TypeDeclaration td : moduleDeclaration.getTypes()) {
            addSupertypesFields(typeMap, fieldMap, td.getName(), processed);
            if (imports.containsKey(td.getName())) {
                failureHandler.reportError(new SyntaxError(td.getNameStart(), td.getNameEnd() + 1, SyntaxErrorConstants.EC_IMPORT_TYPE_DECLARED, String.format("The type \"%s\" was imported.", td.getName())));
            }
            if (typeMap.get(td.getName()).size() > 1) {
                failureHandler.reportError(new SyntaxError(td.getNameStart(), td.getNameEnd() + 1, SyntaxErrorConstants.EC_DUPLICATED_TYPE, String.format("The type \"%s\" was duplicated.", td.getName())));
            } else {
                validateTypeDeclaration(failureHandler, typeMap, td);
                Map<String, List<FieldDeclaration>> fieldM = fieldMap.get(td.getName());
                for (FieldDeclaration fieldDeclaration : td.getVariables()) {
                    if (fieldM.get(fieldDeclaration.getName()).size() > 1) {
                        failureHandler.reportError(new SyntaxError(fieldDeclaration.getNameStart(), fieldDeclaration.getNameEnd() + 1, SyntaxErrorConstants.EC_DUPLICATED_FIELD, String.format("The field \"%s\" was duplicated.", fieldDeclaration.getName())));
                    } else {
                        if (Character.isUpperCase(fieldDeclaration.getName().charAt(0))) {
                            failureHandler.reportError(new SyntaxError(fieldDeclaration.getNameStart(), fieldDeclaration.getNameEnd() + 1, SyntaxErrorConstants.EC_UNSUITABLE_NAMED_FIELD, String.format("The field \"%s\" is starting with a capital letter.", fieldDeclaration.getName())));
                        } else if ('_' == fieldDeclaration.getName().charAt(0)) {
                            failureHandler.reportError(new SyntaxError(fieldDeclaration.getNameStart(), fieldDeclaration.getNameEnd() + 1, SyntaxErrorConstants.EC_UNSUITABLE_NAMED_FIELD, String.format("The field \"%s\" is starting with a \"_\" symbol.", fieldDeclaration.getName())));
                        } else if (fieldDeclaration.getName().equals("class")) {
                            failureHandler.reportError(new SyntaxError(fieldDeclaration.getNameStart(), fieldDeclaration.getNameEnd() + 1, SyntaxErrorConstants.EC_UNSUITABLE_NAMED_FIELD, String.format("A field name can not be \"class\".")));
                        } else if (!isValidJavaIdentifier(fieldDeclaration.getName())) {
                            failureHandler.reportError(new SyntaxError(fieldDeclaration.getNameStart(), fieldDeclaration.getNameEnd() + 1, SyntaxErrorConstants.EC_UNSUITABLE_NAMED_FIELD, String.format("The field \"%s\" is not a valid java identifier.", fieldDeclaration.getName())));
                        }
                        if (fieldDeclaration instanceof SMAttributeDeclaration) {
                            SMAttributeDeclaration attributeDeclaration = (SMAttributeDeclaration) fieldDeclaration;
                            validateAttributeDeclaration(typeMap, failureHandler, attributeDeclaration);
                        } else if (fieldDeclaration instanceof SMIndexDeclaration) {
                            SMIndexDeclaration indexDeclaration = (SMIndexDeclaration) fieldDeclaration;
                            validateIndexDeclaration(typeMap, failureHandler, indexDeclaration);
                        }
                    }
                }
                if (fieldM != null) {
                    outter: for (List<FieldDeclaration> fds : fieldM.values()) {
                        if (fds.size() > 1) {
                            Set<FieldDeclaration> fields = new HashSet<FieldDeclaration>();
                            FieldDeclaration sample = null;
                            for (FieldDeclaration fd : fds) {
                                if (((SMFieldDeclaration) fd).getOwner() == td) {
                                    continue outter;
                                }
                                fields.add(fd);
                                sample = fd;
                            }
                            if (fields.size() > 1) {
                                failureHandler.reportError(new SyntaxError(td.getNameStart(), td.getNameEnd() + 1, SyntaxErrorConstants.EC_DUPLICATED_FIELD, String.format("The field \"%s\" was duplicated within extended types.", sample.getName())));
                            }
                        }
                    }
                }
            }
        }
    }

    private InputStream readFully(InputStream in, String charset, StringBuilder sb) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Reader reader = new InputStreamReader(new FilterInputStream(in) {

            @Override
            public int read() throws IOException {
                int read = super.read();
                if (read >= 0) bos.write(read);
                return read;
            }

            @Override
            public int read(byte[] arg0, int arg1, int arg2) throws IOException {
                int read = super.read(arg0, arg1, arg2);
                if (read > 0) bos.write(arg0, arg1, read);
                return read;
            }
        }, charset);
        int read;
        while ((read = reader.read()) >= 0) {
            sb.append((char) read);
        }
        bos.close();
        return new ByteArrayInputStream(bos.toByteArray());
    }

    private void validateAttributeDeclaration(Map<String, List<SMTypeDeclaration>> typeMap, ISimplemodelParseFailureHandler failureHandler, SMAttributeDeclaration attributeDeclaration) {
        SMOppositeStatement opposite = null;
        int oppositeCount = 0;
        if (attributeDeclaration.getBody() != null) {
            for (Object o : attributeDeclaration.getBody().getStatements()) {
                if (o instanceof SMOppositeStatement) {
                    SMOppositeStatement current = (SMOppositeStatement) o;
                    if (opposite == null) {
                        opposite = current;
                    } else {
                        if (oppositeCount == 1) {
                            failureHandler.reportError(new SyntaxError(opposite.sourceStart(), opposite.sourceEnd() + 1, SyntaxErrorConstants.EC_MULTIPLE_OPPOSITE, String.format("The attribute \"%s\" has multiple opposites.", attributeDeclaration.getName())));
                        }
                        failureHandler.reportError(new SyntaxError(current.sourceStart(), current.sourceEnd() + 1, SyntaxErrorConstants.EC_MULTIPLE_OPPOSITE, String.format("The attribute \"%s\" has multiple opposites.", attributeDeclaration.getName())));
                    }
                    oppositeCount++;
                }
            }
        }
        if (oppositeCount == 1) {
            List<SMTypeDeclaration> typeDeclarationCandidates = typeMap.get(attributeDeclaration.getType().getName());
            if (!opposite.getType().getName().equals(attributeDeclaration.getOwner().getName())) {
                failureHandler.reportError(new SyntaxError(opposite.getType().sourceStart(), opposite.getType().sourceEnd() + 1, SyntaxErrorConstants.EC_OPPOSITE_TYPE_NOT_SELF, String.format("The opposite type should be \"%s\" but it is \"%s\".", opposite.getType().getName(), attributeDeclaration.getOwner().getName())));
            }
            if (typeDeclarationCandidates == null || typeDeclarationCandidates.isEmpty()) {
                failureHandler.reportError(new SyntaxError(opposite.getType().sourceStart(), opposite.getType().sourceEnd() + 1, SyntaxErrorConstants.EC_TYPE_NOT_EXIST, String.format("The type \"%s\" does not exist.", opposite.getType().getName())));
            } else {
                SMTypeDeclaration oppositeType = typeDeclarationCandidates.get(0);
                SMAttributeDeclaration oppositeAttributeDeclaration = oppositeType.getDeclaredAttribute(opposite.getName());
                if (oppositeAttributeDeclaration == null) {
                    failureHandler.reportError(new SyntaxError(opposite.sourceStart(), opposite.sourceEnd() + 1, SyntaxErrorConstants.EC_OPPOSITE_ATTRIBUTE_NOT_EXITS, String.format("The opposite attribute \"%s\" does not exist in %s.", opposite.getName(), oppositeType.getName())));
                } else {
                    if (!oppositeAttributeDeclaration.getName().equals(opposite.getName())) {
                        failureHandler.reportError(new SyntaxError(opposite.sourceStart(), opposite.sourceEnd() + 1, SyntaxErrorConstants.EC_OPPOSITE_NAME_NOT_MATCH, String.format("The opposite attribute \"%s\" name does not match.", opposite.getName())));
                    }
                    if (!oppositeAttributeDeclaration.getType().getName().equals(opposite.getType().getName())) {
                        failureHandler.reportError(new SyntaxError(opposite.getType().sourceStart(), opposite.getType().sourceEnd() + 1, SyntaxErrorConstants.EC_OPPOSITE_TYPE_NOT_MATCH, String.format("The opposite attribute \"%s\" type does not match.", opposite.getType().getName())));
                    }
                    if (oppositeAttributeDeclaration.isMultiple() != opposite.isMultiple()) {
                        failureHandler.reportError(new SyntaxError(opposite.sourceStart(), opposite.sourceEnd() + 1, SyntaxErrorConstants.EC_OPPOSITE_MULTIPLICITY_NOT_MATCH, String.format("The opposite attribute \"%s\" multiplicity does not match.", opposite.getName())));
                    }
                    SMOppositeStatement oppositeOfOpposite = oppositeAttributeDeclaration.getOppositeStatement();
                    if (oppositeOfOpposite == null) {
                        failureHandler.reportError(new SyntaxError(opposite.sourceStart(), opposite.sourceEnd() + 1, SyntaxErrorConstants.EC_OPPOSITE_DOES_NOT_HAVE_OPPOSITE, String.format("The opposite attribute \"%s\" of \"%s\" does not have an opposite.", oppositeAttributeDeclaration.getName(), oppositeType.getName())));
                    } else {
                        if (!attributeDeclaration.getName().equals(oppositeOfOpposite.getName())) {
                            failureHandler.reportError(new SyntaxError(opposite.sourceStart(), opposite.sourceEnd() + 1, SyntaxErrorConstants.EC_OPPOSITE_NAME_NOT_MATCH, String.format("The opposite attribute \"%s\" name does not match.", opposite.getName())));
                        }
                        if (!attributeDeclaration.getType().getName().equals(oppositeOfOpposite.getType().getName())) {
                            failureHandler.reportError(new SyntaxError(opposite.getType().sourceStart(), opposite.getType().sourceEnd() + 1, SyntaxErrorConstants.EC_OPPOSITE_TYPE_NOT_MATCH, String.format("The opposite attribute \"%s\" type does not match.", opposite.getType().getName())));
                        }
                        if (attributeDeclaration.isMultiple() != oppositeOfOpposite.isMultiple()) {
                            failureHandler.reportError(new SyntaxError(opposite.sourceStart(), opposite.sourceEnd() + 1, SyntaxErrorConstants.EC_OPPOSITE_MULTIPLICITY_NOT_MATCH, String.format("The opposite attribute \"%s\" multiplicity does not match.", opposite.getName())));
                        }
                    }
                }
            }
        }
    }

    private SMTypeReference validateExpression(Map<String, List<SMTypeDeclaration>> typeMap, SMTypeReference type, Expression valueAccessExpression, ISimplemodelParseFailureHandler failureHandler) {
        if (valueAccessExpression instanceof SMDotExpression) {
            SMDotExpression dot = (SMDotExpression) valueAccessExpression;
            if (type == null) return null;
            type = validateExpression(typeMap, type, dot.getLeft(), failureHandler);
            return validateExpression(typeMap, type, dot.getRight(), failureHandler);
        } else if (valueAccessExpression instanceof SMAttributeReference) {
            SMAttributeReference ref = (SMAttributeReference) valueAccessExpression;
            List<SMTypeDeclaration> candidates = typeMap.get(type.getName());
            if (candidates == null || candidates.isEmpty()) {
                failureHandler.reportError(new SyntaxError(valueAccessExpression.sourceStart(), valueAccessExpression.sourceEnd() + 1, SyntaxErrorConstants.EC_ATTRIBUTE_DOES_NOT_EXIST, String.format("The type \"%s\" does not have an attribute of name \"%s\".", type.getName(), ref.getAttributeName())));
                return null;
            } else if (candidates.size() > 1) {
                return null;
            } else {
                SMTypeDeclaration typeDeclaration = candidates.get(0);
                SMAttributeDeclaration attributeDeclaration = typeDeclaration.findAttributeDeclaration(ref.getAttributeName());
                if (attributeDeclaration == null) {
                    failureHandler.reportError(new SyntaxError(valueAccessExpression.sourceStart(), valueAccessExpression.sourceEnd() + 1, SyntaxErrorConstants.EC_ATTRIBUTE_DOES_NOT_EXIST, String.format("The type \"%s\" does not have an attribute of name \"%s\".", type.getName(), ref.getAttributeName())));
                    return null;
                } else if (attributeDeclaration.isMultiple()) {
                    failureHandler.reportError(new SyntaxError(valueAccessExpression.sourceStart(), valueAccessExpression.sourceEnd() + 1, SyntaxErrorConstants.EC_ATTRIBUTE_IS_MULTIPLE, String.format("The attribute \"%s\" of type \"%s\" is multiple.", ref.getAttributeName(), type.getName())));
                    return null;
                }
                return attributeDeclaration.getType();
            }
        } else {
            throw new IllegalStateException(valueAccessExpression == null ? "null" : valueAccessExpression.getClass().getName());
        }
    }

    private void validateIndexDeclaration(Map<String, List<SMTypeDeclaration>> typeMap, ISimplemodelParseFailureHandler failureHandler, SMIndexDeclaration indexDeclaration) {
        SMAttributeDeclaration indexOn = indexDeclaration.getOwner().findAttributeDeclaration(indexDeclaration.getTargetAttribute().getAttributeName());
        if (indexOn == null) {
            failureHandler.reportError(new SyntaxError(indexDeclaration.sourceStart(), indexDeclaration.sourceEnd() + 1, SyntaxErrorConstants.EC_ATTRIBUTE_DOES_NOT_EXIST, String.format("The index \"%s\" is on an non existing attribute \"%s\".", indexDeclaration.getName(), indexDeclaration.getTargetAttribute().getAttributeName())));
        } else if (!indexOn.isMultiple()) {
            failureHandler.reportError(new SyntaxError(indexDeclaration.sourceStart(), indexDeclaration.sourceEnd() + 1, SyntaxErrorConstants.EC_ATTRIBUTE_IS_NOT_MULTIPLE, String.format("The index \"%s\" is on an non multiple attribute \"%s\".", indexDeclaration.getName(), indexDeclaration.getTargetAttribute().getAttributeName())));
        }
        Map<String, List<SMKeyStatement>> names = new TreeMap<String, List<SMKeyStatement>>();
        Map<String, List<SMKeyStatement>> paths = new TreeMap<String, List<SMKeyStatement>>();
        for (Object o : indexDeclaration.getBody().getStatements()) {
            SMKeyStatement keyStatement = (SMKeyStatement) o;
            List<SMKeyStatement> name = names.get(keyStatement.getName());
            if (name == null) {
                name = new LinkedList<SMKeyStatement>();
                names.put(keyStatement.getName(), name);
            }
            name.add(keyStatement);
            String stringRepresentation = SMParseUtil.createStringRepresentation(keyStatement.getValueAccessExpression());
            List<SMKeyStatement> path = paths.get(stringRepresentation);
            if (path == null) {
                path = new LinkedList<SMKeyStatement>();
                paths.put(stringRepresentation, path);
            }
            path.add(keyStatement);
        }
        for (Object o : indexDeclaration.getBody().getStatements()) {
            SMKeyStatement keyStatement = (SMKeyStatement) o;
            if (names.get(keyStatement.getName()).size() > 1) failureHandler.reportError(new SyntaxError(keyStatement.sourceStart(), keyStatement.sourceEnd() + 1, SyntaxErrorConstants.EC_DUPLICATED_INDEX_KEY_NAME, String.format("The key name \"%s\" was duplicated.", keyStatement.getName())));
            if (paths.get(SMParseUtil.createStringRepresentation(keyStatement.getValueAccessExpression())).size() > 1) failureHandler.reportError(new SyntaxError(keyStatement.sourceStart(), keyStatement.sourceEnd() + 1, SyntaxErrorConstants.EC_DUPLICATED_INDEX_KEY_PATH, String.format("The key name \"%s\" was duplicated.", keyStatement.getName())));
        }
        for (Object o : indexDeclaration.getBody().getStatements()) {
            SMKeyStatement keyStatement = (SMKeyStatement) o;
            validateExpression(typeMap, indexDeclaration.getType(), keyStatement.getValueAccessExpression(), failureHandler);
        }
    }

    private void validateTypeDeclaration(ISimplemodelParseFailureHandler failureHandler, Map<String, List<SMTypeDeclaration>> typeMap, TypeDeclaration td) {
        if (findAllSuperTypes(typeMap, td).contains(td)) {
            failureHandler.reportError(new SyntaxError(td.getNameStart(), td.getNameEnd() + 1, SyntaxErrorConstants.EC_CYCLIC_INHERITANCE, String.format("The type \"%s\" has cyclic inheritance.", td.getName())));
        }
        if (findConstructorPassedTypes(typeMap, (SMTypeDeclaration) td).contains(td)) {
            failureHandler.reportError(new SyntaxError(td.getNameStart(), td.getNameEnd() + 1, SyntaxErrorConstants.EC_CYCLIC_CONSTRUCTOR_VARIABLE, String.format("The type \"%s\" has cyclic constructor variable.", td.getName())));
        }
    }
}
