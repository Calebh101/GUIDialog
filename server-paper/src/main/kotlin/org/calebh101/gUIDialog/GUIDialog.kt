package org.calebh101.gUIDialog

import com.google.common.io.ByteStreams
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.*


class GUIDialog : JavaPlugin(), Listener, PluginMessageListener {
    val actionsStore = ActionStore(this)
    val sessions: MutableMap<Long, UUID> = mutableMapOf()
    val dialogs = DialogStore(this)

    override fun onEnable() {
        logger.info("Loading GUIDialog...")
        server.pluginManager.registerEvents(this, this)

        server.messenger.registerOutgoingPluginChannel(this, "guidialog:dialog")
        server.messenger.registerOutgoingPluginChannel(this, "guidialog:something")

        server.messenger.registerIncomingPluginChannel(this, "guidialog:action", this)
        server.messenger.registerIncomingPluginChannel(this, "guidialog:register", this)

        this.lifecycleManager.registerEventHandler<ReloadableRegistrarEvent<Commands>>(
            LifecycleEvents.COMMANDS,
            LifecycleEventHandler { commands: ReloadableRegistrarEvent<Commands> ->
                logger.info("Registering commands...")

                val root = Commands.literal("guidialog")
                    .executes { context ->
                        val sender = context.source.sender
                        val version = description.version

                        sender.sendMessage(
                            Component.text()
                                .append(Component.text("GUIDialog By Calebh101").decorate(TextDecoration.BOLD))
                                .append(Component.newline())
                                .append(Component.text("Repo: "))
                                .append(Component.text("https://github.com/Calebh101/GUIDialog")
                                    .color(NamedTextColor.BLUE)
                                    .decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.openUrl("https://github.com/Calebh101/GUIDialog")))
                                .build()
                        );

                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        Commands.literal("dialogs")
                            .requires { sender -> sender.sender.hasPermission("guidialog.dialogs") }
                            .then(Commands.literal("list")
                                .executes { context ->
                                    val sender = context.source.sender
                                    val data = dialogs.load()

                                    sender.sendMessage(Component.text("All ${data.size} dialogs(s):\n${data.entries.map {
                                        "- ${it.key}: ${it.value.build()}"
                                    }.joinToString("\n")}"))

                                    Command.SINGLE_SUCCESS
                                }
                            )
                            .then(Commands.literal("get")
                                .then(Commands.argument("id", StringArgumentType.word())
                                    .executes { context ->
                                        val sender = context.source.sender
                                        val id = StringArgumentType.getString(context, "id")

                                        if (!checkId(id)) {
                                            sender.sendMessage(Component.text("ID may only contain letters, numbers, or underscores.", NamedTextColor.RED))
                                            return@executes 0
                                        }

                                        val dialog = dialogs.get(id)

                                        if (dialog == null) {
                                            sender.sendMessage(Component.text("Dialog not found: $id", NamedTextColor.RED))
                                            return@executes 0
                                        }

                                        sender.sendMessage(Component.text("Dialog $id: /${dialog.build()}"))
                                        Command.SINGLE_SUCCESS
                                    }
                                )
                            )
                            .then(Commands.literal("set")
                                .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("payload", StringArgumentType.greedyString())
                                    .executes { context ->
                                        val sender = context.source.sender
                                        val id = StringArgumentType.getString(context, "id")
                                        val payload = StringArgumentType.getString(context, "payload")

                                        if (!checkId(id)) {
                                            sender.sendMessage(Component.text("ID may only contain letters, numbers, or underscores.", NamedTextColor.RED))
                                            return@executes 0
                                        }

                                        try {
                                            val payload = Gson().fromJson(payload, DialogPayload::class.java)

                                            if (payload == null) {
                                                sender.sendMessage(Component.text("Payload parsed to null.", NamedTextColor.RED))
                                                return@executes Command.SINGLE_SUCCESS
                                            }

                                            payload.toDialog()
                                            dialogs.save(mapOf(id to payload))
                                            sender.sendMessage(Component.text("Saved dialog $id!", NamedTextColor.GREEN))
                                        } catch (e: JsonSyntaxException) {
                                            sender.sendMessage(Component.text("Invalid JSON: ${e.message}", NamedTextColor.RED))
                                            return@executes Command.SINGLE_SUCCESS
                                        } catch (e: IllegalStateException) {
                                            sender.sendMessage(Component.text("Invalid payload: ${e.message}", NamedTextColor.RED))
                                            return@executes Command.SINGLE_SUCCESS
                                        }

                                        Command.SINGLE_SUCCESS
                                    }
                                ))
                            )
                            .then(Commands.literal("delete")
                                .then(Commands.argument("id", StringArgumentType.word())
                                    .executes { context ->
                                        val sender = context.source.sender
                                        val id = StringArgumentType.getString(context, "id")

                                        if (!checkId(id)) {
                                            sender.sendMessage(Component.text("ID may only contain letters, numbers, or underscores.", NamedTextColor.RED))
                                            return@executes 0
                                        }

                                        val dialog = dialogs.get(id)

                                        if (dialog == null) {
                                            sender.sendMessage(Component.text("Dialog not found: $id", NamedTextColor.RED))
                                            return@executes 0
                                        }

                                        dialogs.delete(id)
                                        sender.sendMessage(Component.text("Deleted dialog $id.", NamedTextColor.GREEN))
                                        Command.SINGLE_SUCCESS
                                    }
                                )
                            )
                            .then(Commands.literal("input")
                                .executes { context ->
                                    val sender = context.source.sender

                                    if (sender !is Player) {
                                        sender.sendMessage(Component.text("This command must be run from a player.", NamedTextColor.RED))
                                        return@executes 0
                                    }

                                    val bytes = "dialog_input".toByteArray()
                                    sender.sendPluginMessage(this, "guidialog:something", bytes)
                                    Command.SINGLE_SUCCESS
                                }
                            )
                    )
                    .then(
                        Commands.literal("actions")
                            .requires { sender -> sender.sender.hasPermission("guidialog.actions") }
                            .then(Commands.literal("list")
                                .executes { context ->
                                    val sender = context.source.sender
                                    val data = actionsStore.load()

                                    sender.sendMessage(Component.text("All ${data.size} action(s):\n${data.entries.map {
                                        "- ${it.key}: /${it.value}"
                                    }.joinToString("\n")}"))

                                    Command.SINGLE_SUCCESS
                                }
                            )
                            .then(Commands.literal("get")
                                .then(Commands.argument("id", StringArgumentType.word())
                                    .executes { context ->
                                        val sender = context.source.sender
                                        val id = StringArgumentType.getString(context, "id")

                                        if (!checkId(id)) {
                                            sender.sendMessage(Component.text("ID may only contain letters, numbers, or underscores.", NamedTextColor.RED))
                                            return@executes 0
                                        }

                                        val command = actionsStore.load()[id]

                                        if (command == null) {
                                            sender.sendMessage(Component.text("Action not found: $id", NamedTextColor.RED))
                                            return@executes 0
                                        }

                                        sender.sendMessage(Component.text("Action $id: /$command"))
                                        Command.SINGLE_SUCCESS
                                    }
                                ))
                            .then(Commands.literal("set")
                                .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes { context ->
                                    val sender = context.source.sender
                                    val id = StringArgumentType.getString(context, "id")

                                    if (!checkId(id)) {
                                        sender.sendMessage(Component.text("ID may only contain letters, numbers, or underscores.", NamedTextColor.RED))
                                        return@executes 0
                                    }

                                    var command = StringArgumentType.getString(context, "command")
                                    if (command.startsWith("/")) command = command.replaceFirst("/", "")

                                    logger.info("Registered action $id for user ${sender.name}: $command")
                                    actionsStore.save(mapOf(id to command))
                                    sender.sendMessage(Component.text("Set action $id to command: /$command", NamedTextColor.GREEN))
                                    Command.SINGLE_SUCCESS
                                }
                            )))
                            .then(Commands.literal("run")
                                .then(Commands.argument("target", ArgumentTypes.player())
                                .then(Commands.argument("id", StringArgumentType.word())
                                .executes { context ->
                                    val sender = context.source.sender
                                    val resolver = context.getArgument("target", PlayerSelectorArgumentResolver::class.java)
                                    var target = resolver.resolve(context.source).firstOrNull()
                                    val id = StringArgumentType.getString(context, "id")

                                    if (target == null && sender is Player) {
                                        target = sender
                                    }

                                    if (target == null) {
                                        sender.sendMessage(Component.text("No player found.", NamedTextColor.RED))
                                        return@executes 0
                                    }

                                    if (!id.matches(Regex("[A-Za-z0-9_]+"))) {
                                        sender.sendMessage(Component.text("ID may only contain letters, numbers, or underscores.", NamedTextColor.RED))
                                        return@executes 0
                                    }

                                    var command = actionsStore.load()[id]

                                    if (command == null) {
                                        sender.sendMessage(Component.text("Action not found: $id", NamedTextColor.RED))
                                        return@executes 0
                                    }

                                    command = processCommand(command = command, player = target)
                                    sender.sendMessage(Component.text("Running action $id...", NamedTextColor.GREEN))
                                    logger.info("Running action $id for player ${target.name} and dialog $id: $command")
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                                    Command.SINGLE_SUCCESS
                                }
                            )))
                    )
                    .then(
                        Commands.literal("send")
                        .requires { sender -> sender.sender.hasPermission("guidialog.send") }
                        .then(Commands.argument("player", ArgumentTypes.players())
                        .then(Commands.argument("payload", StringArgumentType.greedyString())
                        .executes { context ->
                            val sender = context.source.sender
                            val payload = StringArgumentType.getString(context, "payload")
                            val resolver = context.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                            val targets: List<Player> = resolver.resolve(context.source)

                            if (targets.isEmpty()) {
                                sender.sendMessage(Component.text("No matching player found.", NamedTextColor.RED))
                                return@executes 0
                            }

                            try {
                                val payload = dialogs.get(payload) ?: Gson().fromJson(payload, DialogPayload::class.java)

                                if (payload == null) {
                                    sender.sendMessage(Component.text("Payload parsed to null.", NamedTextColor.RED))
                                    return@executes Command.SINGLE_SUCCESS
                                }

                                for (target in targets) {
                                    val dialog = payload.toDialog()
                                    sessions[dialog.id] = target.uniqueId
                                    sendPayload(target, dialog)
                                }

                                sender.sendMessage(Component.text("Sent payload to user(s) ${targets.map { it.name }.joinToString(", ")}.", NamedTextColor.GREEN))
                            } catch (e: JsonSyntaxException) {
                                sender.sendMessage(Component.text("Invalid JSON: ${e.message}", NamedTextColor.RED))
                                return@executes Command.SINGLE_SUCCESS
                            } catch (e: IllegalStateException) {
                                sender.sendMessage(Component.text("Invalid payload: ${e.message}", NamedTextColor.RED))
                                return@executes Command.SINGLE_SUCCESS
                            }

                            Command.SINGLE_SUCCESS
                        }
                    )))
                    .then(
                        Commands.literal("sessions")
                            .requires { sender -> sender.sender.hasPermission("guidialog.sessions") }
                            .executes { context ->
                                val sender = context.source.sender

                                sender.sendMessage(Component.text("All ${sessions.size} sessions:\n${
                                    sessions.map {
                                        "- ${it.key}: ${it.value} (${Bukkit.getOfflinePlayer(it.value).name})"
                                    }.joinToString("\n")
                                }"))

                                Command.SINGLE_SUCCESS
                            }
                    )

                commands.registrar().register(root.build(), "GUIDialog commands")
                logger.info("Registered commands: ${root.build().children.joinToString { it.name }}")
            })
    }

    fun processCommand(command: String, player: Player): String {
        return command
    }

    fun sendPayload(player: Player, dialog: Dialog) {
        val bytes = dialog.build().toByteArray(Charsets.UTF_8)
        player.sendPluginMessage(this, "guidialog:dialog", bytes)
    }

    fun checkId(id: String): Boolean {
        return id.matches(Regex("[A-Za-z0-9_]+"))
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        try {
            if (channel == "guidialog:action") {
                val data = ByteStreams.newDataInput(message)
                val id = data.readLong()
                val action = data.readUTF()

                if (sessions[id] != player.uniqueId) {
                    player.sendMessage(Component.text("Invalid session ID.", NamedTextColor.RED))
                    return
                }

                if (!action.matches(Regex("[A-Za-z0-9_]+"))) {
                    player.sendMessage(
                        Component.text(
                            "ID may only contain letters, numbers, or underscores: $action",
                            NamedTextColor.RED
                        )
                    )
                    return
                }

                if (action != "close") {
                    var command = actionsStore.get(action)

                    if (command == null) {
                        player.sendMessage(Component.text("Dialog action not found: $action", NamedTextColor.RED))
                        return
                    }

                    command = processCommand(command = command, player = player)
                    val wrapped = "execute as ${player.uniqueId} at @s run $command"

                    logger.info("Running action $action for player ${player.name} and dialog $id: $wrapped")
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), wrapped)
                }

                sessions.remove(id)
            } else if (channel == "guidialog:register") {
                if (!player.hasPermission("guidialog.dialogs")) {
                    player.sendMessage(Component.text("You don't have permission to do this!", NamedTextColor.RED))
                    return
                }

                val data = ByteStreams.newDataInput(message).readUTF().split(" ")
                val id = data.first()
                val payload = data.subList(1, data.size).joinToString(" ")

                try {
                    val payload = Gson().fromJson(payload, DialogPayload::class.java)

                    if (payload == null) {
                        player.sendMessage(Component.text("Payload parsed to null.", NamedTextColor.RED))
                        return
                    }

                    payload.toDialog()
                    dialogs.save(mapOf(id to payload))
                    player.sendMessage(Component.text("Saved dialog $id!", NamedTextColor.GREEN))
                } catch (e: JsonSyntaxException) {
                    player.sendMessage(Component.text("Invalid JSON: ${e.message}", NamedTextColor.RED))
                    return
                } catch (e: IllegalStateException) {
                    player.sendMessage(Component.text("Invalid payload: ${e.message}", NamedTextColor.RED))
                    return
                }
            }
        } catch (e: Throwable) {
            logger.warning("Unhandled error when processing message on channel $channel from player ${player.name}: $e")
            player.sendMessage(Component.text("An unhandled error occurred.", NamedTextColor.RED))
        }
    }
}