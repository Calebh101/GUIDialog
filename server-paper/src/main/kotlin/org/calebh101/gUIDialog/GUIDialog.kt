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
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.*


class GUIDialog : JavaPlugin(), Listener, PluginMessageListener {
    val actionsStore = ActionStore(this)
    val sessions: MutableMap<Long, UUID> = mutableMapOf()

    override fun onEnable() {
        logger.info("Loading GUIDialog...")
        server.pluginManager.registerEvents(this, this)

        server.messenger.registerOutgoingPluginChannel(this, "guidialog:dialog")
        server.messenger.registerIncomingPluginChannel(this, "guidialog:action", this)

        this.lifecycleManager.registerEventHandler<ReloadableRegistrarEvent<Commands>>(
            LifecycleEvents.COMMANDS,
            LifecycleEventHandler { commands: ReloadableRegistrarEvent<Commands> ->
                logger.info("Registering commands...")

                /*val playerArg = Commands.argument("player", ArgumentTypes.player())
                playerArg.then(Commands.argument("payload", StringArgumentType.greedyString()))

                val idArg = Commands.argument("id", StringArgumentType.word())
                idArg.then(Commands.argument("command", StringArgumentType.greedyString()))

                val root = Commands.literal("guidialog")
                val actions = Commands.literal("actions")

                val send = Commands.literal("send").then(playerArg).requires { sender -> sender.sender.hasPermission("guidialog.send") }.executes { context ->
                    val sender = context.source.sender
                    val payload = StringArgumentType.getString(context, "payload")
                    val resolver = context.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                    val target: Player? = resolver.resolve(context.source).firstOrNull()

                    if (target == null) {
                        sender.sendMessage(Component.text("No matching player found.", NamedTextColor.RED))
                        return@executes 0
                    }

                    try {
                        val payload = Gson().fromJson(payload, DialogPayload::class.java)

                        if (payload == null) {
                            sender.sendMessage(Component.text("Payload parsed to null.", NamedTextColor.RED))
                            return@executes Command.SINGLE_SUCCESS
                        }

                        val dialog = payload.toDialog()
                        logger.info("Sending payload to user " + target.name + " from entity " + sender.name)
                        sessions[dialog.id] = target.uniqueId
                        sendPayload(target, dialog)
                    } catch (e: JsonSyntaxException) {
                        sender.sendMessage(Component.text("Invalid JSON: ${e.message}", NamedTextColor.RED))
                        return@executes Command.SINGLE_SUCCESS
                    } catch (e: IllegalStateException) {
                        sender.sendMessage(Component.text("Invalid payload: ${e.message}", NamedTextColor.RED))
                        return@executes Command.SINGLE_SUCCESS
                    }

                    Command.SINGLE_SUCCESS
                }

                val actionsAdd = Commands.literal("add").then(idArg).requires { sender -> sender.getSender().hasPermission("guidialog.actions") }.executes { context ->
                    val sender = context.source.sender
                    val id = StringArgumentType.getString(context, "id")

                    if (!id.matches(Regex("[A-Za-z0-9_]+"))) {
                        sender.sendMessage(Component.text("ID may only contain letters, numbers, or underscores.", NamedTextColor.RED))
                        return@executes 0
                    }

                    var command = StringArgumentType.getString(context, "command")
                    if (command.startsWith("/")) command = command.replaceFirst("/", "")

                    logger.info("Registered action $id for user ${sender.name}: $command")
                    actionsStore.save(mapOf(id to command))
                    sender.sendMessage(Component.text("Saved action: $id"))
                    Command.SINGLE_SUCCESS
                }

                val actionsGet = Commands.literal("get").then(Commands.argument("id", StringArgumentType.word())).requires { sender -> sender.getSender().hasPermission("guidialog.actions") }.executes { context ->
                    val sender = context.source.sender
                    val id = StringArgumentType.getString(context, "id")

                    if (!id.matches(Regex("[A-Za-z0-9_]+"))) {
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

                val actionsDelete = Commands.literal("delete").then(Commands.argument("id", StringArgumentType.word())).requires { sender -> sender.getSender().hasPermission("guidialog.actions") }.executes { context ->
                    val sender = context.source.sender
                    val id = StringArgumentType.getString(context, "id")

                    if (!id.matches(Regex("[A-Za-z0-9_]+"))) {
                        sender.sendMessage(Component.text("ID may only contain letters, numbers, or underscores.", NamedTextColor.RED))
                        return@executes 0
                    }

                    if (!actionsStore.contains(id)) {
                        sender.sendMessage(Component.text("Action not found: $id", NamedTextColor.RED))
                        return@executes 0
                    }

                    logger.info("Deleted action $id for user ${sender.name}")
                    actionsStore.delete(id)
                    sender.sendMessage(Component.text("Deleted action: $id"))
                    Command.SINGLE_SUCCESS
                }

                val actionsList = Commands.literal("list").requires { sender -> sender.getSender().hasPermission("guidialog.actions") }.executes { context ->
                    val sender = context.source.sender
                    val data = actionsStore.load()

                    sender.sendMessage(Component.text("All ${data.size} action(s):\n${data.entries.map {
                        "- ${it.key}: /${it.value}"
                    }.joinToString("\n")}"))

                    Command.SINGLE_SUCCESS
                }

                actions.then(actionsAdd)
                actions.then(actionsGet)
                actions.then(actionsDelete)
                actions.then(actionsList)

                root.then(send)
                root.then(actions)*/

                val root = Commands.literal("guidialog")
                    .then(
                        Commands.literal("actions")
                            .requires { sender -> sender.sender.hasPermission("guidialog.admin") }
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

                                    if (!id.matches(Regex("[A-Za-z0-9_]+"))) {
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
                                    sender.sendMessage(Component.text("Set action $id to command: /$command"))
                                    Command.SINGLE_SUCCESS
                                }
                            )))
                    )
                    .then(
                        Commands.literal("send")
                        .requires { sender -> sender.sender.hasPermission("guidialog.send") }
                        .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("payload", StringArgumentType.greedyString())
                        .executes { context ->
                            val sender = context.source.sender
                            val payload = StringArgumentType.getString(context, "payload")
                            val resolver = context.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                            val target: Player? = resolver.resolve(context.source).firstOrNull()

                            if (target == null) {
                                sender.sendMessage(Component.text("No matching player found.", NamedTextColor.RED))
                                return@executes 0
                            }

                            try {
                                val payload = Gson().fromJson(payload, DialogPayload::class.java)

                                if (payload == null) {
                                    sender.sendMessage(Component.text("Payload parsed to null.", NamedTextColor.RED))
                                    return@executes Command.SINGLE_SUCCESS
                                }

                                val dialog = payload.toDialog()
                                logger.info("Sending payload to user " + target.name + " from entity " + sender.name)
                                sessions[dialog.id] = target.uniqueId
                                sendPayload(target, dialog)
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
                            .requires { sender -> sender.sender.hasPermission("guidialog.admin") }
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

                commands.registrar().register(root.build(), "GUIDialog command description")
                logger.info("Registered commands: ${root.build().children.joinToString { it.name }}")
            })
    }

    fun sendPayload(player: Player, dialog: Dialog) {
        val bytes = dialog.build().toByteArray(Charsets.UTF_8)
        player.sendPluginMessage(this, "guidialog:dialog", bytes)
    }

    fun checkId(id: String): Boolean {
        return id.matches(Regex("[A-Za-z0-9_]+"))
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != "guidialog:action") return
        val data = ByteStreams.newDataInput(message)
        val id = data.readLong()
        val action = data.readUTF()

        logger.info("Received action $action and ID $id from channel $channel and player ${player.name}")

        if (sessions[id] != player.uniqueId) {
            player.sendMessage(Component.text("Invalid session ID.", NamedTextColor.RED))
            return
        }

        if (!action.matches(Regex("[A-Za-z0-9_]+"))) {
            player.sendMessage(Component.text("ID may only contain letters, numbers, or underscores: $action", NamedTextColor.RED))
            return
        }

        var command = actionsStore.get(action)

        if (command == null) {
            player.sendMessage(Component.text("Dialog action not found: $action", NamedTextColor.RED))
            return
        }

        command = command.replace("@s", player.name)

        logger.info("Running action $action for player ${player.name} and dialog $id: $command")
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        sessions.remove(id)
    }
}