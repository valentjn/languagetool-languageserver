/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bsplines.ltexls.client.LtexLanguageClient;
import org.bsplines.ltexls.languagetool.LanguageToolRuleMatch;
import org.bsplines.ltexls.parsing.AnnotatedTextFragment;
import org.bsplines.ltexls.tools.Tools;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.lsp4j.ConfigurationItem;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.WorkDoneProgressBegin;
import org.eclipse.lsp4j.WorkDoneProgressCreateParams;
import org.eclipse.lsp4j.WorkDoneProgressEnd;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.xtext.xbase.lib.Pair;

public class LtexTextDocumentItem extends TextDocumentItem {
  private LtexLanguageServer languageServer;
  private List<Integer> lineStartPosList;
  private @Nullable Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>> checkingResult;
  private @Nullable List<Diagnostic> diagnostics;
  private @Nullable Position caretPosition;
  private Instant lastCaretChangeInstant;

  public LtexTextDocumentItem(LtexLanguageServer languageServer,
        String uri, String codeLanguageId, int version, String text) {
    super(uri, codeLanguageId, version, text);
    this.languageServer = languageServer;
    this.lineStartPosList = new ArrayList<>();
    this.checkingResult = null;
    this.diagnostics = null;
    this.caretPosition = null;
    this.lastCaretChangeInstant = Instant.now();
    reinitializeLineStartPosList(text, this.lineStartPosList);
  }

  public LtexTextDocumentItem(LtexLanguageServer languageServer, TextDocumentItem document) {
    this(languageServer, document.getUri(), document.getLanguageId(),
        document.getVersion(), document.getText());
  }

  private void reinitializeLineStartPosList() {
    reinitializeLineStartPosList(getText(), this.lineStartPosList);
  }

  private static void reinitializeLineStartPosList(String text, List<Integer> lineStartPosList) {
    lineStartPosList.clear();
    lineStartPosList.add(0);

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);

      if (c == '\r') {
        if ((i + 1 < text.length()) && (text.charAt(i + 1) == '\n')) i++;
        lineStartPosList.add(i + 1);
      } else if (c == '\n') {
        lineStartPosList.add(i + 1);
      }
    }
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if ((obj == null) || !LtexTextDocumentItem.class.isAssignableFrom(obj.getClass())) return false;
    LtexTextDocumentItem other = (LtexTextDocumentItem)obj;

    if (!super.equals(other)) return false;
    if (!this.lineStartPosList.equals(other.lineStartPosList)) return false;
    if (!Tools.equals(this.checkingResult, other.checkingResult)) return false;
    if (!Tools.equals(this.diagnostics, other.diagnostics)) return false;
    if (!Tools.equals(this.caretPosition, other.caretPosition)) return false;
    if (!this.lastCaretChangeInstant.equals(other.lastCaretChangeInstant)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int hash = 3;

    hash = 53 * hash + super.hashCode();
    hash = 53 * hash + this.lineStartPosList.hashCode();
    if (this.checkingResult != null) hash = 53 * hash + this.checkingResult.hashCode();
    if (this.diagnostics != null) hash = 53 * hash + this.diagnostics.hashCode();
    if (this.caretPosition != null) hash = 53 * hash + this.caretPosition.hashCode();
    hash = 53 * hash + this.lastCaretChangeInstant.hashCode();

    return hash;
  }

  public LtexLanguageServer getLanguageServer() {
    return this.languageServer;
  }

  public int convertPosition(Position position) {
    int line = position.getLine();
    int character = position.getCharacter();
    String text = getText();

    if (line < 0) {
      return 0;
    } else if (line >= this.lineStartPosList.size()) {
      return text.length();
    } else {
      int lineStart = this.lineStartPosList.get(line);
      int nextLineStart = ((line < this.lineStartPosList.size() - 1)
          ? this.lineStartPosList.get(line + 1) : text.length());
      int lineLength = nextLineStart - lineStart;

      if (character < 0) {
        return lineStart;
      } else if (character >= lineLength) {
        int pos = lineStart + lineLength;

        if (pos >= 1) {
          if (text.charAt(pos - 1) == '\r') {
            pos--;
          } else if (text.charAt(pos - 1) == '\n') {
            pos--;
            if ((pos >= 1) && (text.charAt(pos - 1) == '\r')) pos--;
          }
        }

        return pos;
      } else {
        return lineStart + character;
      }
    }
  }

  public Position convertPosition(int pos) {
    int line = Collections.binarySearch(this.lineStartPosList, pos);

    if (line < 0) {
      int insertionPoint = -line - 1;
      line = insertionPoint - 1;
    }

    return new Position(line, pos - this.lineStartPosList.get(line));
  }

  public @Nullable Position getCaretPosition() {
    return ((this.caretPosition != null)
        ? new Position(this.caretPosition.getLine(), this.caretPosition.getCharacter()) : null);
  }

  public void setCaretPosition(@Nullable Position caretPosition) {
    if (caretPosition != null) {
      if (this.caretPosition != null) {
        this.caretPosition.setLine(caretPosition.getLine());
        this.caretPosition.setCharacter(caretPosition.getCharacter());
      } else {
        this.caretPosition = new Position(caretPosition.getLine(), caretPosition.getCharacter());
      }
    } else {
      this.caretPosition = null;
    }
  }

  public Instant getLastCaretChangeInstant() {
    return this.lastCaretChangeInstant;
  }

  public void setLastCaretChangeInstant(Instant lastCaretChangeInstant) {
    this.lastCaretChangeInstant = lastCaretChangeInstant;
  }

  @Override
  public void setText(String text) {
    final String oldText = getText();
    super.setText(text);
    reinitializeLineStartPosList();
    this.checkingResult = null;
    this.diagnostics = null;
    this.caretPosition = guessCaretPositionInFullUpdate(oldText);
    if (this.caretPosition != null) this.lastCaretChangeInstant = Instant.now();
  }

  public void applyTextChangeEvents(List<TextDocumentContentChangeEvent> textChangeEvents) {
    Instant oldLastCaretChangeInstant = this.lastCaretChangeInstant;

    for (TextDocumentContentChangeEvent textChangeEvent : textChangeEvents) {
      applyTextChangeEvent(textChangeEvent);
    }

    if (textChangeEvents.size() > 1) {
      this.caretPosition = null;
      this.lastCaretChangeInstant = oldLastCaretChangeInstant;
    }
  }

  public void applyTextChangeEvent(TextDocumentContentChangeEvent textChangeEvent) {
    Range changeRange = textChangeEvent.getRange();
    String changeText = textChangeEvent.getText();
    int fromPos = -1;
    int toPos = -1;
    String oldText = getText();
    String newText;

    if (changeRange != null) {
      fromPos = convertPosition(changeRange.getStart());
      toPos = ((changeRange.getEnd() != changeRange.getStart())
          ? convertPosition(changeRange.getEnd()) : fromPos);
      newText = oldText.substring(0, fromPos) + changeText + oldText.substring(toPos);
    } else {
      newText = changeText;
    }

    super.setText(newText);
    reinitializeLineStartPosList();
    this.checkingResult = null;
    this.diagnostics = null;

    if (changeRange != null) {
      this.caretPosition = guessCaretPositionInIncrementalUpdate(
          changeRange, changeText, fromPos, toPos);
    } else {
      this.caretPosition = guessCaretPositionInFullUpdate(oldText);
    }

    if (this.caretPosition != null) this.lastCaretChangeInstant = Instant.now();
  }

  private @Nullable Position guessCaretPositionInIncrementalUpdate(
        Range changeRange, String changeText, int fromPos, int toPos) {
    @Nullable Position caretPosition = null;

    if (fromPos == toPos) {
      caretPosition = convertPosition(toPos + changeText.length());
    } else if (changeText.isEmpty()) {
      caretPosition = new Position(changeRange.getStart().getLine(),
          changeRange.getStart().getCharacter());
    }

    return caretPosition;
  }

  private @Nullable Position guessCaretPositionInFullUpdate(String oldText) {
    String newText = getText();
    int numberOfEqualCharsAtStart = 0;

    while ((numberOfEqualCharsAtStart < oldText.length())
          && (numberOfEqualCharsAtStart < newText.length())
          && (oldText.charAt(numberOfEqualCharsAtStart)
            == newText.charAt(numberOfEqualCharsAtStart))) {
      numberOfEqualCharsAtStart++;
    }

    int numberOfEqualCharsAtEnd = 0;

    while ((numberOfEqualCharsAtEnd < oldText.length() - numberOfEqualCharsAtStart)
          && (numberOfEqualCharsAtEnd < newText.length() - numberOfEqualCharsAtStart)
          && (oldText.charAt(oldText.length() - numberOfEqualCharsAtEnd - 1)
            == newText.charAt(newText.length() - numberOfEqualCharsAtEnd - 1))) {
      numberOfEqualCharsAtEnd++;
    }

    int numberOfEqualChars = numberOfEqualCharsAtStart + numberOfEqualCharsAtEnd;

    if ((numberOfEqualChars < 0.5 * oldText.length())
          || (numberOfEqualChars < 0.5 * newText.length())) {
      return null;
    }

    return convertPosition(newText.length() - numberOfEqualCharsAtEnd);
  }

  public CompletableFuture<Boolean> checkAndPublishDiagnosticsWithCache() {
    return checkAndPublishDiagnostics(null, true);
  }

  public CompletableFuture<Boolean> checkAndPublishDiagnosticsWithCache(@Nullable Range range) {
    return checkAndPublishDiagnostics(range, true);
  }

  public CompletableFuture<Boolean> checkAndPublishDiagnosticsWithoutCache() {
    return checkAndPublishDiagnostics(null, false);
  }

  public CompletableFuture<Boolean> checkAndPublishDiagnosticsWithoutCache(@Nullable Range range) {
    return checkAndPublishDiagnostics(range, false);
  }

  private CompletableFuture<Boolean> checkAndPublishDiagnostics(
        @Nullable Range range, boolean useCache) {
    @Nullable LtexLanguageClient languageClient = this.languageServer.getLanguageClient();

    return checkAndGetDiagnostics(range, useCache).thenApply((List<Diagnostic> diagnostics) -> {
      if (languageClient == null) return false;
      @Nullable List<Diagnostic> diagnosticsNotAtCaret = extractDiagnosticsNotAtCaret();
      if (diagnosticsNotAtCaret == null) return false;
      languageClient.publishDiagnostics(new PublishDiagnosticsParams(
          getUri(), diagnosticsNotAtCaret));

      if (diagnosticsNotAtCaret.size() < diagnostics.size()) {
        Thread thread = new Thread(new DelayedDiagnosticsPublisherRunnable(
            languageClient, this));
        thread.start();
      }

      return true;
    });
  }

  private CompletableFuture<List<Diagnostic>> checkAndGetDiagnostics(
        @Nullable Range range, boolean useCache) {
    if (useCache && (this.diagnostics != null)) {
      return CompletableFuture.completedFuture(this.diagnostics);
    }

    return check(range, useCache).thenApply(
        (Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>> checkingResult) -> {
          List<LanguageToolRuleMatch> matches = checkingResult.getKey();
          List<Diagnostic> diagnostics = new ArrayList<>();

          for (LanguageToolRuleMatch match : matches) {
            diagnostics.add(this.languageServer.getCodeActionGenerator().createDiagnostic(
                match, this));
          }

          this.diagnostics = diagnostics;
          return diagnostics;
        });
  }

  public @Nullable List<Diagnostic> getDiagnosticsCache() {
    return ((this.diagnostics != null) ? Collections.unmodifiableList(this.diagnostics) : null);
  }

  private @Nullable List<Diagnostic> extractDiagnosticsNotAtCaret() {
    if (this.diagnostics == null) return null;
    if (this.caretPosition == null) return Collections.unmodifiableList(this.diagnostics);
    List<Diagnostic> diagnosticsNotAtCaret = new ArrayList<>();
    int character = this.caretPosition.getCharacter();
    Position beforeCaretPosition = new Position(this.caretPosition.getLine(),
        ((character >= 1) ? (character - 1) : 0));
    Range caretRange = new Range(beforeCaretPosition, this.caretPosition);
    if (this.diagnostics == null) return null;

    for (Diagnostic diagnostic : this.diagnostics) {
      if (!Tools.areRangesIntersecting(diagnostic.getRange(), caretRange)) {
        diagnosticsNotAtCaret.add(diagnostic);
      }
    }

    return diagnosticsNotAtCaret;
  }

  public CompletableFuture<Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>>>
        checkWithCache() {
    return check(null, true);
  }

  public CompletableFuture<Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>>>
        checkWithCache(@Nullable Range range) {
    return check(range, true);
  }

  public CompletableFuture<Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>>>
        checkWithoutCache() {
    return check(null, false);
  }

  public CompletableFuture<Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>>>
        checkWithoutCache(@Nullable Range range) {
    return check(range, false);
  }

  private CompletableFuture<Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>>> check(
        @Nullable Range range, boolean useCache) {
    if (useCache && (this.checkingResult != null)) {
      return CompletableFuture.completedFuture(this.checkingResult);
    }

    @Nullable LtexLanguageClient languageClient = this.languageServer.getLanguageClient();

    if (languageClient == null) {
      return CompletableFuture.completedFuture(
          Pair.of(Collections.emptyList(), Collections.emptyList()));
    }

    String uri = getUri();
    JsonObject progressJsonToken = new JsonObject();
    progressJsonToken.addProperty("uri", uri);
    progressJsonToken.addProperty("operation", "checkDocument");
    progressJsonToken.addProperty("uuid", Tools.getRandomUuid());
    Either<String, Number> progressToken = Either.forLeft(progressJsonToken.toString());

    final CompletableFuture<@Nullable Either<String, Number>> workDoneProgressCreateFuture =
        ((this.languageServer.isClientSupportingWorkDoneProgress())
          ? languageClient.createProgress(new WorkDoneProgressCreateParams(progressToken)).handle(
            (Void voidObject, @Nullable Throwable e) -> {
              if (e == null) {
                WorkDoneProgressBegin workDoneProgressBegin = new WorkDoneProgressBegin();
                workDoneProgressBegin.setTitle(Tools.i18n("checkingDocument"));
                workDoneProgressBegin.setMessage(uri);
                workDoneProgressBegin.setCancellable(false);
                languageClient.notifyProgress(new ProgressParams(
                    progressToken, workDoneProgressBegin));
                return progressToken;
              } else {
                return null;
              }
            })
          : CompletableFuture.completedFuture(null));

    ConfigurationItem configurationItem = new ConfigurationItem();
    configurationItem.setScopeUri(uri);
    configurationItem.setSection("ltex");
    ConfigurationParams configurationParams = new ConfigurationParams(
        Collections.singletonList(configurationItem));

    CompletableFuture<List<Object>> intermediateResult1 = workDoneProgressCreateFuture.thenCompose(
        (@Nullable Either<String, Number> curProgressToken) -> {
        return languageClient.configuration(configurationParams);
      });

    @SuppressWarnings({"assignment.type.incompatible", "return.type.incompatible"})
    CompletableFuture<Pair<List<Object>, List<@Nullable Object>>> intermediateResult2 =
        intermediateResult1.thenCompose(
          (List<Object> configurationResult) -> {
            return (this.languageServer.isClientSupportingWorkspaceSpecificConfiguration()
                ? languageClient.ltexWorkspaceSpecificConfiguration(configurationParams)
                : CompletableFuture.completedFuture(Collections.singletonList(null))).thenApply(
                  (List<@Nullable Object> workspaceSpecificConfigurationResult) -> {
                    return Pair.of(configurationResult, workspaceSpecificConfigurationResult);
                  });
          });

    CompletableFuture<Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>>>
        intermediateResult3 = intermediateResult2.thenApply(
          (Pair<List<Object>, List<@Nullable Object>> futureArgument) -> {
            List<Object> configurationResult = futureArgument.getKey();
            List<@Nullable Object> workspaceSpecificConfigurationResult = futureArgument.getValue();

            try {
              JsonElement jsonConfiguration = (JsonElement)configurationResult.get(0);
              @Nullable Object workspaceSpecificConfiguration =
                  workspaceSpecificConfigurationResult.get(0);
              @Nullable JsonElement jsonWorkspaceSpecificConfiguration =
                  ((workspaceSpecificConfiguration != null)
                    ? (JsonElement)workspaceSpecificConfiguration : null);

              this.languageServer.getSettingsManager().setSettings(
                  jsonConfiguration, jsonWorkspaceSpecificConfiguration);

              Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>> checkingResult =
                  this.languageServer.getDocumentChecker().check(this, range);
              this.checkingResult = checkingResult;

              return checkingResult;
            } finally {
              @Nullable Either<String, Number> curProgressToken =
                  workDoneProgressCreateFuture.join();

              if ((languageClient != null) && (curProgressToken != null)) {
                languageClient.notifyProgress(new ProgressParams(
                    curProgressToken, new WorkDoneProgressEnd()));
              }
            }
          });

    return intermediateResult3;
  }
}
