package com.calebh101.guidialog

import com.mojang.authlib.minecraft.client.MinecraftClient
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
	const val debug = false

	val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		logger.info("Loading GUIDialog...")

		PayloadTypeRegistry.clientboundPlay().register(DialogPayload.TYPE, DialogPayload.CODEC)
		PayloadTypeRegistry.clientboundPlay().register(DoSomethingPayload.TYPE, DoSomethingPayload.CODEC)

		PayloadTypeRegistry.serverboundPlay().register(DialogActionPayload.TYPE, DialogActionPayload.CODEC)
		PayloadTypeRegistry.serverboundPlay().register(DialogRegisterPayload.TYPE, DialogRegisterPayload.CODEC)

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			if (debug) {
				dispatcher.register(
					ClientCommands.literal("dialog_test").executes { context ->
						val name = Minecraft.getInstance().player?.gameProfile?.name

						Minecraft.getInstance().execute {
							Minecraft.getInstance().setScreen(
								DialogScreen(this, Dialog(title = "Test Dialog", body = "You should see this dialog!", actions = mapOf("Ok" to "ok"), id = Random.nextLong()), name = name ?: "Unknown")
							)
						}

						context!!.getSource()!!.sendFeedback(Component.literal("Opened test dialog."))
						1
					}
				)

				dispatcher.register(
					ClientCommands.literal("dialog_input").executes { context ->
						Minecraft.getInstance().execute {
							Minecraft.getInstance().setScreen(
								DialogInputScreen(this)
							)
						}

						1
					}
				)
			}
		}

		ClientPlayNetworking.registerGlobalReceiver(DialogPayload.TYPE) { payload, context ->
			val dialog = payload.toDialog()
			val name = Minecraft.getInstance().player?.gameProfile?.name ?: return@registerGlobalReceiver
			logger.info("Executing dialog ${dialog.id}: ${dialog.title}")

			context.client().execute {
				Minecraft.getInstance().setScreen(
					DialogScreen(this, dialog, name)
				)
			}
		}

		ClientPlayNetworking.registerGlobalReceiver(DoSomethingPayload.TYPE) { payload, context ->
			val action = payload.action

			if (action == "dialog_input") {
				context.client().execute {
					Minecraft.getInstance().setScreen(
						DialogInputScreen(this)
					)
				}
			} else {
				val client = Minecraft.getInstance()
				client.player?.sendSystemMessage(Component.literal("[GUIDialog] Invalid action: $action\nIs GUIDialog up to date?"))
			}
		}
	}

	fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

	fun checkId(id: String): Boolean {
		return id.matches(Regex("[A-Za-z0-9_]+"))
	}

	fun sendAction(id: Long, action: String) {
		val out = ByteArrayOutputStream()
		val data = DataOutputStream(out)

		data.writeLong(id)
		data.writeUTF(action)

		ClientPlayNetworking.send(DialogActionPayload(out.toByteArray()))
	}

	fun sendRegisterDialog(id: String, dialog: String) {
		val out = ByteArrayOutputStream()
		val data = DataOutputStream(out)

		data.writeUTF("$id $dialog")
		ClientPlayNetworking.send(DialogRegisterPayload(out.toByteArray()))
	}
}
