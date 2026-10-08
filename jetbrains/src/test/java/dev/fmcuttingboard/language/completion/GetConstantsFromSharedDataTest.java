package dev.fmcuttingboard.language.completion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Get() completion list moved from hardcoded Java (plugin 1.0.6) to shared data, now the vendored
 * fmscriptinventory catalogue (shared/data/fm-calc-catalogue.json). Nothing the plugin used to offer may go missing,
 * except two names the old hand-written list had wrong: "ScreenScale" is really ScreenScaleFactor, and "ModelName"
 * is not a documented Get function (help.claris.com's get-modelname.html redirects to Claris's 404 page).
 */
public class GetConstantsFromSharedDataTest {

    @Test
    void stillOffersEveryConstantFromBeforeTheMove() {
        List<String> loaded = FileMakerCalculationCompletionContributor.loadGetConstants();
        List<String> before = List.of(
                "AccountName", "ApplicationLanguage", "CurrentTimeUTCMilliseconds",
                "Device", "DocumentsPath", "FileName", "HostName", "LastError",
                "LayoutName", "ModifiedFields", "NetworkProtocol",
                "PersistentID", "PreferencesPath", "ScreenScaleFactor", "ScriptName",
                "SystemLanguage", "TotalRecordCount", "UserName", "WindowName");
        assertTrue(loaded.containsAll(before), () -> "missing: " + before.stream().filter(n -> !loaded.contains(n)).toList());
        assertTrue(loaded.size() > before.size(), "expected the full catalogue list, got " + loaded.size());
    }
}
