package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.exceptions.ModLoadException;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Test double: an in-memory provider whose entries can change and whose reload() can be made to fail. */
public class ReloadableFakeProvider implements DevConsoleProvider {
    private final List<DevConsoleEntry> entries = new ArrayList<>();
    private int reloadCount;
    private String nextReloadError = "";

    public ReloadableFakeProvider(String category, String... names) {
        for (String name : names) {
            entries.add(new DevConsoleEntry("core", "core:" + name.toLowerCase(Locale.ROOT), category, name));
        }
    }

    @Override
    public List<DevConsoleEntry> entries() {
        return new ArrayList<>(entries);
    }

    @Override
    public JComponent createPanel(String id) {
        return new JPanel();
    }

    @Override
    public void reload() {
        if (!nextReloadError.isEmpty()) {
            String error = nextReloadError;
            nextReloadError = "";
            throw new ModLoadException(error);
        }
        reloadCount++;
    }

    public void addEntry(DevConsoleEntry entry) {
        entries.add(entry);
    }

    public void removeEntryById(String id) {
        entries.removeIf(e -> e.id().equals(id));
    }

    public void clearEntries() {
        entries.clear();
    }

    public void setNextReloadFails(String errorMessage) {
        this.nextReloadError = errorMessage;
    }

    public int reloadCallCount() {
        return reloadCount;
    }
}
