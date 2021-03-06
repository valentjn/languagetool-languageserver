/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.logging.Level;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SettingsTest {
  private static Settings compareSettings(Settings settings, Settings otherSettings,
        boolean differenceRelevant) {
    Settings settings2 = new Settings(settings);
    Settings otherSettings2 = new Settings(otherSettings);

    Assertions.assertEquals(settings.hashCode(), settings2.hashCode());
    Assertions.assertEquals(otherSettings.hashCode(), otherSettings2.hashCode());

    Assertions.assertTrue(settings2.equals(settings));
    Assertions.assertTrue(settings.equals(settings2));
    Assertions.assertTrue(otherSettings2.equals(otherSettings));
    Assertions.assertTrue(otherSettings.equals(otherSettings2));

    Assertions.assertFalse(otherSettings.equals(settings));
    Assertions.assertFalse(settings.equals(otherSettings));
    Assertions.assertFalse(otherSettings.equals(settings2));
    Assertions.assertFalse(settings2.equals(otherSettings));
    Assertions.assertFalse(otherSettings2.equals(settings));
    Assertions.assertFalse(settings.equals(otherSettings2));
    Assertions.assertFalse(otherSettings2.equals(settings2));
    Assertions.assertFalse(settings2.equals(otherSettings2));

    Assertions.assertTrue(
        settings.getDifferencesRelevantForLanguageTool(settings2).isEmpty());
    Assertions.assertTrue(
        settings2.getDifferencesRelevantForLanguageTool(settings).isEmpty());
    Assertions.assertTrue(
        otherSettings.getDifferencesRelevantForLanguageTool(otherSettings2).isEmpty());
    Assertions.assertTrue(
        otherSettings2.getDifferencesRelevantForLanguageTool(otherSettings).isEmpty());

    Assertions.assertEquals(!differenceRelevant,
        settings.getDifferencesRelevantForLanguageTool(otherSettings).isEmpty());
    Assertions.assertEquals(!differenceRelevant,
        otherSettings.getDifferencesRelevantForLanguageTool(settings).isEmpty());
    Assertions.assertEquals(!differenceRelevant,
        settings2.getDifferencesRelevantForLanguageTool(otherSettings).isEmpty());
    Assertions.assertEquals(!differenceRelevant,
        otherSettings.getDifferencesRelevantForLanguageTool(settings2).isEmpty());
    Assertions.assertEquals(!differenceRelevant,
        settings.getDifferencesRelevantForLanguageTool(otherSettings2).isEmpty());
    Assertions.assertEquals(!differenceRelevant,
        otherSettings2.getDifferencesRelevantForLanguageTool(settings).isEmpty());
    Assertions.assertEquals(!differenceRelevant,
        settings2.getDifferencesRelevantForLanguageTool(otherSettings2).isEmpty());
    Assertions.assertEquals(!differenceRelevant,
        otherSettings2.getDifferencesRelevantForLanguageTool(settings2).isEmpty());

    return settings2;
  }

  @Test
  public void testJsonSettings() {
    JsonObject bibtexFields = new JsonObject();
    bibtexFields.addProperty("bibtexField", false);

    JsonObject bibtex = new JsonObject();
    bibtex.add("fields", bibtexFields);

    JsonObject latexEnvironments = new JsonObject();
    latexEnvironments.addProperty("latexEnvironment", "ignore");

    JsonObject latexCommands = new JsonObject();
    latexCommands.addProperty("\\latexCommand{}", "ignore");

    JsonObject latex = new JsonObject();
    latex.add("commands", latexCommands);
    latex.add("environments", latexEnvironments);

    JsonObject markdownNodes = new JsonObject();
    markdownNodes.addProperty("markdownNode", "ignore");

    JsonObject markdown = new JsonObject();
    markdown.add("nodes", markdownNodes);

    JsonObject additionalRules = new JsonObject();
    additionalRules.addProperty("enablePickyRules", true);

    JsonObject jsonSettings = new JsonObject();
    jsonSettings.addProperty("enabled", false);
    jsonSettings.add("bibtex", bibtex);
    jsonSettings.add("latex", latex);
    jsonSettings.add("markdown", markdown);
    jsonSettings.add("additionalRules", additionalRules);

    JsonArray englishDictionary = new JsonArray();
    englishDictionary.add("wordone");
    englishDictionary.add("wordtwo");
    englishDictionary.add("-wordone");
    JsonObject dictionary = new JsonObject();
    dictionary.add("en-US", englishDictionary);

    JsonArray englishHiddenFalsePositives = new JsonArray();
    englishHiddenFalsePositives.add("{\"rule\": \"rule\", \"sentence\": \"sentence\"}");
    JsonObject hiddenFalsePositives = new JsonObject();
    hiddenFalsePositives.add("en-US", englishHiddenFalsePositives);

    JsonObject jsonWorkspaceSpecificSettings = new JsonObject();
    jsonWorkspaceSpecificSettings.add("dictionary", dictionary);
    jsonWorkspaceSpecificSettings.add("hiddenFalsePositives", hiddenFalsePositives);

    Settings settings = new Settings(jsonSettings, jsonWorkspaceSpecificSettings);
    Assertions.assertEquals(Collections.emptySet(), settings.getEnabled());
    Assertions.assertEquals(Collections.singleton("wordtwo"), settings.getDictionary());
    Assertions.assertEquals(Collections.singleton(new HiddenFalsePositive("rule", "sentence")),
        settings.getHiddenFalsePositives());
    Assertions.assertEquals(Collections.singletonMap("bibtexField", false),
        settings.getBibtexFields());
    Assertions.assertEquals(Collections.singletonMap("\\latexCommand{}", "ignore"),
        settings.getLatexCommands());
    Assertions.assertEquals(Collections.singletonMap("latexEnvironment", "ignore"),
        settings.getLatexEnvironments());
    Assertions.assertEquals(Collections.singletonMap("markdownNode", "ignore"),
        settings.getMarkdownNodes());
    Assertions.assertEquals(true, settings.getEnablePickyRules());

    jsonSettings.addProperty("diagnosticSeverity", "error");
    settings = new Settings(jsonSettings, jsonWorkspaceSpecificSettings);
    Assertions.assertEquals(DiagnosticSeverity.Error, settings.getDiagnosticSeverity());

    jsonSettings.addProperty("diagnosticSeverity", "warning");
    settings = new Settings(jsonSettings, jsonWorkspaceSpecificSettings);
    Assertions.assertEquals(DiagnosticSeverity.Warning, settings.getDiagnosticSeverity());

    jsonSettings.addProperty("diagnosticSeverity", "information");
    settings = new Settings(jsonSettings, jsonWorkspaceSpecificSettings);
    Assertions.assertEquals(DiagnosticSeverity.Information, settings.getDiagnosticSeverity());

    jsonSettings.addProperty("diagnosticSeverity", "hint");
    settings = new Settings(jsonSettings, jsonWorkspaceSpecificSettings);
    Assertions.assertEquals(DiagnosticSeverity.Hint, settings.getDiagnosticSeverity());

    jsonSettings.addProperty("checkFrequency", "edit");
    settings = new Settings(jsonSettings, jsonWorkspaceSpecificSettings);
    Assertions.assertEquals(CheckFrequency.EDIT, settings.getCheckFrequency());

    jsonSettings.addProperty("checkFrequency", "save");
    settings = new Settings(jsonSettings, jsonWorkspaceSpecificSettings);
    Assertions.assertEquals(CheckFrequency.SAVE, settings.getCheckFrequency());

    jsonSettings.addProperty("checkFrequency", "manual");
    settings = new Settings(jsonSettings, jsonWorkspaceSpecificSettings);
    Assertions.assertEquals(CheckFrequency.MANUAL, settings.getCheckFrequency());
  }

  @Test
  public void testProperties() {
    Settings settings = new Settings();
    Settings settings2 = new Settings();

    settings = settings.withEnabled(false);
    Assertions.assertEquals(Collections.emptySet(), settings.getEnabled());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withEnabled(Collections.singleton("markdown"));
    Assertions.assertEquals(Collections.singleton("markdown"), settings.getEnabled());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withLanguageShortCode("languageShortCode");
    Assertions.assertEquals("languageShortCode", settings.getLanguageShortCode());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withDictionary(Collections.singleton("dictionary"));
    Assertions.assertEquals(Collections.singleton("dictionary"), settings.getDictionary());
    settings2 = compareSettings(settings, settings2, true);

    settings = settings.withDisabledRules(Collections.singleton("disabledRules"));
    Assertions.assertEquals(Collections.singleton("disabledRules"),
        settings.getDisabledRules());
    settings2 = compareSettings(settings, settings2, true);

    settings = settings.withEnabledRules(Collections.singleton("enabledRules"));
    Assertions.assertEquals(Collections.singleton("enabledRules"),
        settings.getEnabledRules());
    settings2 = compareSettings(settings, settings2, true);

    settings = settings.withBibtexFields(Collections.singletonMap("bibtexField", false));
    Assertions.assertEquals(Collections.singletonMap("bibtexField", false),
        settings.getBibtexFields());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withLatexCommands(Collections.singletonMap("latexCommand", "ignore"));
    Assertions.assertEquals(Collections.singletonMap("latexCommand", "ignore"),
        settings.getLatexCommands());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withLatexEnvironments(
        Collections.singletonMap("latexEnvironment", "ignore"));
    Assertions.assertEquals(Collections.singletonMap("latexEnvironment", "ignore"),
        settings.getLatexEnvironments());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withMarkdownNodes(Collections.singletonMap("markdownNode", "ignore"));
    Assertions.assertEquals(Collections.singletonMap("markdownNode", "ignore"),
        settings.getMarkdownNodes());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withHiddenFalsePositives(Collections.singleton(
        new HiddenFalsePositive("ruleId", "sentenceString")));
    Assertions.assertEquals(Collections.singleton(
        new HiddenFalsePositive("ruleId", "sentenceString")),
        settings.getHiddenFalsePositives());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withEnablePickyRules(true);
    Assertions.assertEquals(true, settings.getEnablePickyRules());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withMotherTongueShortCode("motherTongueShortCode");
    Assertions.assertEquals("motherTongueShortCode", settings.getMotherTongueShortCode());
    settings2 = compareSettings(settings, settings2, true);

    settings = settings.withLanguageModelRulesDirectory("languageModelRulesDirectory");
    Assertions.assertEquals("languageModelRulesDirectory",
        settings.getLanguageModelRulesDirectory());
    settings2 = compareSettings(settings, settings2, true);

    settings = settings.withNeuralNetworkModelRulesDirectory("neuralNetworkModelRulesDirectory");
    Assertions.assertEquals("neuralNetworkModelRulesDirectory",
        settings.getNeuralNetworkModelRulesDirectory());
    settings2 = compareSettings(settings, settings2, true);

    settings = settings.withWord2VecModelRulesDirectory("word2VecModelRulesDirectory");
    Assertions.assertEquals("word2VecModelRulesDirectory",
        settings.getWord2VecModelRulesDirectory());
    settings2 = compareSettings(settings, settings2, true);

    settings = settings.withLanguageToolHttpServerUri("languageToolHttpServerUri");
    Assertions.assertEquals("languageToolHttpServerUri", settings.getLanguageToolHttpServerUri());
    settings2 = compareSettings(settings, settings2, true);

    settings = settings.withLogLevel(Level.FINEST);
    Assertions.assertEquals(Level.FINEST, settings.getLogLevel());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withSentenceCacheSize(1337);
    Assertions.assertEquals(1337, settings.getSentenceCacheSize());
    settings2 = compareSettings(settings, settings2, true);

    settings = settings.withDiagnosticSeverity(DiagnosticSeverity.Error);
    Assertions.assertEquals(DiagnosticSeverity.Error, settings.getDiagnosticSeverity());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withCheckFrequency(CheckFrequency.MANUAL);
    Assertions.assertEquals(CheckFrequency.MANUAL, settings.getCheckFrequency());
    settings2 = compareSettings(settings, settings2, false);

    settings = settings.withClearDiagnosticsWhenClosingFile(false);
    Assertions.assertEquals(false, settings.getClearDiagnosticsWhenClosingFile());
    settings2 = compareSettings(settings, settings2, false);
  }

  @Test
  public void testTildeExpansion() {
    Settings settings = new Settings();
    String originalDirPath = "~/tildeExpansion";
    settings = settings.withLanguageModelRulesDirectory(originalDirPath);
    String expandedDirPath = settings.getLanguageModelRulesDirectory();
    Assertions.assertTrue(expandedDirPath.endsWith("tildeExpansion"));
    Assertions.assertNotEquals(originalDirPath, expandedDirPath);
  }
}
