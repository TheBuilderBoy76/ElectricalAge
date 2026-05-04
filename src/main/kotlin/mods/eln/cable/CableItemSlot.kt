package mods.eln.cable

import mods.eln.gui.ISlotSkin
import mods.eln.i18n.I18N
import mods.eln.misc.Utils
import mods.eln.node.six.SixNodeItemSlot
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

/**
 * This class should be used going forward for all devices which accept cables in their inventory. It provides the
 * ability to only accept utility cable spools of a certain length, as well as prevent legacy signal cables from being
 * accepted in inventories where they do not make sense.
 */
class CableItemSlot(
    inventory: IInventory,
    slot: Int,
    x: Int,
    y: Int,
    stackLimit: Int,
    val acceptSignalCable: Boolean,
    val expectedCableLength: Int,
    comment: Array<String> = arrayOf(I18N.tr("Cable slot"))
) : SixNodeItemSlot(
    inventory, slot, x, y, stackLimit, arrayOf(
        ElectricalCableDescriptor::class.java, CurrentCableDescriptor::class.java, UtilityCableDescriptor::class.java
    ), ISlotSkin.SlotSkin.medium, comment
) {

    override fun isItemValid(itemStack: ItemStack): Boolean {
        if (!super.isItemValid(itemStack)) return false

        val cableDescriptor = Utils.getItemObject(itemStack)

        if (cableDescriptor is UtilityCableDescriptor) {
            val cableLength = cableDescriptor.getRemainingLengthMeters(itemStack).toInt()
            return cableLength == expectedCableLength
        }

        return if (cableDescriptor is ElectricalCableDescriptor) !(cableDescriptor.signalWire && !acceptSignalCable) else true
    }

}