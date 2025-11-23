package dev.fmcuttingboard.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Phase 7.1 — Plugin Settings
 * Project-level settings persisted via IntelliJ PersistentStateComponent.
 */
@Service(Service.Level.PROJECT)
@State(
        name = "FMCuttingBoardSettings",
        storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)}
)
public final class FmCuttingBoardSettingsState implements PersistentStateComponent<FmCuttingBoardSettingsState.State> {

    public static class State {
        public String baseDirName = ".fmCuttingBoard";
        // Filename pattern WITHOUT extension. Extensions are added per created type (.xml, .fmcalc)
        public String fileNamePattern = "{timestamp}";
        public boolean previewBeforeClipboardWrite = false;
        public boolean enableDiagnostics = false;
    }

    private State state = new State();

    public static FmCuttingBoardSettingsState getInstance(@NotNull Project project) {
        return project.getService(FmCuttingBoardSettingsState.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
        if (this.state.baseDirName == null || this.state.baseDirName.isBlank()) {
            this.state.baseDirName = ".fmCuttingBoard";
        }
        if (this.state.fileNamePattern == null || this.state.fileNamePattern.isBlank()) {
            this.state.fileNamePattern = "{timestamp}";
        }
        // ensure non-null booleans have sensible defaults
        // (boolean defaults to false if missing from persisted state)
        // maintain backward compatibility if the field did not exist in older configs
        // (Java boolean defaults to false, so nothing to do here)
    }

    // Convenience getters/setters
    public String getBaseDirName() { return state.baseDirName; }
    public void setBaseDirName(String v) { state.baseDirName = (v == null || v.isBlank()) ? ".fmCuttingBoard" : v; }

    public String getFileNamePattern() { return state.fileNamePattern; }
    public void setFileNamePattern(String v) { state.fileNamePattern = (v == null || v.isBlank()) ? "{timestamp}" : v; }

    public boolean isPreviewBeforeClipboardWrite() { return state.previewBeforeClipboardWrite; }
    public void setPreviewBeforeClipboardWrite(boolean v) { state.previewBeforeClipboardWrite = v; }

    public boolean isEnableDiagnostics() { return state.enableDiagnostics; }
    public void setEnableDiagnostics(boolean v) { state.enableDiagnostics = v; }
}
