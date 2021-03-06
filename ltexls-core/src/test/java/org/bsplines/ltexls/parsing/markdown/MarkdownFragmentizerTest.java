/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.parsing.markdown;

import java.util.List;
import org.bsplines.ltexls.parsing.CodeFragment;
import org.bsplines.ltexls.parsing.CodeFragmentizer;
import org.bsplines.ltexls.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MarkdownFragmentizerTest {
  private static void testFragmentizer(CodeFragmentizer fragmentizer, String code) {
    List<CodeFragment> codeFragments = fragmentizer.fragmentize(code, new Settings());
    Assertions.assertEquals(3, codeFragments.size());

    Assertions.assertEquals("markdown", codeFragments.get(0).getCodeLanguageId());
    Assertions.assertEquals(0, codeFragments.get(0).getFromPos());
    Assertions.assertEquals(11, codeFragments.get(0).getCode().length());
    Assertions.assertEquals("en-US", codeFragments.get(0).getSettings().getLanguageShortCode());

    Assertions.assertEquals("markdown", codeFragments.get(1).getCodeLanguageId());
    Assertions.assertEquals(11, codeFragments.get(1).getFromPos());
    Assertions.assertEquals(50, codeFragments.get(1).getCode().length());
    Assertions.assertEquals("de-DE", codeFragments.get(1).getSettings().getLanguageShortCode());

    Assertions.assertEquals("markdown", codeFragments.get(2).getCodeLanguageId());
    Assertions.assertEquals(61, codeFragments.get(2).getFromPos());
    Assertions.assertEquals(49, codeFragments.get(2).getCode().length());
    Assertions.assertEquals("en-US", codeFragments.get(2).getSettings().getLanguageShortCode());
  }

  @Test
  public void test() {
    CodeFragmentizer fragmentizer = CodeFragmentizer.create("markdown");

    testFragmentizer(fragmentizer,
        "Sentence 1\n"
        + "\n[comment]: <> \"ltex: language=de-DE\"\n\nSentence 2\n"
        + "\n[comment]:\t<>\"ltex:\tlanguage=en-US\"\n\nSentence 3\n");

    testFragmentizer(fragmentizer,
        "Sentence 1\n"
        + "\n  <!-- ltex: language=de-DE-->      \n\nSentence 2\n"
        + "\n<!--\t\t\tltex:\t\t\t\tlanguage=en-US\t\t-->\n\nSentence 3\n");
  }

  @Test
  public void testWrongSettings() {
    MarkdownFragmentizer markdownFragmentizer =
        new MarkdownFragmentizer("markdown");
    Assertions.assertDoesNotThrow(() -> markdownFragmentizer.fragmentize(
        "Sentence 1\n[comment]: <> \"ltex: languagede-DE\"\n\nSentence 2\n", new Settings()));
    Assertions.assertDoesNotThrow(() -> markdownFragmentizer.fragmentize(
        "Sentence 1\n[comment]: <> \"ltex: unknownKey=abc\"\n\nSentence 2\n", new Settings()));
  }
}
