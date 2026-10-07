package dev.fmcuttingboard.language.completion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The Get() completion list moved from hardcoded Java (plugin 1.0.6) to shared/data/calc-language.json. */
public class GetConstantsFromSharedDataTest {

    @Test
    void sameConstantsAsBeforeTheMove() {
        assertEquals(List.of(
                "AccountName", "ApplicationLanguage", "CurrentTimeUTCMilliseconds",
                "Device", "DocumentsPath", "FileName", "HostName", "LastError",
                "LayoutName", "ModelName", "ModifiedFields", "NetworkProtocol",
                "PersistentID", "PreferencesPath", "ScreenScale", "ScriptName",
                "SystemLanguage", "TotalRecordCount", "UserName", "WindowName"
        ), FileMakerCalculationCompletionContributor.loadGetConstants());
    }
}
