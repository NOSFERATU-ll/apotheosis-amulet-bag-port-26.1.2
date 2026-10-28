# In-game acceptance checks

Use Minecraft 26.1.2, Java 25, NeoForge 26.1.2.76+, Apotheosis 9.1.x and its dependencies. Install the built jar on both sides. Test once with Curios 15.0.0+26.1.2 and once without Curios.

These are manual checks, not claims of completed gameplay testing:

- Verify the recipe, creative-tab item, bag texture and locked-slot texture.
- Right-click a new bag; all five slots should be locked. Check English/Russian names and tooltips.
- In survival, try unlocking with too few levels; then pay 100, 200, 300, 400 and 500 levels. Verify each cost and the maximum of five slots. Check creative unlocks are free.
- Insert and shift-click real potion charms. Reject unrelated items and locked slots. Verify normal removal, number-key swaps and drag operations.
- While the bag is open, attempt to move/drop the owning bag and swap it with a hotbar/offhand item. It should stay in place.
- Verify effects and durability with the menu both open and closed. Close/reopen, relog and restart the server; contents, unlocks and durability should persist.
- Test the main inventory, offhand and Curios charm slot. Remove the bag and wait for its effect to expire. With multiple bags, only the first eligible nonempty bag should tick.
- On a dedicated server with two players, verify each player's contents are isolated and both players see correct updates. Test death/dropping according to the server's normal inventory rules.
- Test with Apotheosis's `charmsInCuriosOnly` configuration and decide whether the bag's existing behavior (ticking nested charms with a null equipment slot) matches the modpack's intended rules.
