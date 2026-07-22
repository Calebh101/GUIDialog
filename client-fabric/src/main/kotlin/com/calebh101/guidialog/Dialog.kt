package com.calebh101.guidialog

import com.google.gson.Gson
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import java.util.function.Function


data class Dialog(val title: String, val body: String, val actions: Map<String, String> /* pretty to ID */, val id: Long)

data class DialogPayload(val json: String) : CustomPacketPayload {
    private val gson = Gson()

    companion object {
        val ID: Identifier =
            Identifier.fromNamespaceAndPath("guidialog", "dialog")

        val TYPE: CustomPacketPayload.Type<DialogPayload> =
            CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, DialogPayload> = StreamCodec.of(
            { buf, value ->
                buf.writeBytes(value.json.toByteArray(Charsets.UTF_8))
            },
            { buf ->
                val bytes = ByteArray(buf.readableBytes())
                buf.readBytes(bytes)
                DialogPayload(String(bytes, Charsets.UTF_8))
            }
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    fun toDialog(): Dialog {
        return gson.fromJson(json, Dialog::class.java)
    }
}

data class DialogActionPayload(val bytes: ByteArray) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: Identifier = Identifier.fromNamespaceAndPath("guidialog", "action")
        val TYPE: CustomPacketPayload.Type<DialogActionPayload> = CustomPacketPayload.Type(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, DialogActionPayload> = StreamCodec.of(
            { buf, value -> buf.writeBytes(value.bytes) },
            { buf ->
                val bytes = ByteArray(buf.readableBytes())
                buf.readBytes(bytes)
                DialogActionPayload(bytes)
            }
        )
    }
}