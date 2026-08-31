package mods.eln.sixnode.lampsupply

import mods.eln.misc.Utils
import mods.eln.sim.IProcess
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx
import mods.eln.sixnode.wirelesssignal.WirelessUtils

class LampSupplyProcess(val element: LampSupplyElement) : IProcess {

    private var sleepTimer = 0.0

    private val txSet = hashMapOf<String, HashSet<IWirelessSignalTx>>()
    private val txStrength = hashMapOf<IWirelessSignalTx, Double>()

    override fun process(time: Double) {
        if (element.connectedConductance <= (1 / MnaConst.highImpedance)) element.loadResistor.highImpedance()
        else element.loadResistor.setResistance(1.0 / element.connectedConductance)

        element.connectedConductance = 0.0
        sleepTimer -= time

        if (sleepTimer < 0) {
            sleepTimer += Utils.rand(1.2, 2.0)

            val spot = WirelessUtils.buildSpot(element.coordinate, null, 0)
            WirelessUtils.getTx(spot, txSet, txStrength)
        }

        for (idx in 0..<LampSupplyDescriptor.CHANNEL_COUNT) {
            val localEntry = element.localEntries[idx]

            when (localEntry.wirelessChannel.lowercase()) {
                "" -> element.localChannelStates[idx] = true
                "true" -> element.localChannelStates[idx] = true
                "false" -> element.localChannelStates[idx] = false
                else -> {
                    val txs = txSet[localEntry.wirelessChannel]

                    if (txs == null) element.localChannelStates[idx] = false
                    else element.localChannelStates[idx] = element.localAggregators[idx][localEntry.aggregator].aggregate(txs) >= 0.5
                }
            }
        }
    }

}