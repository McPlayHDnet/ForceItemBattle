package forceitembattle.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import forceitembattle.util.Text;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerTextures;

@Getter
public class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder addItemFlags(ItemFlag... itemFlags) {
        this.itemStack.editMeta(meta -> meta.addItemFlags(itemFlags));
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        this.itemStack.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemBuilder setGlowing() {
        this.itemStack.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        return addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    public ItemBuilder setGlowing(boolean state) {
        return state ? this.setGlowing() : this;
    }

    public ItemBuilder setLore(List<String> loreLines) {
        if (loreLines != null) {
            this.itemStack.editMeta(meta -> meta.lore(loreLines.stream().map(ItemBuilder::upright).toList()));
        }
        return this;
    }

    public ItemBuilder setLoreLegacy(List<String> loreLines) {
        List<String> lines = loreLines == null ? List.of() : loreLines;
        this.itemStack.editMeta(meta -> meta.lore(
                lines.stream().map(LegacyComponentSerializer.legacyAmpersand()::deserialize).toList()));
        return this;
    }

    public ItemBuilder setDisplayName(String displayName) {
        if (displayName != null) {
            this.itemStack.editMeta(meta -> meta.displayName(upright(displayName)));
        }
        return this;
    }

    public ItemBuilder setDisplayNameLegacy(String displayName) {
        this.itemStack.editMeta(meta -> meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)));
        return this;
    }

    public ItemBuilder setSkullTexture(PlayerTextures playerTextures) {
        PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
        playerProfile.setTextures(playerTextures);
        this.itemStack.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(playerProfile));
        return this;
    }

    public ItemBuilder setSkullTexture(String skinValue) {
        if (skinValue != null) {
            PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
            playerProfile.setProperty(new ProfileProperty("textures", skinValue));
            this.itemStack.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(playerProfile));
        }
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder setItemName(String itemName) {
        if (itemName != null) {
            this.itemStack.editMeta(meta -> meta.itemName(upright(itemName)));
        }
        return this;
    }

    public ItemBuilder setCustomModelDataStrings(List<String> strings) {
        this.itemStack.editMeta(meta -> {
            var component = meta.getCustomModelDataComponent();
            component.setStrings(strings);
            meta.setCustomModelDataComponent(component);
        });
        return this;
    }

    public ItemBuilder setItemModel(NamespacedKey itemModel) {
        this.itemStack.editMeta(meta -> meta.setItemModel(itemModel));
        return this;
    }

    public <P, C> ItemBuilder setPersistentData(NamespacedKey key, PersistentDataType<P, C> type, C value) {
        this.itemStack.editMeta(meta -> meta.getPersistentDataContainer().set(key, type, value));
        return this;
    }

    private static Component upright(String miniMessage) {
        return Text.of(miniMessage).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
