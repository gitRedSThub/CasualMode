package me.redst.casualMode.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public final class CasualPlayerList {

    private final List<Entry> entries = new ArrayList<>();
    private volatile Set<UUID> casualAccounts = Set.of();
    private volatile List<String> names = List.of();

    public boolean load(List<String> listedNames, Map<UUID, String> knownLinks) {
        Map<String, UUID> linkByName = new HashMap<>();
        knownLinks.forEach((account, name) -> linkByName.putIfAbsent(PlayerNames.key(name), account));

        entries.clear();
        Set<String> seen = new HashSet<>();
        for (String name : listedNames) {
            if (seen.add(PlayerNames.key(name))) {
                entries.add(new Entry(name, linkByName.get(PlayerNames.key(name))));
            }
        }
        publish();
        return !links().equals(knownLinks);
    }

    public boolean isCasual(UUID accountId) {
        return casualAccounts.contains(accountId);
    }

    public List<String> names() {
        return names;
    }

    public int size() {
        return names.size();
    }

    public Map<UUID, String> links() {
        Map<UUID, String> links = new LinkedHashMap<>();
        for (Entry entry : entries) {
            if (entry.accountId != null) {
                links.put(entry.accountId, entry.name);
            }
        }
        return links;
    }

    public JoinResult join(UUID accountId, String currentName) {
        Snapshot before = snapshot();
        Entry own = findByAccount(accountId);
        if (own != null) {
            if (own.name.equals(currentName)) {
                return new JoinResult(JoinStatus.LISTED, null, null, Changes.NONE);
            }
            Entry sameName = findByName(currentName);
            if (sameName != null && sameName != own) {
                if (sameName.accountId != null) {
                    return new JoinResult(JoinStatus.LISTED, null, null, Changes.NONE);
                }
                entries.remove(sameName);
            }
            String previousName = own.name;
            own.name = currentName;
            return new JoinResult(JoinStatus.LISTED, previousName, null, changesSince(before));
        }

        Entry listed = findByName(currentName);
        if (listed == null) {
            return new JoinResult(JoinStatus.NOT_LISTED, null, null, Changes.NONE);
        }
        if (listed.accountId != null) {
            return new JoinResult(JoinStatus.NAME_LINKED_TO_OTHER_ACCOUNT, null, listed.name, Changes.NONE);
        }
        String previousName = listed.name.equals(currentName) ? null : listed.name;
        listed.accountId = accountId;
        listed.name = currentName;
        return new JoinResult(JoinStatus.LISTED, previousName, null, changesSince(before));
    }

    public AddResult add(String name, @Nullable UUID onlineAccountId) {
        Snapshot before = snapshot();
        Entry listed = findByName(name);

        if (onlineAccountId == null) {
            if (listed != null) {
                return new AddResult(AddStatus.ALREADY_LISTED, listed.name, Changes.NONE);
            }
            entries.add(new Entry(name, null));
            return new AddResult(AddStatus.ADDED, name, changesSince(before));
        }

        Entry own = findByAccount(onlineAccountId);
        AddStatus status;
        if (listed == null && own == null) {
            entries.add(new Entry(name, onlineAccountId));
            status = AddStatus.ADDED;
        } else if (listed == null || listed == own) {
            own.name = name;
            status = AddStatus.ALREADY_LISTED;
        } else {
            if (own != null) {
                entries.remove(own);
            }
            status = listed.accountId == null ? AddStatus.ALREADY_LISTED : AddStatus.RELINKED;
            listed.accountId = onlineAccountId;
            listed.name = name;
        }
        return new AddResult(status, name, changesSince(before));
    }

    public @Nullable RemovedEntry remove(String name) {
        Entry listed = findByName(name);
        if (listed == null) {
            return null;
        }
        entries.remove(listed);
        publish();
        return new RemovedEntry(listed.name, listed.accountId);
    }

    private @Nullable Entry findByName(String name) {
        String key = PlayerNames.key(name);
        for (Entry entry : entries) {
            if (PlayerNames.key(entry.name).equals(key)) {
                return entry;
            }
        }
        return null;
    }

    private @Nullable Entry findByAccount(UUID accountId) {
        for (Entry entry : entries) {
            if (accountId.equals(entry.accountId)) {
                return entry;
            }
        }
        return null;
    }

    private Snapshot snapshot() {
        return new Snapshot(names, links());
    }

    private Changes changesSince(Snapshot before) {
        publish();
        return new Changes(!before.names.equals(names), !before.links.equals(links()));
    }

    private void publish() {
        Set<UUID> accounts = new HashSet<>();
        List<String> listedNames = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            listedNames.add(entry.name);
            if (entry.accountId != null) {
                accounts.add(entry.accountId);
            }
        }
        casualAccounts = Collections.unmodifiableSet(accounts);
        names = Collections.unmodifiableList(listedNames);
    }

    private static final class Entry {
        private String name;
        private @Nullable UUID accountId;

        private Entry(String name, @Nullable UUID accountId) {
            this.name = Objects.requireNonNull(name);
            this.accountId = accountId;
        }
    }

    private record Snapshot(List<String> names, Map<UUID, String> links) {
    }

    public record Changes(boolean names, boolean links) {
        public static final Changes NONE = new Changes(false, false);
    }

    public enum JoinStatus {
        NOT_LISTED,
        LISTED,
        NAME_LINKED_TO_OTHER_ACCOUNT
    }

    public record JoinResult(JoinStatus status, @Nullable String previousName, @Nullable String conflictingEntry,
                             Changes changes) {
    }

    public enum AddStatus {
        ADDED,
        ALREADY_LISTED,
        RELINKED
    }

    public record AddResult(AddStatus status, String name, Changes changes) {
    }

    public record RemovedEntry(String name, @Nullable UUID accountId) {
    }
}
