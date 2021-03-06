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
package it.settings;

import com.sonar.orchestrator.Orchestrator;
import it.Category1Suite;
import java.io.IOException;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.client.setting.ResetRequest;
import org.sonarqube.ws.client.setting.SetRequest;
import org.sonarqube.ws.client.setting.SettingsService;
import org.sonarqube.ws.client.setting.ValuesRequest;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonarqube.ws.Settings.Setting;
import static org.sonarqube.ws.Settings.ValuesWsResponse;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.newWsClient;
import static util.ItUtils.resetSettings;

public class SettingsTest {

  /**
   * This setting is defined by server-plugin
   */
  private final static String PLUGIN_SETTING_KEY = "some-property";

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  static SettingsService anonymousSettingsService;
  static SettingsService userSettingsService;
  static SettingsService adminSettingsService;

  @BeforeClass
  public static void initSettingsService() throws Exception {
    userRule.createUser("setting-user", "setting-user");
    anonymousSettingsService = newWsClient(orchestrator).settingsService();
    userSettingsService = newUserWsClient(orchestrator, "setting-user", "setting-user").settingsService();
    adminSettingsService = newAdminWsClient(orchestrator).settingsService();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    userRule.deactivateUsers("setting-user");
  }

  @After
  public void reset_settings() throws Exception {
    resetSettings(orchestrator, null, PLUGIN_SETTING_KEY, "globalPropertyChange.received", "hidden", "setting.secured", "setting.license.secured");
  }

  /**
   * SONAR-3320
   */
  @Test
  public void global_property_change_extension_point() throws IOException {
    adminSettingsService.set(SetRequest.builder().setKey("globalPropertyChange.received").setValue("NEWVALUE").build());
    assertThat(FileUtils.readFileToString(orchestrator.getServer().getWebLogs()))
      .contains("Received change: [key=globalPropertyChange.received, newValue=NEWVALUE]");
  }

  @Test
  public void get_default_value() {
    Setting setting = getSetting(PLUGIN_SETTING_KEY, anonymousSettingsService);
    assertThat(setting.getValue()).isEqualTo("aDefaultValue");
    assertThat(setting.getInherited()).isTrue();
  }

  @Test
  public void set_setting() {
    adminSettingsService.set(SetRequest.builder().setKey(PLUGIN_SETTING_KEY).setValue("some value").build());

    String value = getSetting(PLUGIN_SETTING_KEY, anonymousSettingsService).getValue();
    assertThat(value).isEqualTo("some value");
  }

  @Test
  public void remove_setting() {
    adminSettingsService.set(SetRequest.builder().setKey(PLUGIN_SETTING_KEY).setValue("some value").build());
    adminSettingsService.set(SetRequest.builder().setKey("sonar.links.ci").setValue("http://localhost").build());

    adminSettingsService.reset(ResetRequest.builder().setKeys(PLUGIN_SETTING_KEY, "sonar.links.ci").build());
    assertThat(getSetting(PLUGIN_SETTING_KEY, anonymousSettingsService).getValue()).isEqualTo("aDefaultValue");
    assertThat(getSetting("sonar.links.ci", anonymousSettingsService)).isNull();
  }

  @Test
  public void hidden_setting() {
    adminSettingsService.set(SetRequest.builder().setKey("hidden").setValue("test").build());
    assertThat(getSetting("hidden", anonymousSettingsService).getValue()).isEqualTo("test");
  }

  @Test
  public void secured_setting() {
    adminSettingsService.set(SetRequest.builder().setKey("setting.secured").setValue("test").build());
    assertThat(getSetting("setting.secured", anonymousSettingsService)).isNull();
    assertThat(getSetting("setting.secured", userSettingsService)).isNull();
    assertThat(getSetting("setting.secured", adminSettingsService).getValue()).isEqualTo("test");
  }

  @Test
  public void license_setting() {
    adminSettingsService.set(SetRequest.builder().setKey("setting.license.secured").setValue("test").build());
    assertThat(getSetting("setting.license.secured", anonymousSettingsService)).isNull();
    assertThat(getSetting("setting.license.secured", userSettingsService).getValue()).isEqualTo("test");
    assertThat(getSetting("setting.license.secured", adminSettingsService).getValue()).isEqualTo("test");
  }

  @Test
  public void return_defined_settings_when_no_key_provided() throws Exception {
    adminSettingsService.set(SetRequest.builder().setKey(PLUGIN_SETTING_KEY).setValue("some value").build());
    adminSettingsService.set(SetRequest.builder().setKey("hidden").setValue("test").build());

    assertThat(adminSettingsService.values(ValuesRequest.builder().build()).getSettingsList())
      .extracting(Setting::getKey)
      .contains(PLUGIN_SETTING_KEY, "hidden", "sonar.forceAuthentication", "sonar.defaultGroup",
        // Settings for scanner
        "sonar.core.startTime");

    assertThat(adminSettingsService.values(ValuesRequest.builder().build()).getSettingsList())
      .extracting(Setting::getKey, Setting::getValue)
      .contains(tuple(PLUGIN_SETTING_KEY, "some value"), tuple("hidden", "test"));
  }

  @CheckForNull
  private static Setting getSetting(String key, SettingsService settingsService) {
    ValuesWsResponse response = settingsService.values(ValuesRequest.builder().setKeys(key).build());
    List<Settings.Setting> settings = response.getSettingsList();
    return settings.isEmpty() ? null : settings.get(0);
  }

}
