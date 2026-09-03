package mods.eln.transparentnode.festive

import mods.eln.i18n.I18N.tr
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.lampsupply.AvailableSupply
import mods.eln.sixnode.lampsupply.IWirelessPower
import mods.eln.sixnode.lampsupply.LampSupplyConnectionHelper
import net.minecraft.entity.player.EntityPlayer
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs

class FestiveElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {

    val electricalLoad = NbtElectricalLoad("electricalLoad")
    val loadResistor = Resistor(electricalLoad, null)
    var powerChannel = "xmas" // TODO: Add a GUI in the render panes and allow the user to specify a different channel.

    init {
        loadResistor.resistance = 1000.0
        slowProcessList.add(FestiveElementProcess(this))
    }

    override fun thermoMeterString(side: Direction): String {
        return tr("Not as warm as it could be")
    }

    override fun multiMeterString(side: Direction): String {
        return tr("It probably works if you apply ~200v to the xmas wireless channel")
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return null
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return 0
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        return null
    }

    override fun initialize() {
        connect()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeBoolean(node!!.lightValue > 4)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    class FestiveElementProcess(val elem: FestiveElement): IProcess, IWirelessPower {
        override var previousConnectedSupply: AvailableSupply? = null

        override val powerChannel: String
            get() = elem.powerChannel
        override val coordinate: Coordinate
            get() = elem.coordinate()
        override val loadResistance: Double
            get() = elem.loadResistor.resistance

        override fun updateLoadState(newState: Double) {
            elem.electricalLoad.state = newState
        }

        override fun process(time: Double) {
            LampSupplyConnectionHelper.connectToLampSupply(this)
            var lightDouble = 12 * (abs(elem.loadResistor.voltage) - 180.0) / 20.0
            lightDouble *= 16
            elem.node!!.lightValue = lightDouble.toInt().coerceIn(0, 15)
        }
    }
}
