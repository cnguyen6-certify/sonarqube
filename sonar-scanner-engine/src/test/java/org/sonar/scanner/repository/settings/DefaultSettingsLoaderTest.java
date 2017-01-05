package org.sonar.scanner.repository.settings;

import org.junit.Test;
import org.sonarqube.ws.Settings.FieldValues;
import org.sonarqube.ws.Settings.FieldValues.Value;
import org.sonarqube.ws.Settings.FieldValues.Value.Builder;
import org.sonarqube.ws.Settings.Setting;
import org.sonarqube.ws.Settings.Values;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class DefaultSettingsLoaderTest {

  @Test
  public void should_load_global_multivalue_settings() {
    assertThat(DefaultSettingsLoader.toMap(asList(Setting.newBuilder()
      .setKey("sonar.preview.supportedPlugins")
      .setValues(Values.newBuilder().addValues("java").addValues("php")).build())))
        .containsExactly(entry("sonar.preview.supportedPlugins", "java,php"));
  }

  @Test
  public void should_load_global_propertyset_settings() {
    Builder valuesBuilder = Value.newBuilder();
    valuesBuilder.getMutableValue().put("filepattern", "**/*.xml");
    valuesBuilder.getMutableValue().put("rulepattern", "*:S12345");
    Value value1 = valuesBuilder.build();
    valuesBuilder.clear();
    valuesBuilder.getMutableValue().put("filepattern", "**/*.java");
    valuesBuilder.getMutableValue().put("rulepattern", "*:S456");
    Value value2 = valuesBuilder.build();

    assertThat(DefaultSettingsLoader.toMap(asList(Setting.newBuilder()
      .setKey("sonar.issue.exclusions.multicriteria")
      .setFieldValues(FieldValues.newBuilder().addFieldValues(value1).addFieldValues(value2)).build())))
        .containsOnly(entry("sonar.issue.exclusions.multicriteria", "1,2"),
          entry("sonar.issue.exclusions.multicriteria.1.filepattern", "**/*.xml"),
          entry("sonar.issue.exclusions.multicriteria.1.rulepattern", "*:S12345"),
          entry("sonar.issue.exclusions.multicriteria.2.filepattern", "**/*.java"),
          entry("sonar.issue.exclusions.multicriteria.2.rulepattern", "*:S456"));
  }
}
