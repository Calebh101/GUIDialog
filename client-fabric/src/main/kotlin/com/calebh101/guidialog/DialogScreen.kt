package com.calebh101.guidialog
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.MultiLineTextWidget
import net.minecraft.client.gui.components.ScrollableLayout
import net.minecraft.client.gui.components.StringWidget
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
        val rowSpacing = 4
        val bottomMargin = 10
        val contentMargin = 10
        val contentTop = 25

        val labels = dialog.actions.keys + "Close"
        val chunked = labels.chunked(3)

        val header = StringWidget(
            Component.literal(dialog.title),
            this.font
        )

        header.setPosition(
            (this.width - header.width) / 2,
            10
        )

        this.addRenderableWidget(header)

        val buttonsAreaHeight = (chunked.size * buttonHeight) + ((chunked.size - 1) * rowSpacing)
        val contentAreaHeight = this.height - contentTop - buttonsAreaHeight - bottomMargin - rowSpacing
        val contentWidth = this.width - (contentMargin * 2)

        val content = MultiLineTextWidget(Component.literal(dialog.body), this.font).setMaxWidth(contentWidth)
        val frame = FrameLayout()
        frame.addChild(content)

        val scrollable = ScrollableLayout(this.minecraft, frame, contentAreaHeight)
        scrollable.arrangeElements()

        val contentX = (this.width - scrollable.width) / 2
        scrollable.setPosition(contentX, 25)
        scrollable.visitWidgets { this.addRenderableWidget(it) }

        chunked.forEachIndexed { index, set ->
            val buttons = LinearLayout.horizontal().spacing(8)

            set.forEach { label ->
                buttons.addChild(
                    Button.builder(Component.literal(label)) {
                        if (label.lowercase() == "close") {
                            this.onClose()
                        } else {
                            val id = dialog.actions[label]!!
                            logger.info("Received action: $id ($label)")

                            plugin.sendAction(id = dialog.id, action = id)
                            this.onClose()
                        }
                    }
                        .bounds(0, 0, buttonWidth, buttonHeight)
                        .build()
                )
            }

            buttons.arrangeElements()

            val rowX = (this.width - buttons.width) / 2
            val rowsFromBottom = chunked.size - index  // 1 for last row, 2 for second-to-last, etc.
            val rowY = this.height - bottomMargin - (rowsFromBottom * buttonHeight) - ((rowsFromBottom - 1) * rowSpacing)

            buttons.setPosition(rowX, rowY)
            buttons.visitWidgets { this.addRenderableWidget(it) }
        }
    }

    public override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }
}