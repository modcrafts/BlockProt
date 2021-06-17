/*
 * This file is part of BlockProt, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 spnda
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.sean.blockprot.bukkit.events

import de.sean.blockprot.bukkit.inventories.*
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler
import de.sean.blockprot.bukkit.nbt.LockUtil
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.BlockInventoryHolder

class InventoryEvent : Listener {
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val state = InventoryState.get(player.uniqueId)
        if (state != null) {
            // We have some sort of inventory state, so we'll
            // assume the player is currently in some of our inventories.
            when (event.inventory.holder) {
                is BlockLockInventory -> (event.inventory.holder as BlockLockInventory).onClick(event, state)
                is BlockInfoInventory -> (event.inventory.holder as BlockInfoInventory).onClick(event, state)
                is FriendDetailInventory -> (event.inventory.holder as FriendDetailInventory).onClick(event, state)
                is FriendManageInventory -> (event.inventory.holder as FriendManageInventory).onClick(event, state)
                is FriendSearchResultInventory -> (event.inventory.holder as FriendSearchResultInventory).onClick(event, state)
                is UserSettingsInventory -> (event.inventory.holder as UserSettingsInventory).onClick(event, state)
            }
        } else {
            // No state, let's check if they're in some block inventory.
            try {
                // Casting null does not trigger a ClassCastException.
                if (event.inventory.holder == null) return
                val blockHolder = event.inventory.holder as BlockInventoryHolder
                if (LockUtil.isLockable(blockHolder.block.type)) {
                    // Ok, we have a lockable block, check if they can write anything to this.
                    // TODO: Implement a Cache for this lookup, it seems to be quite expensive.
                    //       We should probably use a MultiMap, or implement our own Key that
                    //       can use multiple key objects, a Block and Player in this case.
                    val handler = BlockNBTHandler(blockHolder.block)
                    val playerUuid = player.uniqueId.toString()
                    if (!handler.canAccess(playerUuid)) {
                        player.closeInventory()
                        event.isCancelled = true
                        return
                    }
                    val friend = handler.getFriend(playerUuid)
                    if (friend.isPresent && !friend.get().canWrite()) {
                        event.isCancelled = true
                        return
                    }
                }
            } catch (e: ClassCastException) {
                // It's not a block and it's therefore also not lockable.
                // This is probably some other custom inventory from another
                // plugin, or possibly some entity inventory, e.g. villagers.
                return
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as Player
        val state = InventoryState.get(player.uniqueId) ?: return
        when (event.inventory.holder) {
            is BlockLockInventory -> (event.inventory.holder as BlockLockInventory).onClose(event, state)
            is BlockInfoInventory -> (event.inventory.holder as BlockInfoInventory).onClose(event, state)
            is FriendDetailInventory -> (event.inventory.holder as FriendDetailInventory).onClose(event, state)
            is FriendManageInventory -> (event.inventory.holder as FriendManageInventory).onClose(event, state)
            is FriendSearchResultInventory -> (event.inventory.holder as FriendSearchResultInventory).onClose(event, state)
            is UserSettingsInventory -> (event.inventory.holder as UserSettingsInventory).onClose(event, state)
        }
    }
}
