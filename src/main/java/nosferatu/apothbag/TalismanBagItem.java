package nosferatu.apothbag;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.item.PotionCharmItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

public class TalismanBagItem extends Item {
    public static final int MAX_SLOTS = 5;
    private static final String TAG_UNLOCKED = "UnlockedSlots";
    private static final String TAG_ITEMS = "Items";

    public TalismanBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        updateAppearance(stack);
    }

    public static void updateAppearance(ItemStack bag) {
        int count = Math.min(3, getBagData(bag).getListOrEmpty(TAG_ITEMS).size());
        Identifier model = Identifier.fromNamespaceAndPath(ApothTalismanBag.MODID,
                count == 0 ? "talisman_bag" : "talisman_bag_" + count);
        if (!model.equals(bag.get(DataComponents.ITEM_MODEL))) {
            bag.set(DataComponents.ITEM_MODEL, model);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                unlockNextSlot(stack, serverPlayer);
            }
            else {
                openBag(serverPlayer, stack);
            }
        }

        return InteractionResult.SUCCESS;
    }

    public static void openBag(ServerPlayer player, ItemStack bagStack) {
        Component title = Component.translatable("container.apoth_talisman_bag.talisman_bag");
        RegistryAccess registries = player.level().registryAccess();
        MenuProvider provider = new SimpleMenuProvider((int id, Inventory inventory, Player menuPlayer) -> {
            TalismanBagContainer container = new TalismanBagContainer(bagStack, registries);
            return new TalismanBagMenu(id, inventory, container);
        }, title);
        player.openMenu(provider);
    }

    public static boolean unlockNextSlot(ItemStack stack, ServerPlayer player) {
        int unlocked = getUnlockedSlots(stack);
        if (unlocked >= MAX_SLOTS) {
            player.sendSystemMessage(Component.translatable("message.apoth_talisman_bag.all_slots_unlocked"), true);
            return false;
        }

        int cost = (unlocked + 1) * 100;
        if (!player.getAbilities().instabuild && player.experienceLevel < cost) {
            player.sendSystemMessage(Component.translatable("message.apoth_talisman_bag.not_enough_levels", cost), true);
            return false;
        }

        if (!player.getAbilities().instabuild) {
            player.giveExperienceLevels(-cost);
        }

        setUnlockedSlots(stack, unlocked + 1);
        player.sendSystemMessage(Component.translatable("message.apoth_talisman_bag.slot_unlocked", unlocked + 1, cost), true);
        return true;
    }

    private static CompoundTag getBagData(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag();
    }

    private static void setBagData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getUnlockedSlots(ItemStack stack) {
        CompoundTag tag = getBagData(stack);
        int slots = tag.getIntOr(TAG_UNLOCKED, 0);
        if (slots < 0) return 0;
        return Math.min(slots, MAX_SLOTS);
    }

    public static void setUnlockedSlots(ItemStack stack, int slots) {
        CompoundTag tag = getBagData(stack);
        tag.putInt(TAG_UNLOCKED, Math.max(0, Math.min(slots, MAX_SLOTS)));
        setBagData(stack, tag);
    }

    public static boolean hasStoredItemsFast(ItemStack stack) {
        if (stack.isEmpty()) return false;

        CompoundTag tag = getBagData(stack);
        int unlocked = tag.getIntOr(TAG_UNLOCKED, 0);
        if (unlocked <= 0) return false;

        return !tag.getListOrEmpty(TAG_ITEMS).isEmpty();
    }

    public static boolean isPotionCharm(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PotionCharmItem && PotionCharmItem.hasEffect(stack);
    }

    public static void enableCharm(ItemStack stack) {
        if (isPotionCharm(stack)) {
            stack.set(Apoth.Components.CHARM_ENABLED, true);
        }
    }

    public static ItemStack createLockedVisual() {
        return new ItemStack((ItemLike) ApothTalismanBag.LOCKED_SLOT.get());
    }

    public static boolean isLockedVisual(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ApothTalismanBag.LOCKED_SLOT.get();
    }

    public static void loadItems(ItemStack bag, TalismanBagContainer container, HolderLookup.Provider registries) {
        CompoundTag tag = getBagData(bag);
        ListTag list = tag.getListOrEmpty(TAG_ITEMS);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            int slot = entry.getByteOr("Slot", (byte) -1) & 255;
            if (slot < MAX_SLOTS && entry.contains("Item")) {
                ItemStack loaded = ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), entry.getCompoundOrEmpty("Item")).getOrThrow();
                container.setItemSilently(slot, loaded);
            }
        }
    }

    public static void saveItems(ItemStack bag, TalismanBagContainer container, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        int unlocked = getUnlockedSlots(bag);
        for (int slot = 0; slot < Math.min(unlocked, MAX_SLOTS); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && !isLockedVisual(stack)) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) slot);
                entry.put("Item", ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow());
                list.add(entry);
            }
        }
        CompoundTag tag = getBagData(bag);
        tag.put(TAG_ITEMS, list);
        setBagData(bag, tag);
        updateAppearance(bag);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        addTooltip(stack, tooltip);
    }

    private static void addTooltip(ItemStack stack, Consumer<Component> tooltip) {
        int unlocked = getUnlockedSlots(stack);
        tooltip.accept(Component.translatable("tooltip.apoth_talisman_bag.unlocked", unlocked, MAX_SLOTS));
        if (unlocked < MAX_SLOTS) {
            int cost = (unlocked + 1) * 100;
            tooltip.accept(Component.translatable("tooltip.apoth_talisman_bag.next_cost", cost));
        }
        tooltip.accept(Component.translatable("tooltip.apoth_talisman_bag.open"));
        tooltip.accept(Component.translatable("tooltip.apoth_talisman_bag.unlock"));
    }

}
