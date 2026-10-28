package nosferatu.apothbag.smoke;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nosferatu.apothbag.BagEvents;
import nosferatu.apothbag.CuriosCompat;
import top.theillusivec4.curios.api.CuriosApi;

final class CuriosSmokeTest {
    static void check(ServerPlayer player, ItemStack bag) {
        player.getInventory().clearContent();
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        // Fake players never pass through the normal player-login initialization.
        inventory.loadDatapacks();
        var stacks = inventory.getStacksHandler("charm").orElseThrow().getStacks();
        stacks.setStackInSlot(0, bag);
        if (CuriosCompat.findFirstActiveBag(player) != bag) {
            throw new AssertionError("equipped Curios bag was not found");
        }
        player.removeEffect(MobEffects.NIGHT_VISION);
        BagEvents.onPlayerTick(new PlayerTickEvent.Post(player));
        if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
            throw new AssertionError("Curios bag did not apply its charm effect");
        }
        stacks.setStackInSlot(0, ItemStack.EMPTY);
        System.out.println("AMULET BAG CURIOS CHECK PASSED");
    }
}
