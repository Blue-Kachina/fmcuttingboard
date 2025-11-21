package dev.fmcuttingboard.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FmCuttingBoardSettingsStateTest {

    @Test
    void defaultsAreApplied() {
        FmCuttingBoardSettingsState state = new FmCuttingBoardSettingsState();
        assertEquals(".fmCuttingBoard", state.getBaseDirName());
        assertEquals("fmclip-{timestamp}.xml", state.getFileNamePattern());
    }

    @Test
    void loadStateAppliesDefaultsWhenBlankOrNull() {
        FmCuttingBoardSettingsState s = new FmCuttingBoardSettingsState();
        FmCuttingBoardSettingsState.State raw = new FmCuttingBoardSettingsState.State();
        raw.baseDirName = ""; // blank should default
        raw.fileNamePattern = null; // null should default
        s.loadState(raw);
        assertEquals(".fmCuttingBoard", s.getBaseDirName());
        assertEquals("fmclip-{timestamp}.xml", s.getFileNamePattern());
    }

    @Test
    void settersNormalizeBlankValuesToDefaults() {
        FmCuttingBoardSettingsState s = new FmCuttingBoardSettingsState();
        s.setBaseDirName("");
        s.setFileNamePattern("");
        assertEquals(".fmCuttingBoard", s.getBaseDirName());
        assertEquals("fmclip-{timestamp}.xml", s.getFileNamePattern());

        s.setBaseDirName("clips");
        s.setFileNamePattern("my-{timestamp}.xml");
        assertEquals("clips", s.getBaseDirName());
        assertEquals("my-{timestamp}.xml", s.getFileNamePattern());
    }
}
