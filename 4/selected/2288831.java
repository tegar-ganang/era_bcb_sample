package com.ilog.translator.java2cs.w4;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.text.edits.TextEditGroup;
import com.ilog.translator.java2cs.translation.ITranslationContext;
import com.ilog.translator.java2cs.translation.astrewriter.ASTRewriterVisitor;
import com.sun.org.apache.bcel.internal.generic.Type;

public class ChangeComplexEnumToClass extends ASTRewriterVisitor {

    private static String _enumConstantName = "__j2cs__Name";

    private static String _enumConstantOrdinal = "__j2cs__Ordinal";

    private static String _enumConstants = "__j2cs__Constants";

    private static String _enumField = "__j2cs__ComplexEnum";

    private static ArrayList<String> _forbiddenName = new ArrayList<String>();

    static {
        _forbiddenName.add("Type");
    }

    public ChangeComplexEnumToClass(ITranslationContext context) {
        super(context);
        transformerName = "Change complex java enum to C# class";
        description = new TextEditGroup(transformerName);
    }

    private void addConstants(TypeDeclaration a_type, List<EnumConstantDeclaration> a_constants) {
        AST l_ast = a_type.getAST();
        int l_cpt = 0;
        for (EnumConstantDeclaration l_constant : a_constants) {
            VariableDeclarationFragment l_frag = l_ast.newVariableDeclarationFragment();
            ClassInstanceCreation l_classInstCrea = l_ast.newClassInstanceCreation();
            l_classInstCrea.setType(l_ast.newSimpleType(l_ast.newSimpleName(a_type.getName().toString())));
            List<Expression> l_constArgs = l_constant.arguments();
            for (Expression l_t : l_constArgs) {
                Expression l_expr = (Expression) ASTNode.copySubtree(l_ast, l_t);
                if (l_expr.toString().endsWith(".class")) {
                    ASTNode l_typeof = currentRewriter.createStringPlaceholder("/* typeof(" + l_expr.toString().replace(".class", "") + ") */" + l_expr.toString(), ASTNode.SIMPLE_NAME);
                    l_classInstCrea.arguments().add(l_typeof);
                } else {
                    l_classInstCrea.arguments().add(l_expr);
                }
            }
            StringLiteral l_constantName = l_ast.newStringLiteral();
            l_constantName.setLiteralValue(l_constant.getName().toString());
            l_classInstCrea.arguments().add(l_constantName);
            l_classInstCrea.arguments().add(l_ast.newNumberLiteral(String.valueOf(l_cpt)));
            l_frag.setName(l_ast.newSimpleName(l_constant.getName().toString()));
            l_frag.setInitializer(l_classInstCrea);
            FieldDeclaration l_field = l_ast.newFieldDeclaration(l_frag);
            SimpleName l_fieldName = l_ast.newSimpleName(a_type.getName().toString());
            l_field.setType(l_ast.newSimpleType(l_fieldName));
            l_field.modifiers().add(l_ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
            l_field.modifiers().add(l_ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
            a_type.bodyDeclarations().add(l_field);
            currentRewriter.replace(l_field.getType(), currentRewriter.createStringPlaceholder("readonly " + l_field.getType().toString(), l_field.getType().getNodeType()), description);
            l_cpt++;
        }
    }

    private TypeDeclaration createTypeDeclaration(EnumDeclaration a_enum) {
        AST l_ast = a_enum.getAST();
        TypeDeclaration l_type = l_ast.newTypeDeclaration();
        String l_name = a_enum.getName().toString();
        if (_forbiddenName.contains(l_name)) {
            l_type.setName(l_ast.newSimpleName(l_name + "0"));
        } else {
            l_type.setName(l_ast.newSimpleName(l_name));
        }
        List<Modifier> l_modifiers = a_enum.modifiers();
        for (Modifier l_mod : l_modifiers) {
            l_type.modifiers().add(l_ast.newModifier(l_mod.getKeyword()));
        }
        return l_type;
    }

    private MethodDeclaration buildValueOfMethod(AST a_ast, String a_typeName, List<EnumConstantDeclaration> a_constants) {
        MethodDeclaration l_method = a_ast.newMethodDeclaration();
        l_method.setName(a_ast.newSimpleName("ValueOf"));
        l_method.setReturnType2(a_ast.newSimpleType(a_ast.newSimpleName(a_typeName)));
        l_method.modifiers().add(a_ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
        l_method.modifiers().add(a_ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
        SingleVariableDeclaration l_param = a_ast.newSingleVariableDeclaration();
        l_param.setName(a_ast.newSimpleName("type"));
        l_param.setType(a_ast.newSimpleType(a_ast.newSimpleName("String")));
        l_method.parameters().add(l_param);
        Block l_body = a_ast.newBlock();
        SwitchStatement l_switch = a_ast.newSwitchStatement();
        l_switch.setExpression(a_ast.newSimpleName("type"));
        for (int i = 0; i < a_constants.size(); i++) {
            String l_fieldName = a_constants.get(i).getName().getIdentifier();
            SwitchCase l_case = a_ast.newSwitchCase();
            StringLiteral l_str = a_ast.newStringLiteral();
            l_str.setEscapedValue("\"" + l_fieldName + "\"");
            l_case.setExpression(l_str);
            ReturnStatement l_return = a_ast.newReturnStatement();
            l_return.setExpression(a_ast.newSimpleName(l_fieldName));
            l_switch.statements().add(l_case);
            l_switch.statements().add(l_return);
        }
        l_body.statements().add(l_switch);
        ReturnStatement l_return = a_ast.newReturnStatement();
        l_return.setExpression(a_ast.newNullLiteral());
        l_body.statements().add(l_return);
        l_method.setBody(l_body);
        return l_method;
    }

    private void ChangeToStringMethod(MethodDeclaration a_meth) {
        AST l_ast = a_meth.getAST();
        Block l_body = l_ast.newBlock();
        ReturnStatement l_return = l_ast.newReturnStatement();
        l_return.setExpression(l_ast.newSimpleName(this._enumConstantName));
        l_body.statements().add(l_return);
        String l_returnType = "override " + a_meth.getReturnType2().toString();
        currentRewriter.replace(a_meth.getReturnType2(), currentRewriter.createStringPlaceholder(l_returnType, a_meth.getReturnType2().getNodeType()), description);
        a_meth.setBody(l_body);
    }

    private MethodDeclaration buildCompareToMethod(AST a_ast, String a_typeName) {
        MethodDeclaration l_method = a_ast.newMethodDeclaration();
        l_method.setName(a_ast.newSimpleName("CompareTo"));
        l_method.setReturnType2(a_ast.newPrimitiveType(PrimitiveType.INT));
        l_method.modifiers().add(a_ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
        SingleVariableDeclaration l_param = a_ast.newSingleVariableDeclaration();
        l_param.setName(a_ast.newSimpleName("l_enum"));
        l_param.setType(a_ast.newSimpleType(a_ast.newSimpleName(a_typeName)));
        l_method.parameters().add(l_param);
        Block l_body = a_ast.newBlock();
        MethodInvocation l_ordinalInvocation = a_ast.newMethodInvocation();
        l_ordinalInvocation.setName(a_ast.newSimpleName("Ordinal"));
        InfixExpression l_substration = a_ast.newInfixExpression();
        l_substration.setLeftOperand(l_ordinalInvocation);
        l_substration.setOperator(InfixExpression.Operator.MINUS);
        l_ordinalInvocation = a_ast.newMethodInvocation();
        l_ordinalInvocation.setName(a_ast.newSimpleName("Ordinal"));
        l_ordinalInvocation.setExpression(a_ast.newSimpleName("l_enum"));
        l_substration.setRightOperand(l_ordinalInvocation);
        ReturnStatement l_return = a_ast.newReturnStatement();
        l_return.setExpression(l_substration);
        l_body.statements().add(l_return);
        l_method.setBody(l_body);
        return l_method;
    }

    private MethodDeclaration buildNameMethod(AST a_ast) {
        MethodDeclaration l_method = a_ast.newMethodDeclaration();
        l_method.setName(a_ast.newSimpleName("Name"));
        l_method.setReturnType2(a_ast.newSimpleType(a_ast.newName("String")));
        l_method.modifiers().add(a_ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
        Block l_body = a_ast.newBlock();
        ReturnStatement l_return = a_ast.newReturnStatement();
        l_return.setExpression(a_ast.newSimpleName(this._enumConstantName));
        l_body.statements().add(l_return);
        l_method.setBody(l_body);
        return l_method;
    }

    private MethodDeclaration buildOrdinalMethod(AST a_ast) {
        MethodDeclaration l_method = a_ast.newMethodDeclaration();
        l_method.setName(a_ast.newSimpleName("Ordinal"));
        l_method.setReturnType2(a_ast.newPrimitiveType(PrimitiveType.INT));
        l_method.modifiers().add(a_ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
        Block l_body = a_ast.newBlock();
        ReturnStatement l_return = a_ast.newReturnStatement();
        l_return.setExpression(a_ast.newSimpleName(this._enumConstantOrdinal));
        l_body.statements().add(l_return);
        l_method.setBody(l_body);
        return l_method;
    }

    private void completeConstrutor(MethodDeclaration a_meth) {
        AST l_ast = a_meth.getAST();
        SingleVariableDeclaration l_name = l_ast.newSingleVariableDeclaration();
        l_name.setName(l_ast.newSimpleName("j2cs_name"));
        l_name.setType(l_ast.newSimpleType(l_ast.newName("String")));
        a_meth.parameters().add(l_name);
        SingleVariableDeclaration l_ordinal = l_ast.newSingleVariableDeclaration();
        l_ordinal.setName(l_ast.newSimpleName("j2cs_ordinal"));
        l_ordinal.setType(l_ast.newPrimitiveType(PrimitiveType.INT));
        a_meth.parameters().add(l_ordinal);
        Assignment l_ass = l_ast.newAssignment();
        l_ass.setLeftHandSide(l_ast.newSimpleName(this._enumConstantName));
        l_ass.setRightHandSide(l_ast.newSimpleName("j2cs_name"));
        a_meth.getBody().statements().add(l_ast.newExpressionStatement(l_ass));
        l_ass = l_ast.newAssignment();
        l_ass.setLeftHandSide(l_ast.newSimpleName(this._enumConstantOrdinal));
        l_ass.setRightHandSide(l_ast.newSimpleName("j2cs_ordinal"));
        a_meth.getBody().statements().add(l_ast.newExpressionStatement(l_ass));
        a_meth.modifiers().clear();
        a_meth.modifiers().add(l_ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
    }

    private MethodDeclaration buildValuesMethod(AST a_ast, String a_typeName) {
        MethodDeclaration l_method = a_ast.newMethodDeclaration();
        l_method.setName(a_ast.newSimpleName("Values"));
        l_method.setReturnType2(a_ast.newArrayType(a_ast.newSimpleType(a_ast.newName(a_typeName))));
        l_method.modifiers().add(a_ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
        l_method.modifiers().add(a_ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
        Block l_body = a_ast.newBlock();
        ReturnStatement l_return = a_ast.newReturnStatement();
        l_return.setExpression(a_ast.newSimpleName(this._enumConstants));
        l_body.statements().add(l_return);
        l_method.setBody(l_body);
        return l_method;
    }

    private FieldDeclaration builEnumField(AST a_ast) {
        VariableDeclarationFragment l_frag = a_ast.newVariableDeclarationFragment();
        l_frag.setName(a_ast.newSimpleName(this._enumField));
        FieldDeclaration l_field = a_ast.newFieldDeclaration(l_frag);
        l_field.setType(a_ast.newPrimitiveType(PrimitiveType.INT));
        l_field.modifiers().add(a_ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
        l_field.modifiers().add(a_ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
        currentRewriter.replace(l_field.getType(), currentRewriter.createStringPlaceholder("readonly " + l_field.getType().toString(), l_field.getType().getNodeType()), description);
        return l_field;
    }

    private FieldDeclaration buildConstantsTable(AST a_ast, String a_typeName, List<EnumConstantDeclaration> a_constants) {
        VariableDeclarationFragment l_fragment = a_ast.newVariableDeclarationFragment();
        l_fragment.setName(a_ast.newSimpleName(this._enumConstants));
        ArrayInitializer l_init = a_ast.newArrayInitializer();
        for (int i = 0; i < a_constants.size(); i++) {
            String l_fieldName = a_constants.get(i).getName().getIdentifier();
            l_init.expressions().add(a_ast.newSimpleName(l_fieldName));
        }
        l_fragment.setInitializer(l_init);
        FieldDeclaration l_field = a_ast.newFieldDeclaration(l_fragment);
        l_field.setType(a_ast.newArrayType(Helper.getTypeFromString(a_ast, a_typeName)));
        l_field.modifiers().add(a_ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
        l_field.modifiers().add(a_ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
        return l_field;
    }

    private void addFieldsAndMethods(TypeDeclaration a_type, List<BodyDeclaration> a_enumBody, List<EnumConstantDeclaration> a_constants) {
        AST l_ast = a_type.getAST();
        boolean l_toStringExists = false;
        for (BodyDeclaration l_bd : a_enumBody) {
            BodyDeclaration l_bdCopy = (BodyDeclaration) ASTNode.copySubtree(l_bd.getAST(), l_bd);
            if (l_bdCopy instanceof MethodDeclaration) {
                if (((MethodDeclaration) l_bdCopy).isConstructor()) {
                    completeConstrutor((MethodDeclaration) l_bdCopy);
                } else if (l_bdCopy.modifiers().size() == 0) {
                    l_bdCopy.modifiers().add(l_ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
                } else if (((MethodDeclaration) l_bdCopy).getName().getIdentifier().equals("ToString")) {
                    ChangeToStringMethod((MethodDeclaration) l_bdCopy);
                    l_toStringExists = true;
                }
            }
            a_type.bodyDeclarations().add(l_bdCopy);
        }
        a_type.bodyDeclarations().add(builEnumField(l_ast));
        a_type.bodyDeclarations().add(buildConstantsTable(l_ast, a_type.getName().getIdentifier(), a_constants));
        a_type.bodyDeclarations().add(buildValuesMethod(l_ast, a_type.getName().getIdentifier()));
        a_type.bodyDeclarations().add(buildValueOfMethod(l_ast, a_type.getName().getIdentifier(), a_constants));
        VariableDeclarationFragment l_frag = l_ast.newVariableDeclarationFragment();
        l_frag.setName(l_ast.newSimpleName(this._enumConstantOrdinal));
        FieldDeclaration l_ordinalField = l_ast.newFieldDeclaration(l_frag);
        l_ordinalField.setType(l_ast.newPrimitiveType(PrimitiveType.INT));
        a_type.bodyDeclarations().add(l_ordinalField);
        a_type.bodyDeclarations().add(buildOrdinalMethod(l_ast));
        l_frag = l_ast.newVariableDeclarationFragment();
        l_frag.setName(l_ast.newSimpleName(this._enumConstantName));
        FieldDeclaration l_nameField = l_ast.newFieldDeclaration(l_frag);
        l_nameField.setType(l_ast.newSimpleType(l_ast.newSimpleName("String")));
        a_type.bodyDeclarations().add(l_nameField);
        a_type.bodyDeclarations().add(buildNameMethod(l_ast));
        a_type.bodyDeclarations().add(buildCompareToMethod(l_ast, a_type.getName().getIdentifier()));
        if (!l_toStringExists) {
        }
    }

    public boolean visit(EnumDeclaration a_enum) {
        if (a_enum.bodyDeclarations().size() > 0) {
            TypeDeclaration l_type = createTypeDeclaration(a_enum);
            addConstants(l_type, a_enum.enumConstants());
            addFieldsAndMethods(l_type, a_enum.bodyDeclarations(), a_enum.enumConstants());
            ListRewrite l_listRewrite = currentRewriter.getListRewrite(a_enum.getParent(), TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
            l_listRewrite.insertBefore(l_type, a_enum, description);
            l_listRewrite.remove(a_enum, description);
        }
        return true;
    }
}
