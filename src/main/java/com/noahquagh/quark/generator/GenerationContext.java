package com.noahquagh.quark.generator;

import org.jetbrains.annotations.Nullable;
import com.noahquagh.quark.model.DesignPatternType;
import com.noahquagh.quark.model.ExistingClassInfo;
import com.noahquagh.quark.model.PatternRole;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


/**
 * Contexte immuable regroupant l'ensemble des paramètres et résolutions de noms d'une session de génération.
 *
 * @param pattern {@link DesignPatternType}
 * @param baseName {@link String}
 * @param packageName {@link String}
 * @param roleClasses {@code Map<String, List<String>>}
 * @param enabledOptionalRoleIds {@code Set<String>}
 * @param roleInstanceCounts {@code Map<String,Integer>}
 * @param variants {@code List<String>}
 * @param pedagogical {@code boolean}
 * @param existingClassInfo {@link ExistingClassInfo}
 *
 * @author noahq
 * @version 1.2
 */
public record GenerationContext(
        DesignPatternType pattern,
        String baseName,
        String packageName,
        Map<String, List<String>> roleClasses,
        Set<String> enabledOptionalRoleIds,
        Map<String, Integer> roleInstanceCounts,
        List<String> variants,
        boolean pedagogical,
        @Nullable ExistingClassInfo existingClassInfo
) {
    
    public GenerationContext(DesignPatternType pattern, String baseName, String packageName,
                             Map<String, List<String>> roleClasses, boolean pedagogical) {
        this(pattern, baseName, packageName, roleClasses, Set.of(), Map.of(), List.of(), pedagogical, null);
    }

    public String primaryClassOf(String roleId) {
        List<String> names = roleClasses.get(roleId);
        return (names != null && !names.isEmpty()) ? names.get(0) : null;
    }

    public List<String> allClassesOf(String roleId) {
        return roleClasses.getOrDefault(roleId, List.of());
    }

    public Optional<PatternRole> existingTargetRole() {
        return existingClassInfo != null ? pattern.findExistingTargetRole() : Optional.empty();
    }
}
