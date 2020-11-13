package io.github.eirikh1996.movecraftspace.utils

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit.broadcastMessage
import org.bukkit.util.ChatPaginator.CLOSED_CHAT_PAGE_HEIGHT
import kotlin.math.ceil

class Paginator constructor(val name: String = "") {
    private val content = ArrayList<TextComponent>()

    fun addLine(line: TextComponent) {
        content.add(line)
    }

    fun getPage(pageNo: Int, command: String) : Array<TextComponent?> {
        if (!inBounds(pageNo))
            throw IndexOutOfBoundsException(pageNo.toString() + " is out of bounds")
        val lineCount = if (pageNo == pageCount) { (content.size % CLOSED_CHAT_PAGE_HEIGHT) + 1 } else CLOSED_CHAT_PAGE_HEIGHT
        val page = arrayOfNulls<TextComponent>(lineCount + 2)
        page[0] = TextComponent("§5==========§9 " + name + " §5==========")
        for (i in 0 until lineCount - 1) {
            val index = ((CLOSED_CHAT_PAGE_HEIGHT - 1) * (pageNo - 1)) + i
            page[i + 1] = content[index]
        }
        val clickArrows = TextComponent(
            if (pageNo == 1) "---" else {
                "<<<"
            }
        )
        clickArrows.color = if (pageNo == 1) ChatColor.RED else ChatColor.GREEN
        if (pageNo > 1) {
            clickArrows.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command + " " + (pageNo - 1))
        }
        val separator = TextComponent(" | ")
        separator.color = ChatColor.BLUE
        val lastClickArrow = TextComponent(
            if (pageNo == pageCount) "---" else {
                ">>>"
            }
        )
        lastClickArrow.color = if (pageNo == pageCount) ChatColor.RED else ChatColor.GREEN
        if (pageNo < pageCount)
            lastClickArrow.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command + " " + (pageNo + 1))
        clickArrows.addExtra(separator)
        clickArrows.addExtra(lastClickArrow)
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