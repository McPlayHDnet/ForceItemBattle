package forceitembattle.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ItemBuilder {

    private transient ItemStack itemStack;
    private int slot;

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }


    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder setDamage(int damage) {
        if (!(this.itemStack.getItemMeta() instanceof Damageable))
            return this;
        ((Damageable)this.itemStack.getItemMeta()).setDamage(damage);
        return this;
    }

    public ItemBuilder addItemFlag(ItemFlag itemFlag) {
        return addItemFlags(Collections.singletonList(itemFlag));
    }

    public ItemBuilder addItemFlags(List<ItemFlag> itemFlags) {
        return addItemFlags(itemFlags.toArray(new ItemFlag[0]));
    }

    public ItemBuilder removeItemFlag(ItemFlag itemFlag) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        itemMeta.removeItemFlags(itemFlag);
        this.itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setSkullMeta(SkullMeta skullMeta) {
        this.itemStack.setItemMeta((ItemMeta)skullMeta);
        return this;
    }

    public ItemBuilder setMaxDurability(short durability) {
        this.itemStack.setDurability(durability);
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag[] itemFlags) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        itemMeta.addItemFlags(itemFlags);
        this.itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        this.itemStack.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemBuilder addEnchantments(HashMap<Enchantment, Integer> enchantmentLevelsMap, boolean ignoreLevelRestriction) {
        enchantmentLevelsMap.forEach((enchantment, level) -> this.itemStack.getItemMeta().addEnchant(enchantment, level.intValue(), ignoreLevelRestriction));
        return this;
    }

    public ItemBuilder setGlowing() {
        Enchantment enchantment;
        if (this.itemStack.getType() == Material.BOW) {
            enchantment = Enchantment.LURE;
        } else {
            enchantment = Enchantment.ARROW_INFINITE;
        }
        addEnchantment(enchantment, 0);
        addItemFlag(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder setGlowing(boolean state) {
        if(state) this.setGlowing();
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        this.itemStack.getItemMeta().setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder addLore(String... lore) {
        addLore(List.of(lore));
        return this;
    }

    public ItemBuilder addLore(List<String> loreLines) {
        ItemMeta itemMeta = this.itemStack.getItemMeta();
        List<String> lore = itemMeta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        for (String line : loreLines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        itemMeta.setLore(lore);
        setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        setLore(List.of(lore));
        return this;
    }

    public ItemBuilder setLore(List<String> loreLines) {
        ItemMeta itemMeta = getItemStack().getItemMeta();
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        itemMeta.setLore(lore);
        setItemMeta(itemMeta);
        return this;
    }
    public ItemBuilder setDisplayName(String displayName) {
        ItemMeta itemMeta = getItemStack().getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setItemMeta(ItemMeta itemMeta) {
        this.itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setMaterial(Material material) {
        this.itemStack.setType(material);
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

}
