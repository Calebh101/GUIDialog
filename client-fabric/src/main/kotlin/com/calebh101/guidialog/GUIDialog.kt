package com.calebh101.guidialog

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import java.util.function.Supplier

object GUIDialog : ModInitializer {
	const val MOD_ID: String = "guidialog"
	val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		logger.info("Loading GUIDialog...")

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(
				ClientCommands.literal("test_dialog").executes { context ->
					Minecraft.getInstance().execute {
						Minecraft.getInstance().setScreen(
							DialogScreen(this, Dialog(title = "Test Dialog", body = "You should see this dialog!", actions = mapOf("Ok" to "ok")))
						)
					}

					context!!.getSource()!!.sendFeedback(Component.literal("Opened test dialog."))
					1
				}
			)
		}
	}

	fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

	fun showDialog(dialog: Dialog) {}
}
