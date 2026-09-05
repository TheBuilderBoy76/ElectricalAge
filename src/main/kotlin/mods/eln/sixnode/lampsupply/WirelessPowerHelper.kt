package mods.eln.sixnode.lampsupply

import mods.eln.misc.Coordinate

data class AvailableSupply(val powerChannel: LampSupplyElement.PowerSupplyChannelHandle, val distance: Double)

interface IWirelessPower {
    var previousConnectedSupply: AvailableSupply?
    val powerChannel: String
    val coordinate: Coordinate
    val loadResistance: Double

    fun updateLoadVoltage(newVoltage: Double)
}

object LampSupplyConnectionHelper {

    private fun findBestLampSupply(device: IWirelessPower): AvailableSupply? {
        val availableSupplies = LampSupplyElement.globalChannelMap[device.powerChannel]

        return when {
            availableSupplies == null -> {
                null
            }

            availableSupplies.contains(device.previousConnectedSupply?.powerChannel) && !LampSupplyElement.forceCachedLampSupplyUpdate -> {
                device.previousConnectedSupply
            }

            else -> {
                availableSupplies.map { AvailableSupply(it, it.element.sixNode!!.coordinate.trueDistanceTo(device.coordinate)) }
                    .filter {it.distance < it.powerChannel.element.range}.minByOrNull { it.distance }
            }
        }
    }

    fun connectToLampSupply(device: IWirelessPower): Boolean {
        device.previousConnectedSupply = findBestLampSupply(device)

        val bestPowerChannel = device.previousConnectedSupply?.powerChannel

        if (bestPowerChannel != null && bestPowerChannel.element.getChannelState(bestPowerChannel.id)) {
            bestPowerChannel.element.addToConductance(device.loadResistance)
            device.updateLoadVoltage(bestPowerChannel.element.electricalLoad.voltage)
        } else {
            device.updateLoadVoltage(0.0)
        }

        return (bestPowerChannel != null)
    }

}