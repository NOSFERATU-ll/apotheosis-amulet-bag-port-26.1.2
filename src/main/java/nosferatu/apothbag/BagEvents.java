package nosferatu.apothbag;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class BagEvents {

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof TalismanBagItem && TalismanBagItem.hasStoredItemsFast(stack)) {
                tickBag(player, stack);
                return;
            }
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof TalismanBagItem && TalismanBagItem.hasStoredItemsFast(offhand)) {
            tickBag(player, offhand);
            return;
        }

        ItemStack curiosBag = CuriosCompat.findFirstActiveBag(player);
        if (!curiosBag.isEmpty()) {
            tickBag(player, curiosBag);
        }
    }

    private static void tickBag(ServerPlayer player, ItemStack bag) {
        if (!TalismanBagItem.hasStoredItemsFast(bag)) {
            return;
        }

        RegistryAccess registries = player.level().registryAccess();
        TalismanBagContainer container = player.containerMenu instanceof TalismanBagMenu menu
                && menu.getBagContainer().getBagStack() == bag
                ? menu.getBagContainer() : new TalismanBagContainer(bag, registries);
        int unlocked = TalismanBagItem.getUnlockedSlots(bag);
        boolean dirty = false;

        for (int i = 0; i < Math.min(unlocked, TalismanBagItem.MAX_SLOTS); i++) {
            ItemStack charm = container.getItem(i);
            if (TalismanBagItem.isPotionCharm(charm)) {
                TalismanBagItem.enableCharm(charm);
                charm.getItem().inventoryTick(charm, player.level(), player, null);
                dirty = true;
            }
        }

        if (dirty) {
            container.setChanged();
        }
    }
}
