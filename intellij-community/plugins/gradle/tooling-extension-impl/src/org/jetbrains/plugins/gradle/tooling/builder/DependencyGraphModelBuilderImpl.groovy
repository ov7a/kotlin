// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.builder

import com.intellij.openapi.externalSystem.model.project.dependencies.ComponentDependenciesImpl
import com.intellij.openapi.externalSystem.model.project.dependencies.DependencyScopeNode
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependencies
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependenciesImpl
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import org.jetbrains.plugins.gradle.tooling.tasks.DependenciesReport
import org.jetbrains.plugins.gradle.tooling.util.JavaPluginUtil

@CompileStatic
class DependencyGraphModelBuilderImpl implements ModelBuilderService {
  def compileClasspathConfigurationAvailable = GradleVersion.current().baseVersion >= GradleVersion.version("2.12")
  def runtimeClasspathConfigurationAvailable = GradleVersion.current().baseVersion >= GradleVersion.version("3.4")

  @Override
  boolean canBuild(String modelName) {
    return ProjectDependencies.name == modelName
  }

  @Override
  Object buildAll(String modelName, Project project) {
    def resolveSourceSetDependencies = System.properties.'idea.resolveSourceSetDependencies' as boolean
    if (!resolveSourceSetDependencies) return null

    def sourceSetContainer = JavaPluginUtil.getSourceSetContainer(project)
    if (sourceSetContainer == null) return null

    ProjectDependenciesImpl dependencies = new ProjectDependenciesImpl()
    for (sourceSet in sourceSetContainer) {
      def compileConfigurationName =
        compileClasspathConfigurationAvailable ? sourceSet.compileClasspathConfigurationName : sourceSet.compileConfigurationName
      def compileConfiguration = project.configurations.findByName(compileConfigurationName)
      if (compileConfiguration == null) continue

      def runtimeConfigurationName =
        runtimeClasspathConfigurationAvailable ? sourceSet.runtimeClasspathConfigurationName : sourceSet.runtimeConfigurationName
      def runtimeConfiguration = project.configurations.findByName(runtimeConfigurationName)
      if (runtimeConfiguration == null) continue

      DependencyScopeNode compileScopeNode = DependenciesReport.buildDependenciesGraph(compileConfiguration, project)
      DependencyScopeNode runtimeScopeNode = DependenciesReport.buildDependenciesGraph(runtimeConfiguration, project)

      if (!compileScopeNode.dependencies.isEmpty() || !runtimeScopeNode.dependencies.isEmpty()) {
        dependencies.add(new ComponentDependenciesImpl(sourceSet.name, compileScopeNode, runtimeScopeNode))
      }
    }

    return dependencies.componentsDependencies.isEmpty() ? null : dependencies
  }


  @NotNull
  @Override
  ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder.create(project, e, "Dependency graph model errors")
  }
}
