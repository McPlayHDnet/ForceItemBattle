package forceitembattle.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import forceitembattle.ForceItemBattle;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class ItemBuilder {

    private final ItemStack itemStack;

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

    public ItemBuilder setGlowing() {
        this.itemStack.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        addItemFlag(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder setGlowing(boolean state) {
        if(state) this.setGlowing();
        return this;
    }

    public ItemBuilder setLore(List<String> loreLines) {
        if(loreLines == null) return this;
        ItemMeta itemMeta = getItemStack().getItemMeta();
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        itemMeta.lore(lore);
        setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setLoreLegacy(List<String> loreLines) {
        if (loreLines == null) {
            loreLines = new ArrayList<>();
        }

        ItemMeta itemMeta = getItemStack().getItemMeta();
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(line.replaceAll("&", "§"));
        }

        itemMeta.setLore(lore);
        setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setCustomModelData(int customModelData) {
        ItemMeta itemMeta = getItemStack().getItemMeta();
        itemMeta.setCustomModelData(customModelData);
        setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setDisplayName(String displayName) {
        if (displayName == null) return this;
        ItemMeta itemMeta = getItemStack().getItemMeta();
        itemMeta.displayName(ForceItemBattle.getInstance().getGamemanager().getMiniMessage().deserialize(displayName).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setDisplayNameLegacy(String displayName) {
        ItemMeta itemMeta = getItemStack().getItemMeta();
        itemMeta.setDisplayName(displayName.replaceAll("&", "§"));
        setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setSkullTexture(PlayerTextures playerTextures) {
        SkullMeta skullMeta = (SkullMeta)this.itemStack.getItemMeta();
        PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
        playerProfile.setTextures(playerTextures);
        skullMeta.setPlayerProfile(playerProfile);
        setSkullMeta(skullMeta);
        return this;
    }

    public ItemBuilder setSkullTexture(String skinValue) {
        if(skinValue != null) {
            SkullMeta skullMeta = (SkullMeta)this.itemStack.getItemMeta();
            PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
            playerProfile.setProperty(new ProfileProperty("textures", skinValue));
            skullMeta.setPlayerProfile(playerProfile);
            setSkullMeta(skullMeta);
        }
        return this;
    }

    public ItemBuilder setItemMeta(ItemMeta itemMeta) {
        this.itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder setSkullMeta(SkullMeta skullMeta) {
        this.itemStack.setItemMeta((ItemMeta)skullMeta);
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
}
