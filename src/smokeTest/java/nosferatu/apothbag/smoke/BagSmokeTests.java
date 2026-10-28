package nosferatu.apothbag.smoke;

import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nosferatu.apothbag.*;

import java.nio.file.Files;
import java.nio.file.Path;

/** Runs against real mod registries and a real server world; never included in the release jar. */
@Mod("apoth_bag_smoke_tests")
public final class BagSmokeTests {
    public BagSmokeTests() {
        NeoForge.EVENT_BUS.addListener(this::run);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void run(ServerStartedEvent event) {
        var server = event.getServer();
        Path result = Path.of(System.getProperty("apothbag.smokeResult"));
        try {
            Files.deleteIfExists(result);
            var level = server.overworld();
            var registries = level.registryAccess();
            var player = FakePlayerFactory.getMinecraft(level);
            player.getInventory().clearContent();
            ItemStack bag = new ItemStack(ApothTalismanBag.TALISMAN_BAG.get());
            player.getInventory().setItem(0, bag);
            check(TalismanBagItem.getUnlockedSlots(bag) == 0, "new bag must have no unlocked slots");
            player.getAbilities().instabuild = false;
            player.experienceLevel = 99;
            check(!TalismanBagItem.unlockNextSlot(bag, player), "insufficient XP must reject unlock");
            player.experienceLevel = 100;
            check(TalismanBagItem.unlockNextSlot(bag, player), "100 levels must unlock the first slot");
            check(player.experienceLevel == 0, "unlock must consume XP levels");

            ItemStack charm = PotionContents.createItemStack(Apoth.Items.POTION_CHARM.value(), Potions.LONG_NIGHT_VISION);
            check(TalismanBagItem.isPotionCharm(charm), "real Apotheosis charm must be accepted");
            var container = new TalismanBagContainer(bag, registries);
            check(!container.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "ordinary items must be rejected");
            check(!container.canPlaceItem(1, charm), "locked slot must reject charms");
            container.setItem(0, charm);
            check(charm.getOrDefault(Apoth.Components.CHARM_ENABLED, false), "stored charm must be enabled");

            // Exercise full ItemStack serialization, as used on disk and across a restart.
            var ops = registries.createSerializationContext(NbtOps.INSTANCE);
            ItemStack restored = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, bag).getOrThrow()).getOrThrow();
            var loaded = new TalismanBagContainer(restored, registries);
            check(TalismanBagItem.getUnlockedSlots(restored) == 1, "unlocks must survive serialization");
            check(ItemStack.matches(loaded.getItem(0), charm), "charm components must survive serialization");

            // The open menu and the tick handler must share the same live inventory.
            var menu = new TalismanBagMenu(1, player.getInventory(), container);
            player.containerMenu = menu;
            player.removeEffect(MobEffects.NIGHT_VISION);
            BagEvents.onPlayerTick(new PlayerTickEvent.Post(player));
            check(player.hasEffect(MobEffects.NIGHT_VISION), "bag must apply the actual charm effect");
            check(container.getItem(0).getDamageValue() > 0, "active charm must consume durability");
            int damage = container.getItem(0).getDamageValue();
            menu.removed(player);
            check(new TalismanBagContainer(bag, registries).getItem(0).getDamageValue() == damage,
                    "closing the menu must not restore spent durability");
            player.containerMenu = player.inventoryMenu;

            player.getAbilities().instabuild = true;
            for (int i = 1; i < 5; i++) check(TalismanBagItem.unlockNextSlot(bag, player), "creative unlock failed");
            check(!TalismanBagItem.unlockNextSlot(bag, player), "sixth slot must not unlock");
            var full = new TalismanBagContainer(bag, registries);
            for (int i = 0; i < 5; i++) full.setItem(i, charm.copy());
            var allLoaded = new TalismanBagContainer(bag, registries);
            for (int i = 0; i < 5; i++) check(TalismanBagItem.isPotionCharm(allLoaded.getItem(i)), "five-slot roundtrip failed");
            allLoaded.removeItem(4, 1);
            check(new TalismanBagContainer(bag, registries).getItem(4).isEmpty(), "removed charm must stay removed");

            if (ModList.get().isLoaded("curios")) CuriosSmokeTest.check(player, bag);

            Files.createDirectories(result.getParent());
            Files.writeString(result, "PASS: XP unlocks, slot restrictions, five-slot persistence, charm effect, durability, open-menu persistence, removal.\n");
            System.out.println("AMULET BAG SMOKE TESTS PASSED");
        } catch (Throwable failure) {
            failure.printStackTrace();
            throw new IllegalStateException("Amulet Bag smoke tests failed", failure);
        } finally {
            server.halt(false);
        }
    }
}
