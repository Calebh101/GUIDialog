package com.calebh101.guidialog

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.function.Supplier
import kotlin.random.Random

object GUIDialog : ModInitializer {
	const val MOD_ID: String = "guidialog"
	val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		logger.info("Loading GUIDialog...")

		PayloadTypeRegistry.clientboundPlay().register(DialogPayload.TYPE, DialogPayload.CODEC)
		PayloadTypeRegistry.serverboundPlay().register(DialogActionPayload.TYPE, DialogActionPayload.CODEC)

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(
				ClientCommands.literal("test_dialog").executes { context ->
					Minecraft.getInstance().execute {
						Minecraft.getInstance().setScreen(
							DialogScreen(this, Dialog(title = "Test Dialog", body = "You should see this dialog!", actions = mapOf("Ok" to "ok"), id = Random.nextLong()))
						)
					}

					context!!.getSource()!!.sendFeedback(Component.literal("Opened test dialog."))
					1
				}
			)
		}

		ClientPlayNetworking.registerGlobalReceiver(DialogPayload.TYPE) { payload, context ->
			val dialog = payload.toDialog()
			logger.info("Executing dialog ${dialog.id}: ${dialog.title}")

			context.client().execute {
				Minecraft.getInstance().setScreen(
					DialogScreen(this, dialog = dialog)
				)
			}
		}
	}

	fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

	fun sendAction(id: Long, action: String) {
		val out = ByteArrayOutputStream()
		val data = DataOutputStream(out)

		data.writeLong(id)
		data.writeUTF(action)

		ClientPlayNetworking.send(DialogActionPayload(out.toByteArray()))
	}
}
