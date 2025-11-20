package dev.fmcuttingboard.clipboard;

import com.intellij.openapi.ide.CopyPasteManager;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Optional;

/**
 * Default implementation backed by IntelliJ's CopyPasteManager.
 */
public class DefaultClipboardService implements ClipboardService {

    private final CopyPasteManager manager;

    public DefaultClipboardService() {
        this(CopyPasteManager.getInstance());
    }

    public DefaultClipboardService(CopyPasteManager manager) {
        this.manager = manager;
    }

    @Override
    public Optional<String> readText() throws ClipboardAccessException {
        try {
            if (!manager.areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
                return Optional.empty();
            }
            String s = (String) manager.getContents(DataFlavor.stringFlavor);
            return Optional.ofNullable(s);
        } catch (IllegalStateException e) { // clipboard busy/locked
            throw new ClipboardAccessException("Clipboard is currently unavailable (locked).", e);
        } catch (UnsupportedFlavorException | IOException e) {
            throw new ClipboardAccessException("Clipboard does not contain readable text.", e);
        } catch (Throwable t) {
            throw new ClipboardAccessException("Unexpected clipboard error while reading.", t);
        }
    }

    @Override
    public void writeText(String text) throws ClipboardAccessException {
        try {
            StringSelection selection = new StringSelection(text == null ? "" : text);
            manager.setContents(selection);
        } catch (IllegalStateException e) { // clipboard busy/locked
            throw new ClipboardAccessException("Clipboard is currently unavailable (locked).", e);
        } catch (Throwable t) {
            throw new ClipboardAccessException("Unexpected clipboard error while writing.", t);
        }
    }
}
