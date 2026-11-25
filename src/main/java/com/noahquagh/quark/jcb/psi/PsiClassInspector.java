package com.noahquagh.quark.jcb.psi;

import com.intellij.psi.*;
import com.noahquagh.quark.model.ExistingClassInfo;
import com.noahquagh.quark.model.ExistingClassInfo.MethodSignature;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Inspecteur PSI extrayant les métadonnées et méthodes publiques d'une {@link PsiClass}
 * pour alimenter le générateur de patrons de conception.
 *
 * @author noahq
 * @version 1.0
 */
public class PsiClassInspector {

    private static final Set<String> OBJECT_METHODS = Set.of(
            "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait", "clone", "finalize"
    );

    /**
     * Analyse une classe ou interface PSI et retourne son descripteur {@link ExistingClassInfo}.
     *
     * @param psiClass classe cible sélectionnée dans le projet (peut être {@code null})
     * @return descripteur extrait, ou {@code null} si {@code psiClass} est invalide
     */
    public static @Nullable ExistingClassInfo inspect(@Nullable PsiClass psiClass) {
        if (psiClass == null || psiClass.getName() == null) {
            return null;
        }

        String qualifiedName = psiClass.getQualifiedName() != null
                ? psiClass.getQualifiedName()
                : psiClass.getName();
        String simpleName = psiClass.getName();
        String packageName = "";
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = qualifiedName.substring(0, lastDot);
        }

        List<MethodSignature> methods = new ArrayList<>();
        for (PsiMethod method : psiClass.getAllMethods()) {
            if (method.isConstructor()) continue;
            if (!method.hasModifierProperty(PsiModifier.PUBLIC)) continue;
            if (method.hasModifierProperty(PsiModifier.STATIC)) continue;
            if (OBJECT_METHODS.contains(method.getName())) continue;

            PsiType returnPsiType = method.getReturnType();
            String returnType = (returnPsiType != null) ? returnPsiType.getPresentableText() : "void";

            List<String> params = new ArrayList<>();
            for (PsiParameter param : method.getParameterList().getParameters()) {
                params.add(param.getType().getPresentableText() + " " + param.getName());
            }

            methods.add(new MethodSignature(returnType, method.getName(), params));
        }

        return new ExistingClassInfo(qualifiedName, simpleName, packageName, psiClass.isInterface(), methods);
    }
}