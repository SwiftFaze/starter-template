package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.quests.Quest;
import javax.swing.JComponent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Exposes every mod-loaded quest as its own searchable dev-console entry,
 * with objective and reward data shown directly in the detail view via the
 * standard DetailTable rendering.
 */
public class QuestSandboxProvider implements DevConsoleProvider {

    private static final String CATEGORY = "Quests";
    private final QuestSandboxModel model;

    public QuestSandboxProvider() {
        this(Paths.get("mods"));
    }

    public QuestSandboxProvider(Path modsRoot) {
        this(new QuestSandboxModel(modsRoot));
    }

    public QuestSandboxProvider(List<Quest> quests) {
        this(new QuestSandboxModel(quests));
    }

    private QuestSandboxProvider(QuestSandboxModel model) {
        this.model = model;
    }

    @Override
    public List<DevConsoleEntry> entries() {
        return model.quests().stream()
                .map(quest -> new DevConsoleEntry(
                        namespaceOf(quest.getId()),
                        quest.getId(),
                        CATEGORY,
                        quest.getName()
                ))
                .toList();
    }

    @Override
    public JComponent createPanel(String id) {
        Quest quest = model.findById(id);
        return new InspectableDetailPanel(quest.getName(), quest);
    }

    private static String namespaceOf(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(0, colon) : id;
    }
}
