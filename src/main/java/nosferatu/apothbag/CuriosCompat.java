package nosferatu.apothbag;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Optional Curios integration kept behind reflection so Curios is never required at runtime.
 */
public final class CuriosCompat {
    private static boolean initialized = false;
    private static boolean available = false;
    private static Method getCuriosInventoryMethod;

    private CuriosCompat() {
    }

    public static ItemStack findFirstActiveBag(Player player) {
        if (!init()) {
            return ItemStack.EMPTY;
        }

        try {
            Object inventoryResult = getCuriosInventoryMethod.invoke(null, player);
            Object handler = unwrapOptionalLike(inventoryResult);
            if (handler == null) {
                return ItemStack.EMPTY;
            }

            Object curiosValue = handler.getClass().getMethod("getCurios").invoke(handler);
            if (!(curiosValue instanceof Map<?, ?> curios)) {
                return ItemStack.EMPTY;
            }

            // The bag is assigned to Curios' preset "charm" slot by datapack.
            ItemStack charmBag = findBagInStacksHandler(curios.get("charm"));
            if (!charmBag.isEmpty()) {
                return charmBag;
            }

            // Be tolerant of user datapacks/configs that move the bag to another Curios slot.
            for (Object stacksHandler : curios.values()) {
                ItemStack bag = findBagInStacksHandler(stacksHandler);
                if (!bag.isEmpty()) {
                    return bag;
                }
            }
        }
        catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack findBagInStacksHandler(Object stacksHandler) throws Exception {
        if (stacksHandler == null) {
            return ItemStack.EMPTY;
        }

        Object stacks = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
        if (stacks == null) {
            return ItemStack.EMPTY;
        }

        int slots = ((Number) stacks.getClass().getMethod("getSlots").invoke(stacks)).intValue();
        Method getStackInSlot = stacks.getClass().getMethod("getStackInSlot", int.class);

        for (int i = 0; i < slots; i++) {
            Object value = getStackInSlot.invoke(stacks, i);
            if (value instanceof ItemStack stack
                    && stack.getItem() instanceof TalismanBagItem
                    && TalismanBagItem.hasStoredItemsFast(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean init() {
        if (initialized) {
            return available;
        }

        initialized = true;
        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            getCuriosInventoryMethod = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
            available = true;
        }
        catch (Throwable ignored) {
            available = false;
        }
        return available;
    }

    private static Object unwrapOptionalLike(Object value) throws Exception {
        if (value == null) {
            return null;
        }

        // Curios 1.21.x returns java.util.Optional directly.
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }

        // Compatibility fallback for older Curios LazyOptional-style return values.
        Object resolved = value.getClass().getMethod("resolve").invoke(value);
        if (resolved instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return null;
    }
}
