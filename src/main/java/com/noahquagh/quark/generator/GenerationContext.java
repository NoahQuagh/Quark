package com.noahquagh.quark.generator;

import com.noahquagh.quark.model.ClassKind;
import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.PatternRole;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.codeStyle.CodeStyleManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author noahq
 * @version 1.2
 */
public record GenerationContext(
        DesignPatternType pattern,
        String baseName,
        String packageName,
        Map<String, List<String>> roleClasses,
        boolean pedagogical
) {
    public String primaryClassOf(String roleId){
        List<String> names = roleClasses.get(roleId);
        return (names != null && !names.isEmpty()) ? names.get(0) : null;
    }

    public List<String> allClassesOf(String roleId) {
        return roleClasses.getOrDefault(roleId, List.of());
    }
}
