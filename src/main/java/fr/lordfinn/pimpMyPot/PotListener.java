package fr.lordfinn.pimpMyPot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.DecoratedPot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;

import java.util.Map;

public class PotListener implements Listener {

    @EventHandler
    public void onPotInteract(PlayerInteractEvent event) {
        // only right-click block, main hand
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Block block = event.getClickedBlock();
        BlockFace clickedFace = event.getBlockFace();

        if (block == null || item == null) return;
        if (!item.getType().name().endsWith("_POTTERY_SHERD")) return;
        if (!(block.getState() instanceof DecoratedPot pot)) return;
        // ignore clicks on top/bottom
        if (clickedFace == BlockFace.UP || clickedFace == BlockFace.DOWN) return;
        // Cancel vanilla interaction (prevents consuming the sherd)
        event.setCancelled(true);

        // Clone original item to prevent consumption on some servers
        ItemStack originalClone = item.clone();

        // IMPORTANT FIX:
        // Decorated pot's FRONT is opposite of its block facing!
        BlockFace potFacing = BlockFace.NORTH;
        if (block.getBlockData() instanceof Directional directional) {
            potFacing = directional.getFacing().getOppositeFace(); // <-- FIXED
        }

        // Map world face to pot side
        DecoratedPot.Side side = worldFaceToPotSide(clickedFace, potFacing);
        if (side == null) return;

        Material previousSherd = pot.getSherd(side);

        // Apply the sherd only on clicked side
        pot.setSherd(side, item.getType());
        pot.update(true);

        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            item.setAmount(item.getAmount() - 1);
            if (previousSherd != null && previousSherd != Material.AIR) {
                ItemStack refund = new ItemStack(previousSherd, 1);

                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(refund);

                if (!leftovers.isEmpty()) {
                    leftovers.values().forEach(stack ->
                            player.getWorld().dropItemNaturally(player.getLocation(), stack)
                    );
                }
            }
        }

        // Restore original item (prevent consumption)
        player.getInventory().setItemInMainHand(originalClone);

        String sherdName = item.getType().name()
                .replace("_POTTERY_SHERD", "")
                .replace("_", " ")
                .toLowerCase();

        String sideName = side.name().toLowerCase();

        player.sendActionBar(
                Component.text("Applied ")
                        .color(NamedTextColor.GRAY)
                        .append(Component.text(sherdName, NamedTextColor.GOLD))
                        .append(Component.text(" to ", NamedTextColor.GRAY))
                        .append(Component.text(sideName, NamedTextColor.DARK_GRAY))
                .append(Component.text(" side.", NamedTextColor.GRAY))
        );
    }

    /**
     * Converts clicked face → pot side depending on pot orientation.
     */
    private DecoratedPot.Side worldFaceToPotSide(BlockFace clicked, BlockFace potFacing) {
        int clickedIdx = faceIndex(clicked);
        int potIdx = faceIndex(potFacing);
        if (clickedIdx == -1 || potIdx == -1) return null;

        int delta = (clickedIdx - potIdx + 4) % 4;
        return switch (delta) {
            case 0 -> DecoratedPot.Side.FRONT;
            case 1 -> DecoratedPot.Side.LEFT;
            case 2 -> DecoratedPot.Side.BACK;
            case 3 -> DecoratedPot.Side.RIGHT;
            default -> null;
        };
    }

    private int faceIndex(BlockFace face) {
        return switch (face) {
            case NORTH -> 0;
            case EAST  -> 1;
            case SOUTH -> 2;
            case WEST  -> 3;
            default -> -1;
        };
    }
}
