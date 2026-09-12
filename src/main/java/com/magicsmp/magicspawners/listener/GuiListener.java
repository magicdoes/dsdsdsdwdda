package com.magicsmp.magicspawners.listener;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.gui.SpawnerGui;
import com.magicsmp.magicspawners.gui.SpawnerPanelGui;
import com.magicsmp.magicspawners.model.SpawnerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GuiListener implements Listener {
    private final MagicSpawners plugin;

    public GuiListener(MagicSpawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        SpawnerGui.Session spawnerSession = SpawnerGui.OPEN.get(p.getUniqueId());
        if (spawnerSession != null) {
            // Prevent every action that can move/collect items from the protected top GUI.
            // Normal clicks inside the player's own inventory are still allowed.
            if (affectsProtectedTop(e)) {
                e.setCancelled(true);
            }

            if (e.getClickedInventory() == e.getView().getTopInventory()) {
                e.setCancelled(true);
                int slot = e.getRawSlot();
                SpawnerData d = spawnerSession.data();

                if (slot == 45) {
                    SpawnerGui.previousPage(p);
                } else if (slot == 53) {
                    SpawnerGui.nextPage(p);
                } else if (slot == 50) {
                    collect(p, d);
                    SpawnerGui.refresh(p);
                } else if (slot == 48) {
                    sell(p, d);
                    SpawnerGui.refresh(p);
                }
            }
            return;
        }

        SpawnerPanelGui.Session session = SpawnerPanelGui.OPEN.get(p.getUniqueId());
        if (session == null) return;

        if (affectsProtectedTop(e)) e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        e.setCancelled(true);
        int slot = e.getRawSlot();

        switch (session.view()) {
            case MAIN -> handleMainPanel(p, slot);
            case DIMENSION -> handleDimensionPanel(p, e, session, slot);
            case CONFIRM_REMOVE -> handleRemovalConfirm(p, session, slot);
        }
    }

    /**
     * Returns true for actions originating in the player inventory that can still
     * reach into the plugin's top inventory (shift-click, double-click collect, etc.).
     */
    private boolean affectsProtectedTop(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return false;
        if (e.getClickedInventory() == e.getView().getTopInventory()) return true;

        InventoryAction action = e.getAction();
        return e.isShiftClick()
                || e.getClick().isKeyboardClick()
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.HOTBAR_MOVE_AND_READD
                || action == InventoryAction.HOTBAR_SWAP;
    }

    private void handleMainPanel(Player p, int slot) {
        if (slot == 11) SpawnerPanelGui.openDimension(p, org.bukkit.World.Environment.NORMAL, 0);
        else if (slot == 13) SpawnerPanelGui.openDimension(p, org.bukkit.World.Environment.NETHER, 0);
        else if (slot == 15) SpawnerPanelGui.openDimension(p, org.bukkit.World.Environment.THE_END, 0);
    }

    private void handleDimensionPanel(Player p, InventoryClickEvent e, SpawnerPanelGui.Session session, int slot) {
        if (slot == 45) {
            if (session.page() > 0) SpawnerPanelGui.openDimension(p, session.environment(), session.page() - 1);
            return;
        }
        if (slot == 49) {
            SpawnerPanelGui.openMain(p);
            return;
        }
        if (slot == 53) {
            SpawnerPanelGui.openDimension(p, session.environment(), session.page() + 1);
            return;
        }

        UUID id = session.slotToSpawner().get(slot);
        if (id == null) return;
        SpawnerData d = SpawnerPanelGui.find(id);
        if (d == null) {
            SpawnerPanelGui.openDimension(p, session.environment(), session.page());
            return;
        }

        if (e.isRightClick()) {
            var loc = d.location();
            if (loc == null) {
                p.sendMessage("§cThat spawner's world is not loaded.");
                return;
            }
            p.closeInventory();
            loc.getChunk().load();
            p.teleport(loc.clone().add(0.5, 1.0, 0.5));
            p.sendMessage("§aTeleported to the §f" + plugin.manager().pretty(d.entityType()) + " Spawner§a.");
        } else if (e.isLeftClick()) {
            SpawnerPanelGui.openRemovalConfirm(p, session, d);
        }
    }

    private void handleRemovalConfirm(Player p, SpawnerPanelGui.Session session, int slot) {
        if (slot == 11) {
            SpawnerPanelGui.openDimension(p, session.environment(), session.page());
            return;
        }
        if (slot != 15) return;

        SpawnerData d = SpawnerPanelGui.find(session.pendingRemoval());
        if (d != null) {
            var loc = d.location();
            if (loc != null) {
                loc.getChunk().load();
                if (loc.getBlock().getType() == Material.SPAWNER) loc.getBlock().setType(Material.AIR, false);
            }
            plugin.storage().remove(d);
            plugin.storage().markDirty();
            p.sendMessage("§aSpawner removed from the panel and world.");
        } else {
            p.sendMessage("§cThat spawner no longer exists.");
        }
        SpawnerPanelGui.openDimension(p, session.environment(), session.page());
    }

    @EventHandler
    public void drag(InventoryDragEvent e) {
        UUID id = e.getWhoClicked().getUniqueId();
        if ((SpawnerGui.OPEN.containsKey(id) || SpawnerPanelGui.OPEN.containsKey(id))
                && e.getRawSlots().stream().anyMatch(s -> s < e.getView().getTopInventory().getSize())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            SpawnerGui.OPEN.remove(p.getUniqueId());
            SpawnerPanelGui.OPEN.remove(p.getUniqueId());
        }
    }

    private void collect(Player p, SpawnerData d) {
        if (d.storage().isEmpty()) {
            p.sendMessage(plugin.messages().get("no-items"));
            return;
        }
        int total = 0;
        Map<Material, Integer> copy = new HashMap<>(d.storage());
        d.storage().clear();
        for (var en : copy.entrySet()) {
            int left = en.getValue();
            total += left;
            while (left > 0) {
                int n = Math.min(left, en.getKey().getMaxStackSize());
                Map<Integer, ItemStack> rest = p.getInventory().addItem(new ItemStack(en.getKey(), n));
                for (ItemStack r : rest.values()) p.getWorld().dropItemNaturally(p.getLocation(), r);
                left -= n;
            }
        }
        plugin.storage().markDirty();
        p.sendMessage(plugin.messages().get("collected-items", Map.of("count", String.valueOf(total))));
    }

    private void sell(Player p, SpawnerData d) {
        if (d.storage().isEmpty()) {
            p.sendMessage(plugin.messages().get("no-items"));
            return;
        }
        if (!plugin.economy().available()) {
            p.sendMessage("§cVault economy is not available.");
            return;
        }
        double total = 0;
        for (var en : d.storage().entrySet()) {
            total += plugin.entities().price(d.entityType(), en.getKey()) * en.getValue();
        }
        if (total <= 0) {
            p.sendMessage("§cThese drops do not have sell prices configured.");
            return;
        }
        d.storage().clear();
        plugin.storage().markDirty();
        plugin.economy().deposit(p, total);
        p.sendMessage(plugin.messages().get("sold-items", Map.of("price", String.format("%.2f", total))));
    }
}
