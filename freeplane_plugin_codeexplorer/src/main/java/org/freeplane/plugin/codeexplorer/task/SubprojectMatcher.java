/*
 * Created on 8 Feb 2024
 *
 * author dimitry
 */
package org.freeplane.plugin.codeexplorer.task;

import java.util.Optional;

import com.tngtech.archunit.core.domain.JavaClass;

public interface SubprojectMatcher {
    Optional<SubprojectIdentifier> subprojectIdentifier(JavaClass javaClass);
    default boolean belongsToSubproject(JavaClass javaClass) {
        return subprojectIdentifier(javaClass).isPresent();
    }
}
