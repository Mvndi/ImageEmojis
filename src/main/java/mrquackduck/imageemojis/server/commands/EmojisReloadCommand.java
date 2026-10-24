package mrquackduck.imageemojis.server.commands;

import mrquackduck.imageemojis.ImageEmojisPlugin;
import mrquackduck.imageemojis.configuration.Configuration;
import mrquackduck.imageemojis.configuration.Permissions;
import mrquackduck.imageemojis.types.enums.SuggestionMode;
import mrquackduck.imageemojis.types.models.EmojiModel;
import mrquackduck.imageemojis.utils.SuggestionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Level;

public class EmojisReloadCommand implements CommandExecutor {
    private static final String RELOADED_MESSAGE = "reloaded";
    private static final String RELOAD_ERROR_MESSAGE = "an-error-occurred";

    private final ImageEmojisPlugin plugin;
    private final Configuration config;

    public EmojisReloadCommand(ImageEmojisPlugin plugin) {
        this.plugin = plugin;
        this.config = new Configuration(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> reload(commandSender));
        return true;
    }

    private void reload(CommandSender sender) {
        try {
            SuggestionMode oldMode = config.suggestionMode();
            List<EmojiModel> oldEmojis = plugin.getEmojiRepository().getEmojis();
            plugin.reload();
            updateSuggestions(oldEmojis, oldMode);
            sendReply(sender, config.getMessage(RELOADED_MESSAGE));
        } catch (Exception exception) {
            sendReply(sender, config.getMessage(RELOAD_ERROR_MESSAGE));
            plugin.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
        }
    }

    private void updateSuggestions(List<EmojiModel> oldEmojis, SuggestionMode oldMode) {
        SuggestionMode newMode = config.suggestionMode();
        List<EmojiModel> newEmojis = plugin.getEmojiRepository().getEmojis();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.getScheduler().execute(plugin, () -> {
                SuggestionManager.removeSuggestions(player, oldEmojis, oldMode);
                if (player.hasPermission(Permissions.USE)) {
                    SuggestionManager.addSuggestions(player, newEmojis, newMode);
                }
            }, null, 1L);
        }
    }

    private void sendReply(CommandSender sender, String message) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.getScheduler().execute(plugin, () -> player.sendMessage(message), null, 1L);
            return;
        }
        sender.sendMessage(message);
    }
}
