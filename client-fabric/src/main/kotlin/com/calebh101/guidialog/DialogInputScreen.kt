package com.calebh101.guidialog

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.mojang.brigadier.Command
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.MultiLineEditBox
import net.minecraft.client.gui.components.MultiLineTextWidget
import net.minecraft.client.gui.components.MultilineTextField
import net.minecraft.client.gui.components.ScrollableLayout
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

class DialogInputScreen(val plugin: GUIDialog) : Screen(Component.literal("Dialog Input")) {
    val logger
        get() = plugin.logger

    protected override fun init() {
        val buttonWidth = 120
        val buttonHeight = 20
        val bottomMargin = 10
        val sideMargin = 20
        val topMargin = 10
        val labelHeight = 12
        val singleLineHeight = 20
        val spacing = 4

        val titleLabel = StringWidget(Component.literal("ID"), this.font)
        titleLabel.setPosition(sideMargin, topMargin)
        this.addRenderableWidget(titleLabel)
        val singleLineY = topMargin + labelHeight + spacing

        val titleInput = EditBox(
            this.font,
            sideMargin,
            singleLineY,
            this.width - sideMargin * 2,
            singleLineHeight,
            Component.literal("ID input")
        )

        this.addRenderableWidget(titleInput)
        val bodyLabelY = singleLineY + singleLineHeight + spacing
        val bodyLabel = StringWidget(Component.literal("Dialog (JSON)"), this.font)
        bodyLabel.setPosition(sideMargin, bodyLabelY)
        this.addRenderableWidget(bodyLabel)

        val bodyInputY = bodyLabelY + labelHeight + spacing
        val buttonsY = this.height - bottomMargin - buttonHeight
        val bodyInputHeight = buttonsY - spacing - bodyInputY

        val bodyInput = MultiLineEditBox.builder()
            .setX(sideMargin)
            .setY(bodyInputY)
            .setPlaceholder(Component.literal("JSON input"))
            .build(this.font, this.width - sideMargin * 2, bodyInputHeight, Component.literal("Dialog (JSON)"))

        this.addRenderableWidget(bodyInput)
        val buttons = LinearLayout.horizontal().spacing(8)

        buttons.addChild(
            Button.builder(Component.literal("Submit")) {
                fun error(message: String) {
                    SystemToast.add(
                        Minecraft.getInstance().toastManager,
                        SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                        Component.literal("Error"),
                        Component.literal(message)
                    )
                }

                if (!plugin.checkId(titleInput.value)) {
                    error("Invalid ID; ID can only contain letters, numbers, or underscores.")
                    return@builder
                }

                try {
                    Gson().fromJson(bodyInput.value, OtherDialog::class.java)
                } catch (e: JsonSyntaxException) {
                    error("Invalid JSON.")
                    return@builder
                } catch (e: IllegalStateException) {
                    error("Invalid payload: ${e.message}")
                    return@builder
                }

                plugin.sendRegisterDialog(titleInput.value, bodyInput.value)
                this.onClose()
            }
                .bounds(0, 0, buttonWidth, buttonHeight)
                .build()
        )

        buttons.addChild(
            Button.builder(Component.literal("Close")) {
                this.onClose()
            }
                .bounds(0, 0, buttonWidth, buttonHeight)
                .build()
        )

        buttons.arrangeElements()

        val rowX = (this.width - buttons.width) / 2
        val rowY = this.height - bottomMargin - buttonHeight

        buttons.setPosition(rowX, rowY)
        buttons.visitWidgets { this.addRenderableWidget(it) }
    }

    public override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
}