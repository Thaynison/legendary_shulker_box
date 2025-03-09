package www.legendarycommunity.com.br.legendary_shulker_box;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Legendary_shulker_box extends JavaPlugin implements Listener {

    private NamespacedKey shulkerKey;
    private static final int SHULKER_SIZE = 54; // Define o tamanho do inventário para 54 slots
    private final Map<Inventory, String> openShulkerInventories = new HashMap<>(); // Rastreia inventários abertos

    @Override
    public void onEnable() {
        shulkerKey = new NamespacedKey(this, "shulker_id");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Legendary Shulker Box habilitado com sucesso!");
    }


    @Override
    public void onDisable() {
        getLogger().info("Legendary Shulker Box desabilitado.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Verifica se o item na mão é uma Shulker Box
        if (itemInHand.getType().toString().endsWith("_SHULKER_BOX")) {
            event.setCancelled(true); // Cancela o comportamento padrão

            // Verifica se o item possui um estado de bloco (BlockStateMeta)
            if (itemInHand.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
                if (blockStateMeta.getBlockState() instanceof ShulkerBox) {
                    // Obtém ou cria um identificador único para a Shulker Box
                    String shulkerId = getOrCreateShulkerId(itemInHand);

                    // Carregar itens salvos
                    Inventory shulkerInventory = Bukkit.createInventory(player, SHULKER_SIZE, "Shulker Box");
                    File shulkerFile = getShulkerFile(player.getUniqueId(), shulkerId);
                    if (shulkerFile.exists()) {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(shulkerFile);
                        for (int i = 0; i < SHULKER_SIZE; i++) {
                            shulkerInventory.setItem(i, config.getItemStack("items." + i));
                        }
                    }

                    // Associa o inventário ao identificador da Shulker Box
                    openShulkerInventories.put(shulkerInventory, shulkerId);

                    // Abre o inventário para o jogador
                    player.openInventory(shulkerInventory);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        // Verifica se o inventário fechado é uma Shulker Box
        if (openShulkerInventories.containsKey(inventory)) {
            Player player = (Player) event.getPlayer();
            String shulkerId = openShulkerInventories.get(inventory);

            // Salvar os itens no arquivo YAML
            File shulkerFile = getShulkerFile(player.getUniqueId(), shulkerId);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(shulkerFile);

            for (int i = 0; i < SHULKER_SIZE; i++) {
                ItemStack item = inventory.getItem(i);
                config.set("items." + i, item);
            }

            try {
                config.save(shulkerFile);
            } catch (IOException e) {
                getLogger().severe("Erro ao salvar arquivo YAML para a Shulker Box " + shulkerId);
                e.printStackTrace();
            }

            // Remove o inventário da lista de abertos
            openShulkerInventories.remove(inventory);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Verifica se o inventário é uma Shulker Box
        if (openShulkerInventories.containsKey(inventory)) {

            // Impede que o jogador coloque uma Shulker Box dentro de outra
            if (currentItem != null && currentItem.getType().toString().endsWith("_SHULKER_BOX")) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não pode colocar uma Shulker Box dentro de outra!");
            }

            // Impede o arraste (drag) de itens da Shulker Box para fora dela
            // Detecta quando o jogador tenta arrastar (click + move de itens)
            if (event.getSlot() == -999 || event.getClick().isShiftClick() || cursorItem != null) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não pode mover itens para fora da Shulker Box!");
            }

            // Bloqueia o drop de itens diretamente da Shulker Box para o chão
            if (event.getClick().isRightClick() || event.getClick().isShiftClick() || event.getClick().isMouseClick()) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não pode dropar itens da Shulker Box!");
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        // Verifica se o item sendo droppado é uma Shulker Box ou está dentro de uma Shulker Box
        if (item != null && item.getType().toString().endsWith("_SHULKER_BOX")) {
            event.setCancelled(true); // Bloqueia o drop do item
            player.sendMessage("§cVocê não pode dropar itens da Shulker Box!");
        }
    }






    private String getOrCreateShulkerId(ItemStack shulkerBox) {
        ItemMeta meta = shulkerBox.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        // Verifica se já existe um identificador
        if (dataContainer.has(shulkerKey, PersistentDataType.STRING)) {
            return dataContainer.get(shulkerKey, PersistentDataType.STRING);
        }

        // Cria um novo identificador único
        String newId = UUID.randomUUID().toString();
        dataContainer.set(shulkerKey, PersistentDataType.STRING, newId);
        shulkerBox.setItemMeta(meta);

        return newId;
    }

    private File getShulkerFile(UUID playerUuid, String shulkerId) {
        File playerFolder = new File(getDataFolder(), "players/" + playerUuid);
        if (!playerFolder.exists()) {
            playerFolder.mkdirs();
        }
        return new File(playerFolder, shulkerId + ".yml");
    }
}
