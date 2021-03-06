/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.languagetool;

import org.bsplines.ltexls.settings.Settings;
import org.bsplines.ltexls.settings.SettingsManager;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.util.NullnessUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.languagetool.server.HTTPServer;

@TestInstance(Lifecycle.PER_CLASS)
public class LanguageToolHttpInterfaceTest {
  private @MonotonicNonNull Thread serverThread;
  private Settings defaultSettings;

  public LanguageToolHttpInterfaceTest() {
    this.defaultSettings = new Settings();
  }

  @BeforeAll
  public void setUp() throws InterruptedException {
    this.serverThread = new Thread(() -> {
      HTTPServer.main(new String[]{"--port", "8081", "--allow-origin", "*"});
    });
    this.serverThread.start();

    // wait until LanguageTool has initialized itself
    Thread.sleep(5000);

    this.defaultSettings = this.defaultSettings.withLanguageToolHttpServerUri(
        "http://localhost:8081");
  }

  @AfterAll
  public void tearDown() {
    if (this.serverThread != null) this.serverThread.interrupt();
  }

  @Test
  public void testConstructor() {
    Assertions.assertTrue(new LanguageToolHttpInterface(
        "http://localhost:8081/", "en-US", "").isReady());
    Assertions.assertFalse(new LanguageToolHttpInterface(
        "http://localhost:80:81/", "en-US", "").isReady());
  }

  @Test
  public void testCheck() {
    LanguageToolJavaInterfaceTest.assertMatches(this.defaultSettings, false);
  }

  @Test
  public void testOtherMethods() {
    SettingsManager settingsManager = new SettingsManager(this.defaultSettings);
    LanguageToolInterface ltInterface = settingsManager.getLanguageToolInterface();
    Assertions.assertNotNull(NullnessUtil.castNonNull(ltInterface));
    Assertions.assertDoesNotThrow(() -> ltInterface.activateDefaultFalseFriendRules());
    Assertions.assertDoesNotThrow(() -> ltInterface.activateLanguageModelRules("foobar"));
    Assertions.assertDoesNotThrow(() -> ltInterface.activateNeuralNetworkRules("foobar"));
    Assertions.assertDoesNotThrow(() -> ltInterface.activateWord2VecModelRules("foobar"));
    Assertions.assertDoesNotThrow(() -> ltInterface.enableEasterEgg());
  }
}
