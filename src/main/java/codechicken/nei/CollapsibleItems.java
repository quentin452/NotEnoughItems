package codechicken.nei;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import codechicken.nei.ItemList.AnyMultiItemFilter;
import codechicken.nei.ItemList.EverythingItemFilter;
import codechicken.nei.ItemList.NothingItemFilter;
import codechicken.nei.PresetsList.Preset;
import codechicken.nei.PresetsList.PresetMode;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.util.ItemStackFilterParser;

public class CollapsibleItems {

    protected static class GroupItem {

        public String guid;
        public ItemFilter filter;
        public boolean expanded = false;
        public String displayName = "";

        public void setFilter(String filter) {
            this.filter = ItemStackFilterParser.parse(filter.trim());
            this.guid = this.filter != null ? UUID.nameUUIDFromBytes(filter.getBytes()).toString() : "";
        }

        public void setFilter(ItemFilter filter, String guid) {
            this.filter = filter;
            this.guid = guid;
        }

        public boolean matches(ItemStack stack) {
            return this.filter.matches(stack);
        }
    }

    private static final String STATE_KEY = "collapsibleitems";

    protected File statesFile;
    protected final List<GroupItem> groups = new ArrayList<>();
    protected final Map<ItemStack, Integer> cache = new ConcurrentHashMap<>();

    public void load() {
        try {

            if (NEIClientConfig.world.nbt.hasKey(STATE_KEY)) {
                NBTTagCompound states = NEIClientConfig.world.nbt.getCompoundTag(STATE_KEY);
                @SuppressWarnings("unchecked")
                final Map<String, NBTPrimitive> list = (Map<String, NBTPrimitive>) states.tagMap;
                final Map<String, GroupItem> mapping = new HashMap<>();

                for (GroupItem group : this.groups) {
                    mapping.put(group.guid, group);
                }

                for (Map.Entry<String, NBTPrimitive> nbtEntry : list.entrySet()) {
                    if (mapping.containsKey(nbtEntry.getKey())) {
                        mapping.get(nbtEntry.getKey()).expanded = nbtEntry.getValue().func_150290_f() == 1;
                    }
                }
            }

        } catch (Exception e) {
            NEIClientConfig.logger.error("Error loading collapsible items states", e);
        }

        reloadGroups();
    }

    public void reloadGroups() {
        this.groups.clear();
        this.cache.clear();

        for (int i = PresetsList.presets.size() - 1; i >= 0; i--) {
            Preset preset = PresetsList.presets.get(i);
            if (preset.enabled && preset.mode == PresetMode.GROUP) {
                GroupItem group = new GroupItem();
                group.setFilter(preset, UUID.nameUUIDFromBytes(preset.items.toString().getBytes()).toString());
                group.displayName = preset.name;
                addGroup(group);
            }
        }

        if (NEIClientConfig.enableCollapsibleItems()) {
            ClientHandler.loadSettingsFile(
                    "collapsibleitems.cfg",
                    lines -> parseFile(lines.collect(Collectors.toCollection(ArrayList::new))));
        }
    }

    private void parseFile(List<String> itemStrings) {
        final JsonParser parser = new JsonParser();
        GroupItem group = new GroupItem();

        for (String itemStr : itemStrings) {
            try {

                if (itemStr.startsWith("; ")) {
                    JsonObject settings = parser.parse(itemStr.substring(2)).getAsJsonObject();

                    if (settings.get("displayName") != null) {
                        group.displayName = settings.get("displayName").getAsString();
                    }

                    if (settings.get("unlocalizedName") != null) {
                        String unlocalizedName = settings.get("unlocalizedName").getAsString();
                        String displayName = StatCollector.translateToLocal(unlocalizedName);

                        if (!displayName.equals(unlocalizedName)) {
                            group.displayName = displayName;
                        }
                    }

                    if (settings.get("expanded") != null) {
                        group.expanded = settings.get("expanded").getAsBoolean();
                    }

                } else {
                    group.setFilter(itemStr);
                }

                if (group != null && group.filter != null) {
                    addGroup(group);
                    group = new GroupItem();
                }

            } catch (IllegalArgumentException | JsonSyntaxException | IllegalStateException e) {
                NEIClientConfig.logger.error("Failed to load collapsible items from json string:\n{}", itemStr);
            }
        }
    }

    protected void addGroup(GroupItem group) {
        if (group == null || group.filter == null
                || group.filter instanceof EverythingItemFilter
                || group.filter instanceof NothingItemFilter)
            return;
        this.groups.add(group);
    }

    public boolean isEmpty() {
        return this.groups.isEmpty();
    }

    public ItemFilter getItemFilter() {
        AnyMultiItemFilter filter = new AnyMultiItemFilter();

        for (GroupItem group : this.groups) {
            filter.filters.add(group.filter);
        }

        return filter;
    }

    public void updateCache(final List<ItemStack> items) {
        this.cache.clear();

        try {

            ItemList.forkJoinPool.submit(() -> items.parallelStream().forEach(stack -> {
                GroupItem group = this.groups.stream().filter(g -> g.matches(stack)).findFirst().orElse(null);

                if (group != null) {
                    this.cache.put(stack, this.groups.indexOf(group));
                }
            })).get();

        } catch (Exception e) {
            NEIClientConfig.logger.error("Error create collapsible items groups", e);
        }

    }

    public int getGroupIndex(ItemStack stack) {

        if (stack == null) {
            return -1;
        }

        return this.cache.getOrDefault(stack, -1);
    }

    public String getDisplayName(int groupIndex) {

        if (groupIndex < this.groups.size()) {
            return this.groups.get(groupIndex).displayName;
        }

        return null;
    }

    public boolean isExpanded(int groupIndex) {

        if (groupIndex < this.groups.size()) {
            return this.groups.get(groupIndex).expanded;
        }

        return true;
    }

    public void setExpanded(int groupIndex, boolean expanded) {

        if (groupIndex < this.groups.size()) {
            this.groups.get(groupIndex).expanded = expanded;
            saveStates();
        }
    }

    public void toggleGroups(Boolean expanded) {

        if (expanded == null) {
            expanded = this.groups.stream().noneMatch(g -> g.expanded);
        }

        for (GroupItem group : this.groups) {
            group.expanded = expanded;
        }

        saveStates();
    }

    private void saveStates() {
        NBTTagCompound list = new NBTTagCompound();

        for (GroupItem group : this.groups) {
            list.setBoolean(group.guid, group.expanded);
        }

        NEIClientConfig.world.nbt.setTag(STATE_KEY, list);
    }

}
