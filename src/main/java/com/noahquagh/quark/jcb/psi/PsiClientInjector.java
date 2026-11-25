package com.noahquagh.quark.jcb.psi;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;


/**
 * Utilitaire responsable de l'injection directe de dépendances dans une classe Java existante
 * ouverte dans l'éditeur IntelliJ IDEA via l'API PSI.
 *
 * @author noahq
 * @version 1.0
 */
public class PsiClientInjector {

    /**
     * Injecte un attribut privé correspondant au type principal du patron généré dans la classe cible,
     * en gérant automatiquement l'import et le formatage du code.
     *
     * @param project             projet IntelliJ actif
     * @param targetClass         classe Java ouverte dans laquelle injecter l'attribut
     * @param qualifiedTargetType nom qualifié complet du type à injecter (ex : {@code "com.app.strategy.PaymentStrategy"})
     */
    public static void injectClientField(Project project, PsiClass targetClass, String qualifiedTargetType){
        int lastDot = qualifiedTargetType.lastIndexOf('.');
        String simpleName = (lastDot >= 0) ? qualifiedTargetType.substring(lastDot + 1) : qualifiedTargetType;
        String fieldName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);

        if (targetClass.findFieldByName(fieldName,false)!=null){
            return;
        }

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiField field = factory.createFieldFromText("private "+qualifiedTargetType+" "+fieldName+";",targetClass);

        WriteCommandAction.runWriteCommandAction(project, "Injecter le client " + simpleName, null, () -> {
            PsiElement added = targetClass.add(field);
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(added);
        });
    }
}
