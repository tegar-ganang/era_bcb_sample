package com.spartez.poisoninspection;

import com.intellij.psi.*;
import java.util.*;
import org.jetbrains.annotations.NotNull;

class IsPoisonousResolver {

    private final Set<PsiField> writePoisonous = new HashSet<PsiField>();

    private final Set<PsiField> writeSafe = new HashSet<PsiField>();

    private final Set<PsiField> readPoisonous = new HashSet<PsiField>();

    private final Set<PsiField> readSafe = new HashSet<PsiField>();

    public boolean isPoisonous(PsiField field, boolean isWrite) {
        final Set<PsiField> poisonousCache = isWrite ? writePoisonous : readPoisonous;
        final Set<PsiField> safeCache = isWrite ? writeSafe : readSafe;
        if (poisonousCache.contains(field)) {
            return true;
        }
        if (safeCache.contains(field)) {
            return false;
        }
        boolean isPoisonous = resolve(field, isWrite);
        if (isPoisonous) {
            poisonousCache.add(field);
        } else {
            safeCache.add(field);
        }
        return isPoisonous;
    }

    private static boolean resolve(PsiField field, boolean isWrite) {
        final PsiClass clazz = field.getContainingClass();
        return isWrite ? hasSetter(clazz, field) : hasGetter(clazz, field);
    }

    static String getFieldBeanName(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static boolean hasSetter(PsiClass clazz, PsiField field) {
        String fname = "set" + getFieldBeanName(field.getName());
        PsiMethod[] methods = clazz.findMethodsByName(fname, false);
        for (PsiMethod method : methods) {
            if (isSimpleSetter(method, field)) {
                return false;
            }
        }
        return methods.length > 0;
    }

    private static boolean hasGetter(PsiClass clazz, PsiField field) {
        final String fieldBeanName = getFieldBeanName(field.getName());
        List<PsiMethod> methods = new ArrayList<PsiMethod>(Arrays.asList(clazz.findMethodsByName("get" + fieldBeanName, false)));
        if (field.getType() == PsiType.BOOLEAN || field.getType().equalsToText("java.lang.Boolean")) {
            methods.addAll(Arrays.asList(clazz.findMethodsByName("is" + fieldBeanName, false)));
        }
        filterOutInvalidGetters(methods, field.getType());
        for (PsiMethod method : methods) {
            if (isSimpleGetter(method, field)) {
                return false;
            }
        }
        return methods.size() > 0;
    }

    private static void filterOutInvalidGetters(List<PsiMethod> methods, @NotNull PsiType fieldType) {
        Iterator<PsiMethod> i = methods.iterator();
        while (i.hasNext()) {
            PsiMethod psiMethod = i.next();
            PsiType returnType = psiMethod.getReturnType();
            if (!fieldType.equals(returnType)) {
                i.remove();
            }
        }
    }

    private static boolean isSimpleSetter(PsiMethod method, PsiField field) {
        PsiParameterList paramList = method.getParameterList();
        if (paramList.getParametersCount() != 1) {
            return false;
        }
        PsiParameter param = paramList.getParameters()[0];
        if (!field.getType().isAssignableFrom(param.getType())) {
            return false;
        }
        final PsiCodeBlock body = method.getBody();
        if (body == null) {
            return false;
        }
        PsiStatement[] statements = body.getStatements();
        if (statements.length != 1) {
            return false;
        }
        if (statements[0] instanceof PsiExpressionStatement) {
            PsiExpression expression = ((PsiExpressionStatement) statements[0]).getExpression();
            if (expression instanceof PsiAssignmentExpression) {
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
                PsiReference lReference = assignment.getLExpression().getReference();
                if (lReference == null || !lReference.isReferenceTo(field)) {
                    return false;
                }
                PsiExpression rExpression = assignment.getRExpression();
                return rExpression instanceof PsiReference && ((PsiReference) rExpression).isReferenceTo(param);
            }
        }
        return false;
    }

    private static boolean isSimpleGetter(PsiMethod method, PsiField field) {
        PsiParameterList paramList = method.getParameterList();
        if (paramList.getParametersCount() != 0) {
            return false;
        }
        final PsiCodeBlock body = method.getBody();
        if (body == null) {
            return false;
        }
        PsiStatement[] statements = body.getStatements();
        if (statements.length != 1) {
            return false;
        }
        if (statements[0] instanceof PsiReturnStatement) {
            PsiExpression expression = ((PsiReturnStatement) statements[0]).getReturnValue();
            return expression instanceof PsiReference && ((PsiReference) expression).isReferenceTo(field);
        }
        return false;
    }
}
