package org.hiro.psi;

import com.intellij.codeInsight.generation.PsiElementClassMember;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import static com.intellij.openapi.ui.Messages.getQuestionIcon;
import static com.intellij.openapi.ui.Messages.showYesNoCancelDialog;
import com.intellij.psi.*;
import static com.intellij.psi.JavaPsiFacade.getInstance;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import static com.intellij.psi.search.GlobalSearchScope.allScope;
import org.hiro.CodeGenerationException;
import static org.hiro.psi.PsiClassHelper.getAllFieldsInHierarchyToDisplay;
import java.util.ArrayList;
import java.util.List;

public class GeneratedBuilderClassBuilder extends BasePsiClassBuilder<GeneratedBuilderClassBuilder> {

    private static final String BUILDER = "Builder";

    private final Project project;

    private final CodeStyleManager codeStyleManager;

    private PsiClass classToBuild;

    public GeneratedBuilderClassBuilder(PsiElementFactory elementFactory, Project project, CodeStyleManager codeStyleManager) {
        super(elementFactory, project);
        this.project = project;
        this.codeStyleManager = codeStyleManager;
    }

    @Override
    public PsiClass build() {
        JavaPsiFacade psiFacade = getInstance(project);
        GlobalSearchScope globalSearchScope = allScope(project);
        PsiClass existingBuilder = psiFacade.findClass(qualifiedClassName, globalSearchScope);
        if (existingBuilder != null) {
            String classExistsMessage = "Builder already exists for '" + classToBuild.getName() + "', overwrite the existing builder?";
            int overwriteClass = showYesNoCancelDialog(project, classExistsMessage, "Warning", getQuestionIcon());
            switch(overwriteClass) {
                case 0:
                    existingBuilder.delete();
                    break;
                case 1:
                    return existingBuilder;
                default:
                    return null;
            }
        }
        return buildNewClass(psiFacade, globalSearchScope);
    }

    private PsiClass buildNewClass(JavaPsiFacade psiFacade, GlobalSearchScope globalSearchScope) {
        withImports(new PsiImportListBuilder().withImportsForFile((PsiJavaFile) classToBuild.getContainingFile()).withImport(getElementFactory().createImportStatement(psiFacade.findClass("java.util.Map", globalSearchScope))).withImport(getElementFactory().createImportStatement(psiFacade.findClass("java.util.HashMap", globalSearchScope))).withImport(getElementFactory().createImportStatement(psiFacade.findClass("java.lang.reflect.Field", globalSearchScope))).build());
        withField("private Map<String,Object> values = new HashMap<String,Object>();");
        withMethod(createNewEmptyObjectMethod());
        withMethod(createBuildMethod());
        withMethods(createWithMethods(getAllFieldsInHierarchyToDisplay(classToBuild)));
        withMethods(createGetMethods(getAllFieldsInHierarchyToDisplay(classToBuild)));
        return super.build();
    }

    private List<PsiMethod> createGetMethods(PsiElementClassMember[] fieldsToDisplay) {
        List<PsiMethod> methods = new ArrayList<PsiMethod>();
        for (PsiElementClassMember field : fieldsToDisplay) {
            PsiField psiField = (PsiField) field.getElement();
            methods.add(new GetMethodBuilder(getElementFactory(), codeStyleManager).withClassToBuild(classToBuild).withFieldName(psiField.getName()).build());
        }
        return methods;
    }

    private PsiMethod createNewEmptyObjectMethod() {
        return new NewEmptyObjectMethodBuilder(getElementFactory(), codeStyleManager).withClassToBuild(classToBuild).build();
    }

    private PsiMethod createBuildMethod() {
        return new GuffBuildMethodBuilder(getElementFactory(), codeStyleManager).withClassToBuild(classToBuild).build();
    }

    public GeneratedBuilderClassBuilder withClassToBuild(PsiClass classToBuild) {
        this.classToBuild = classToBuild;
        withDirectory(classToBuild.getContainingFile().getContainingDirectory());
        withName(classToBuild.getName() + BUILDER);
        withQualifiedName(classToBuild.getQualifiedName() + BUILDER);
        return this;
    }

    private List<PsiMethod> createWithMethods(PsiElementClassMember[] fieldsToDisplay) {
        List<PsiMethod> methods = new ArrayList<PsiMethod>();
        try {
            for (PsiElementClassMember field : fieldsToDisplay) {
                methods.add(createMethod(GeneratedBuilderHelper.getWithMethodText(field, className, GeneratedBuilderHelper.WITH_METHOD_VALUEMAP_TEMPLATE)));
            }
        } catch (CodeGenerationException e) {
            Messages.showMessageDialog(project, "Velocity error generating code - see IDEA log for more details:\n" + e.getMessage(), "Warning", Messages.getWarningIcon());
        }
        return methods;
    }

    private PsiMethod createMethod(String methodText) {
        return new PsiMethodBuilder(getElementFactory(), codeStyleManager).withMethodText(methodText).build();
    }
}
