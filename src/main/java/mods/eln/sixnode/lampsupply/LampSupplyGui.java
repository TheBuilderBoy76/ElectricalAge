package mods.eln.sixnode.lampsupply;

import mods.eln.gui.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import static mods.eln.i18n.I18N.tr;

public class LampSupplyGui extends GuiContainerEln {

    public static final byte SET_POWER_NAME_EVENT = 0;
    public static final byte SET_WIRELESS_NAME_EVENT = 1;
    public static final byte SET_SELECTED_AGGREGATOR_EVENT = 2;

    private LampSupplyRender render;

    private HashMap<Object, Integer> powerMap = new HashMap<Object, Integer>();
    private HashMap<Object, Integer> wirelessMap = new HashMap<Object, Integer>();

    public LampSupplyGui(LampSupplyRender render, EntityPlayer player, IInventory inventory) {
        super(new LampSupplyContainer(player, inventory));
        this.render = render;
    }

    class AggregatorBt extends GuiButtonEln {
        byte id;
        int channel;

        public AggregatorBt(int x, int y, int width, int height, String str, int channel, byte id) {
            super(x, y, width, height, str);
            this.id = id;
            this.channel = channel;
        }

        @Override
        public void onMouseClicked() {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream stream = new DataOutputStream(bos);

                render.preparePacketForServer(stream);

                stream.writeByte(SET_SELECTED_AGGREGATOR_EVENT);
                stream.writeInt(channel);
                stream.writeInt(id);

                render.sendPacketToServer(bos);
            } catch (IOException e) {

                e.printStackTrace();
            }
            super.onMouseClicked();
        }

        @Override
        public void idraw(int x, int y, float f) {
            this.enabled = render.getLocalEntries().get(channel).aggregator != id;
            super.idraw(x, y, f);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        int y = 6;

        int x;
        for (int id = 0; id < LampSupplyDescriptor.CHANNEL_COUNT; id++) {
            x = 6;

            LampSupplyElement.LocalLampSupplyEntry e = render.getLocalEntries().get(id);
            GuiTextFieldEln powerChannel = newGuiTextField(x, y, 101);
            x += powerChannel.getWidth() + 12;
            powerChannel.setText(e.powerChannel);
            powerChannel.setComment(0, tr("Power channel name"));
            powerMap.put(powerChannel, id);

            GuiTextFieldEln wirelessChannel = newGuiTextField(x, y, 101);
            x += wirelessChannel.getWidth() + 12;
            wirelessChannel.setText(e.wirelessChannel);
            wirelessChannel.setComment(0, tr("Wireless channel name"));
            wirelessMap.put(wirelessChannel, id);
            y += wirelessChannel.getHeight() + 2;
            x = 6;
            int w = 68;
            AggregatorBt buttonBigger, buttonSmaller, buttonToogle;

            add(buttonBigger = new AggregatorBt(x, y, w, 20, tr("Biggest"), id, (byte) 0));
            x += 2 + w;
            add(buttonSmaller = new AggregatorBt(x, y, w, 20, tr("Smallest"), id, (byte) 1));
            x += 2 + w;
            add(buttonToogle = new AggregatorBt(x, y, w, 20, tr("Toggle"), id, (byte) 2));
            x += 2 + w;

            buttonBigger.setHelper(helper);
            int lineNumber = 0;
            for (String line : tr("Uses the biggest\nvalue on the channel.").split("\n"))
                buttonBigger.setComment(lineNumber++, line);

            buttonSmaller.setHelper(helper);
            lineNumber = 0;
            for (String line : tr("Uses the smallest\nvalue on the channel.").split("\n"))
                buttonSmaller.setComment(lineNumber++, line);

            buttonToogle.setHelper(helper);
            lineNumber = 0;
            for (String line : tr("Toggles the output each time\nan emitter's value rises.\nUseful to allow multiple buttons\nto control the same light.").split("\n"))
                buttonToogle.setComment(lineNumber++, line);
            y += buttonToogle.height + 6;
        }
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 220, 205, 8, 125);
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        if (powerMap.containsKey(object)) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream stream = new DataOutputStream(bos);

                render.preparePacketForServer(stream);
                stream.writeByte(SET_POWER_NAME_EVENT);
                stream.writeInt(powerMap.get(object));
                stream.writeUTF(((GuiTextFieldEln) object).getText());

                render.sendPacketToServer(bos);
            } catch (IOException e) {

                e.printStackTrace();
            }

        }
        if (wirelessMap.containsKey(object)) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream stream = new DataOutputStream(bos);

                render.preparePacketForServer(stream);
                stream.writeByte(SET_WIRELESS_NAME_EVENT);
                stream.writeInt(wirelessMap.get(object));
                stream.writeUTF(((GuiTextFieldEln) object).getText());

                render.sendPacketToServer(bos);
            } catch (IOException e) {

                e.printStackTrace();
            }

        }
        super.guiObjectEvent(object);
    }
}
