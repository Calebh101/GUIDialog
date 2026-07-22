package org.calebh101.gUIDialog

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class GUIDialog : JavaPlugin(), Listener {
    val channel = NamespacedKey("guidialog", "sync")
    val actionsStore = ActionStore(this)

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        logger.info("Loading GUIDialog...")
        server.messenger.registerOutgoingPluginChannel(this, channel.toString())

        this.lifecycleManager.registerEventHandler<ReloadableRegistrarEvent<Commands>>(
            LifecycleEvents.COMMANDS,
            LifecycleEventHandler { commands: ReloadableRegistrarEvent<Commands>? ->
                val send = Commands.literal("send").then(Commands.argument("player", ArgumentTypes.player())).then(Commands.argument("payload", StringArgumentType.greedyString()))
                val actions = Commands.literal("actions")

                val actionsAdd = Commands.literal("add").then(Commands.argument("id", StringArgumentType.word())).then(Commands.argument("command", StringArgumentType.greedyString()))
                val actionsGet = Commands.literal("get").then(Commands.argument("id", StringArgumentType.word()))
                val actionsDelete = Commands.literal("delete").then(Commands.argument("id", StringArgumentType.word()))
                val actionsList = Commands.literal("list")

                actions.then(actionsAdd)
                actions.then(actionsGet)
                actions.then(actionsDelete)
                actions.then(actionsList)

                val root: LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("dialog")
                root.then(send)
                root.then(actions)

                send.requires { sender -> sender.getSender().hasPermission("guidialog.send") }.executes { context ->
                    val sender = context.source.sender
                    val payload = StringArgumentType.getString(context, "payload")
                    val resolver = context.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                    val target: Player? = resolver.resolve(context.source).firstOrNull()

                    if (target == null) {
                        sender.sendMessage(Component.text("No matching player found.", NamedTextColor.RED))
                        return@executes 0
                    }

                    try {
                        val dialog = Gson().fromJson(payload, Dialog::class.java)

                        if (dialog == null) {
                            sender.sendMessage(Component.text("Payload parsed to null.", NamedTextColor.RED))
                            return@executes Command.SINGLE_SUCCESS
                        }

                        logger.info("Sending payload to user " + target.name + " from entity " + sender.name)
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

                actionsAdd.requires { sender -> sender.getSender().hasPermission("guidialog.actions") }.executes { context ->
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

                actionsGet.requires { sender -> sender.getSender().hasPermission("guidialog.actions") }.executes { context ->
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

                actionsDelete.requires { sender -> sender.getSender().hasPermission("guidialog.actions") }.executes { context ->
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

                actionsList.requires { sender -> sender.getSender().hasPermission("guidialog.actions") }.executes { context ->
                    val sender = context.source.sender
                    val data = actionsStore.load()

                    sender.sendMessage(Component.text("All ${data.size} action(s):\n${data.entries.map {
                        "- ${it.key}: /${it.value}"
                    }.joinToString("\n")}"))

                    Command.SINGLE_SUCCESS
                }
            })
    }

    fun sendPayload(player: Player, dialog: Dialog) {
        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)

        data.writeUTF(dialog.build())
        player.sendPluginMessage(this, channel.toString(), out.toByteArray())
    }
}