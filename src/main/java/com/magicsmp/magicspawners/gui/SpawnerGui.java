package com.magicsmp.magicspawners.gui;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.model.SpawnerData;
import com.magicsmp.magicspawners.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Digital spawner storage GUI.
 *
 * Slots 0-44 are loot, 45/53 are page controls and the middle bottom
 * controls are Sell All / info / Collect Loot.
 */
public final class SpawnerGui {
    public record Session(SpawnerData data, int page) {}

    public static final Map<UUID, Session> OPEN = new HashMap<>();
    private static final int PAGE_SIZE = 45;

    private SpawnerGui() {}

    public static void open(Player p, SpawnerData d) {
        open(p, d, 0);
    }

    public static void open(Player p, SpawnerData d, int requestedPage) {
        int maxPage = maxPage(d);
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        Inventory inv = build(d, page);
        p.openInventory(inv);
        OPEN.put(p.getUniqueId(), new Session(d, page));
    }

    public static void refresh(Player p) {
        Session session = OPEN.get(p.getUniqueId());
        if (session != null) open(p, session.data(), session.page());
    }

    public static void previousPage(Player p) {
        Session session = OPEN.get(p.getUniqueId());
        if (session != null && session.page() > 0) {
            open(p, session.data(), session.page() - 1);
        }
    }

    public static void nextPage(Player p) {
        Session session = OPEN.get(p.getUniqueId());
        if (session == null) return;
        int maxPage = maxPage(session.data());
        if (session.page() < maxPage) {
            open(p, session.data(), session.page() + 1);
        }
    }

    private static Inventory build(SpawnerData d, int page) {
        MagicSpawners pl = MagicSpawners.get();
        String mob = pl.manager().pretty(d.entityType());
        int maxPage = maxPage(d);
        page = Math.max(0, Math.min(page, maxPage));

        String title = d.stackSize() + " " + mob + " ѕᴘᴀᴡɴᴇʀ &8(" + (page + 1) + "/" + (maxPage + 1) + ")";
        Inventory inv = Bukkit.createInventory(null, 54, ColorUtil.component(title));

        List<ItemStack> stacks = lootStacks(d);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, stacks.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, stacks.get(i));
        }

        // Keep the complete bottom row protected and visually stable.
        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < 54; slot++) inv.setItem(slot, filler);

        String prevLore = page > 0 ? "&fClick to go to the previous page" : "&7No previous page";
        String nextLore = page < maxPage ? "&fClick to go to the next page" : "&7No next page";

        inv.setItem(45, button(Material.ARROW, "&#1FFD98← ᴘʀᴇᴠɪᴏᴜѕ ᴘᴀɢᴇ", prevLore));
        inv.setItem(48, button(Material.GOLD_INGOT, "&#FF1D1Dѕᴇʟʟ ᴀʟʟ", "&fClick to sell all mob drops!"));
        inv.setItem(49, button(Material.SPAWNER, "&#1FFD98" + mob + " ѕᴘᴀᴡɴᴇʀ",
                "&#1FFD98" + d.totalItems() + " &fitems stored",
                "&7Stack: &fx" + d.stackSize(),
                "&7Page: &f" + (page + 1) + "/" + (maxPage + 1)));
        inv.setItem(50, button(Material.DROPPER, "&#1FFD98ᴄᴏʟʟᴇᴄᴛ ʟᴏᴏᴛ", "&fClick to collect all loot"));
        inv.setItem(53, button(Material.ARROW, "&#1FFD98ɴᴇxᴛ ᴘᴀɢᴇ →", nextLore));
        return inv;
    }

    private static int maxPage(SpawnerData d) {
        int stackCount = lootStackCount(d);
        return Math.max(0, (stackCount - 1) / PAGE_SIZE);
    }

    private static int lootStackCount(SpawnerData d) {
        int count = 0;
        for (var e : d.storage().entrySet()) {
            int amount = Math.max(0, e.getValue());
            int max = Math.max(1, e.getKey().getMaxStackSize());
            count += (amount + max - 1) / max;
        }
        return count;
    }

    private static List<ItemStack> lootStacks(SpawnerData d) {
        List<ItemStack> stacks = new ArrayList<>();
        for (var e : d.storage().entrySet()) {
            int left = Math.max(0, e.getValue());
            int max = Math.max(1, e.getKey().getMaxStackSize());
            while (left > 0) {
                int amount = Math.min(left, max);
                stacks.add(new ItemStack(e.getKey(), amount));
                left -= amount;
            }
        }
        return stacks;
    }

    public static ItemStack button(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        meta.displayName(ColorUtil.component(name));
        meta.lore(Arrays.stream(lore).map(ColorUtil::component).toList());
        i.setItemMeta(meta);
        return i;
    }
}
