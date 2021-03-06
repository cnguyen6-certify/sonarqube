/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package it.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import it.Category3Suite;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the extension point org.sonar.api.batch.bootstrap.ProjectBuilder
 * <p/>
 * A Sonar plugin can override the project definition injected by build-tool.
 * Example: C# plugin loads project structure and modules from Visual Studio metadata file.
 *
 * @since 2.9
 */
public class ProjectBuilderTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Test
  public void shouldDefineProjectFromPlugin() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("analysis/project-builder"))
      .setCleanSonarGoals()
      .setProperty("sonar.enableProjectBuilder", "true")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    checkProject();
    checkSubProject("project-builder-module-a");
    checkSubProject("project-builder-module-b");
    checkFile("project-builder-module-a", "src/HelloA.java");
    checkFile("project-builder-module-b", "src/HelloB.java");
    assertThat(getResource("com.sonarsource.it.projects.batch:project-builder-module-b:src/IgnoredFile.java")).isNull();
  }

  private void checkProject() {
    Resource project = getResource("com.sonarsource.it.projects.batch:project-builder");

    // name has been changed by plugin
    assertThat(project.getName()).isEqualTo("Name changed by plugin");

    assertThat(project).isNotNull();
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("lines")).isGreaterThan(10);
  }

  private void checkSubProject(String subProjectKey) {
    Resource subProject = getResource("com.sonarsource.it.projects.batch:" + subProjectKey);
    assertThat(subProject).isNotNull();
    assertThat(subProject.getMeasureIntValue("files")).isEqualTo(1);
    assertThat(subProject.getMeasureIntValue("lines")).isGreaterThan(5);
  }

  private void checkFile(String subProjectKey, String fileKey) {
    Resource file = getResource("com.sonarsource.it.projects.batch:" + subProjectKey + ":" + fileKey);
    assertThat(file).isNotNull();
    assertThat(file.getMeasureIntValue("lines")).isGreaterThan(5);
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "lines", "files"));
  }
}
