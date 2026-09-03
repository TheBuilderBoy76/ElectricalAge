package mods.eln.sixnode.powersocket

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor.Companion.getDescriptor
import mods.eln.item.BrushDescriptor
import mods.eln.item.IConfigurable
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.plotUIP
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sixnode.lampsupply.AvailableSupply
import mods.eln.sixnode.lampsupply.IWirelessPower
import mods.eln.sixnode.lampsupply.LampSupplyConnectionHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.pow

class PowerSocketElement(sixNode: SixNode?, side: Direction?, descriptor: SixNodeDescriptor?) : SixNodeElement(
    sixNode!!, side!!, descriptor!!
), IConfigurable {
    var descriptor: PowerSocketDescriptor?
    var electricalLoad = NbtElectricalLoad("electricalLoad")
    var voltageSource = VoltageSource("voltSrc", electricalLoad, null)
    private var powerSocketProcess: IProcess = PowerSocketProcess(this)
    var channel = "Default channel"
    var paintColor = 0
    var voltageWatchdog = VoltageStateWatchDog(electricalLoad)

    init {
        electricalLoadList.add(electricalLoad)
        electricalComponentList.add(voltageSource)
        slowProcessList.add(powerSocketProcess)
        this.descriptor = descriptor as PowerSocketDescriptor?
        slowProcessList.add(voltageWatchdog)
        voltageWatchdog.setDestroys(WorldExplosion(this).cableExplosion())
        voltageWatchdog.setNominalVoltage(300.0)
    }

    class PowerSocketProcess(val element: PowerSocketElement) : IProcess, IWirelessPower {
        override var previousConnectedSupply: AvailableSupply? = null

        override val powerChannel: String
            get() = element.channel
        override val coordinate: Coordinate
            get() = element.coordinate!!
        override val loadResistance: Double
            get() = element.voltageSource.voltage.pow(2) / element.voltageSource.power

        // Unused in this specific implementation
        override fun updateLoadState(newState: Double) {}

        override fun process(time: Double) {
            previousConnectedSupply = LampSupplyConnectionHelper.findBestLampSupply(this)

            val bestPowerChannel = previousConnectedSupply?.powerChannel

            if (bestPowerChannel != null && bestPowerChannel.element.getChannelState(bestPowerChannel.id)) {
                element.voltageSource.setVoltage(bestPowerChannel.element.electricalLoad.voltage)
                bestPowerChannel.element.addToConductance(loadResistance)
            } else {
                element.voltageSource.setVoltage(0.0)
            }
        }
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad {
        return electricalLoad
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return NodeBase.maskElectricalPower + (1 shl NodeBase.maskColorCareShift) + (paintColor shl NodeBase.maskColorShift)
    }

    override fun multiMeterString(): String {
        return plotUIP(electricalLoad.voltage, electricalLoad.getCurrent())
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun initialize() {
        Eln.applySmallRs(electricalLoad)
    }

    override fun inventoryChanged() {
        super.inventoryChanged()
        sixNode!!.disconnect()
        sixNode!!.connect()
        needPublish()
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setString("channel", channel)
        nbt.setInteger("color", paintColor)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        channel = nbt.getString("channel")
        paintColor = nbt.getInteger("color")
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            when (stream.readByte()) {
                setChannelId -> {
                    channel = stream.readUTF()
                    needPublish()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeUTF(channel)
            stream.writeInt(paintColor)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onBlockActivated(
        entityPlayer: EntityPlayer,
        side: Direction,
        vx: Float,
        vy: Float,
        vz: Float
    ): Boolean {
        val used = entityPlayer.currentEquippedItem
        if (used != null) {
            val desc = getDescriptor(used)
            if (desc != null && desc is BrushDescriptor) {
                val color: Int = desc.getColor(used)
                if (color != paintColor && desc.use(used, entityPlayer)) {
                    paintColor = color
                    sixNode!!.reconnect()
                }
                return true
            }
        }
        return false
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("powerChannels")) {
            val newChannel = compound.getTagList("powerChannels", 8).getStringTagAt(0)
            if (newChannel != null && !newChannel.isEmpty()) {
                channel = newChannel
                needPublish()
            }
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        val list = NBTTagList()
        list.appendTag(NBTTagString(channel))
        compound.setTag("powerChannels", list)
    }

    companion object {
        const val setChannelId: Byte = 1
    }
}
