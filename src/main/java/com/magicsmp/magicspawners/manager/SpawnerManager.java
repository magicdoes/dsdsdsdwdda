package com.magicsmp.magicspawners.manager;

import com.magicsmp.magicspawners.MagicSpawners;
import com.magicsmp.magicspawners.model.SpawnerData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.UUID;

public final class SpawnerManager {
    private final MagicSpawners plugin;
    private final NamespacedKey typeKey, amountKey;
    public SpawnerManager(MagicSpawners plugin) {
        this.plugin=plugin; typeKey=new NamespacedKey(plugin,"entity_type"); amountKey=new NamespacedKey(plugin,"stack_amount");
    }
    public ItemStack createItem(EntityType type,int amount){
        ItemStack item=new ItemStack(Material.SPAWNER); ItemMeta meta=item.getItemMeta();
        meta.displayName(com.magicsmp.magicspawners.util.ColorUtil.component("&#1FFD98"+pretty(type)+" Spawner &7x"+amount));
        meta.getPersistentDataContainer().set(typeKey,PersistentDataType.STRING,type.name());
        meta.getPersistentDataContainer().set(amountKey,PersistentDataType.INTEGER,Math.max(1,amount)); item.setItemMeta(meta); return item;
    }
    public EntityType itemType(ItemStack item){
        if(item==null||item.getType()!=Material.SPAWNER||!item.hasItemMeta())return null;
        String v=item.getItemMeta().getPersistentDataContainer().get(typeKey,PersistentDataType.STRING); if(v==null)return null;
        try{return EntityType.valueOf(v);}catch(Exception e){return null;}
    }
    public int itemAmount(ItemStack item){if(item==null||!item.hasItemMeta())return 1;Integer i=item.getItemMeta().getPersistentDataContainer().get(amountKey,PersistentDataType.INTEGER);return i==null?1:Math.max(1,i);}
    public SpawnerData register(org.bukkit.Location loc,EntityType type,int amount){
        SpawnerData d=new SpawnerData(UUID.randomUUID(),loc,type,amount); plugin.storage().put(d);
        if(loc.getBlock().getState() instanceof CreatureSpawner cs){cs.setSpawnedType(type);cs.update(true,false);} return d;
    }
    public String pretty(EntityType type){String s=type.name().toLowerCase(Locale.ROOT).replace('_',' ');StringBuilder b=new StringBuilder();for(String p:s.split(" "))b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');return b.toString().trim();}
}
