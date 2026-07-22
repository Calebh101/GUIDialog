package com.calebh101.guidialog
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.MultiLineTextWidget
import net.minecraft.client.gui.components.ScrollableLayout
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.gui.layouts.FrameLayout
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class DialogScreen(val plugin: GUIDialog, val dialog: Dialog) : Screen(Component.literal(dialog.title)) {
    val logger
        get() = plugin.logger

    protected override fun init() {
        val buttonWidth = 120
        val buttonHeight = 20
        val contentWidth = 220
        val bottomMargin = 26

        val contentAreaHeight = this.height - bottomMargin - 10

        val content = MultiLineTextWidget(Component.literal(dialog.body), this.font).setMaxWidth(contentWidth)
        val frame = FrameLayout()
        frame.addChild(content)

        val scrollable = ScrollableLayout(this.minecraft, frame, contentAreaHeight)
        scrollable.arrangeElements()

        val contentX = (this.width - scrollable.width) / 2
        scrollable.setPosition(contentX, 10)
        scrollable.visitWidgets { this.addRenderableWidget(it) }

        val buttonRow = LinearLayout.horizontal().spacing(8)
        val labels = dialog.actions.keys + "Close"

        labels.forEach { label ->
            buttonRow.addChild(
                Button.builder(Component.literal(label)) {
                    if (label.lowercase() == "close") {
                        this.onClose()
                    } else {
                        val id = dialog.actions[label]!!
                        logger.info("Received label: $label ($id")
                    }
                }
                    .bounds(0, 0, 100, buttonHeight)
                    .build()
            )
        }

        buttonRow.arrangeElements()

        val rowX = (this.width - buttonRow.width) / 2
        val rowY = this.height - bottomMargin

        buttonRow.setPosition(rowX, rowY)
        buttonRow.visitWidgets { this.addRenderableWidget(it) }
    }

    public override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
}