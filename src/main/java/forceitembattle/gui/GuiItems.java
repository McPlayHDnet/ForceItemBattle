package forceitembattle.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public final class GuiItems {

    private GuiItems() {
    }

    private static final String PREVIOUS_ACTIVE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==";
    private static final String PREVIOUS_DISABLED =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZkYWI3MjcxZjRmZjA0ZDU0NDAyMTkwNjdhMTA5YjVjMGMxZDFlMDFlYzYwMmMwMDIwNDc2ZjdlYjYxMjE4MCJ9fX0=";
    private static final String NEXT_ACTIVE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19";
    private static final String NEXT_DISABLED =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFhMTg3ZmVkZTg4ZGUwMDJjYmQ5MzA1NzVlYjdiYTQ4ZDNiMWEwNmQ5NjFiZGM1MzU4MDA3NTBhZjc2NDkyNiJ9fX0=";
    private static final String BACK =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmViNTg4YjIxYTZmOThhZDFmZjRlMDg1YzU1MmRjYjA1MGVmYzljYWI0MjdmNDYwNDhmMThmYzgwMzQ3NWY3In19fQ==";

    /** Neutral filler for the body of a menu. */
    public static ItemStack filler() {
        return blankPane(Material.GRAY_STAINED_GLASS_PANE);
    }

    /** Border for settings-style menus. */
    public static ItemStack border() {
        return blankPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
    }

    /** Border for the vault and achievement menus. */
    public static ItemStack accentBorder() {
        return blankPane(Material.CYAN_STAINED_GLASS_PANE);
    }

    public static ItemStack nextPage() {
        return next("Next Page");
    }

    public static ItemStack previousPage() {
        return previous("Previous Page");
    }

    /** Forward button, e.g. {@code next("Next Recipe")}. */
    public static ItemStack next(String label) {
        return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .setDisplayName("<dark_green>» <green>" + label)
                .addItemFlags(ItemFlag.values())
                .getItemStack();
    }

    /** Back button, e.g. {@code previous("Previous Recipe")}. */
    public static ItemStack previous(String label) {
        return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setDisplayName("<dark_red>« <red>" + label)
                .addItemFlags(ItemFlag.values())
                .getItemStack();
    }

    /**
     * Page arrows as player heads, greyed when the move isn't available.
     *
     * The four textures are the ones the achievement menu already uses, lifted here so every paged
     * menu draws its arrows from one place rather than each carrying its own copy. AchievementInventory
     * still holds its own set and can be migrated onto these separately -- it works as-is, so there is
     * no reason to touch it in the same change.
     *
     * Note which is which: PREVIOUS_DISABLED and NEXT_ACTIVE are easy to mix up, and the collection
     * dex did exactly that -- it drew the greyed previous arrow on every page, on every page number.
     */
    /** "Back" head that returns to the menu above this one — the same icon in every menu that has one. */
    public static ItemStack back() {
        return pageHead(BACK, "<dark_red>« <red>Back", null);
    }

    public static ItemStack pageBack(boolean enabled) {
        return pageHead(enabled ? PREVIOUS_ACTIVE : PREVIOUS_DISABLED,
                enabled ? "<dark_red>« <red>Previous page" : "<dark_gray>« <gray>Previous page",
                enabled ? null : "<dark_gray>» <gray>You're on the first page");
    }

    public static ItemStack pageForward(boolean enabled) {
        return pageHead(enabled ? NEXT_ACTIVE : NEXT_DISABLED,
                enabled ? "<dark_green>» <green>Next page" : "<dark_gray>» <gray>Next page",
                enabled ? null : "<dark_gray>» <gray>You're on the last page");
    }

    private static ItemStack pageHead(String texture, String displayName, String reason) {
        ItemBuilder itemBuilder = new ItemBuilder(Material.PLAYER_HEAD)
                .setSkullTexture(texture)
                .setDisplayName(displayName);
        if (reason != null) {
            itemBuilder.setLore(java.util.List.of("", reason));
        }
        return itemBuilder.getItemStack();
    }

    public static ItemStack noItemsFound() {
        return new ItemBuilder(Material.BARRIER)
                .setDisplayName("<red>No Items found")
                .getItemStack();
    }

    private static ItemStack blankPane(Material material) {
        return new ItemBuilder(material)
                .setDisplayName("")
                .addItemFlags(ItemFlag.values())
                .getItemStack();
    }
}
