package io.github.eirikh1996.movecraftspace.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.util.ChatPaginator.CLOSED_CHAT_PAGE_HEIGHT
import kotlin.math.ceil

class Paginator constructor(val name: String = "") {
    private val content = ArrayList<Component>()

    fun addLine(line: TextComponent) {
        content.add(line)
    }

    fun getPage(pageNo: Int, command: String) : Array<Component?> {
        if (!inBounds(pageNo))
            throw IndexOutOfBoundsException(pageNo.toString() + " is out of bounds")
        val lineCount = if (pageNo == pageCount) { (content.size % CLOSED_CHAT_PAGE_HEIGHT) + 1 } else CLOSED_CHAT_PAGE_HEIGHT
        val page = arrayOfNulls<Component>(lineCount + 2)
        page[0] = Component.text("==========", NamedTextColor.DARK_PURPLE)
            .append(Component.text(name, NamedTextColor.BLUE))
            .append(Component.text("==========", NamedTextColor.DARK_PURPLE))//TextComponent("§5==========§9 " + name + " §5==========")
        for (i in 0 until lineCount - 1) {
            val index = ((CLOSED_CHAT_PAGE_HEIGHT - 1) * (pageNo - 1)) + i
            page[i + 1] = content[index]
        }
        val clickArrows = Component.text(
            if (pageNo == 1) "---" else "<<<",
            if (pageNo == 1) NamedTextColor.DARK_RED else NamedTextColor.DARK_GREEN
        )
        if (pageNo > 1) {
            clickArrows.clickEvent(ClickEvent.runCommand(command + " " + (pageNo - 1)))
        }
        clickArrows.append(Component.text(" | ", NamedTextColor.BLUE))
        val lastClickArrow = Component.text(
            if (pageNo == pageCount) "---" else ">>>",
            if (pageNo == pageCount) NamedTextColor.DARK_RED else NamedTextColor.DARK_GREEN
        )
        if (pageNo < pageCount)
            lastClickArrow.clickEvent(ClickEvent.runCommand(command + " " + (pageNo + 1)))
        clickArrows.append(lastClickArrow)
        page[lineCount + 1] = clickArrows
        return page
    }

    private val pageCount : Int get() {
        return ceil((content.size).toDouble() / (CLOSED_CHAT_PAGE_HEIGHT - 1)).toInt()
    }

    private fun inBounds(page: Int) : Boolean {
        return page > 0 && page <= pageCount
    }
}