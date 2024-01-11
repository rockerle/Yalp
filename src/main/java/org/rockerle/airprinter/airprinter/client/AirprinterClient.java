package org.rockerle.airprinter.airprinter.client;

import fi.dy.masa.litematica.data.DataManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class AirprinterClient implements ClientModInitializer {

    private KeyBinding toggle;
//    private KeyBinding test;
    private boolean runPrinter = false;
    private Printer printer;
    @Override
    public void onInitializeClient() {
        toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding("printer toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O,""));
//        test = KeyBindingHelper.registerKeyBinding(new KeyBinding("printer test button", InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_P,"cat.test"));
//        this.printer = new Printer(MinecraftClient.getInstance());
        ClientTickEvents.END_CLIENT_TICK.register(ctx->{
            if(this.runPrinter && ctx.player.age%2==0){
                try{
                    printer.tick();
                }catch(Exception e){
                    e.printStackTrace();
                    printer.reset();
                }
            }
            if(toggle.wasPressed()) {
                this.runPrinter = !this.runPrinter;
                MinecraftClient.getInstance().player.sendMessage(Text.of("Toggled Printer "+(this.runPrinter?"§2on":"§4off")));
            }
//            if(test.wasPressed()){
//                DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement().toggleRenderEnclosingBox();
//                fi.dy.masa.litematica.selection.Box bb = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement().getEclosingBox();
//                net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(bb.getPos1().toCenterPos(),bb.getPos2().toCenterPos());
//                System.out.println("litematica box has corners "+bb.getPos1().toShortString()+" // "+bb.getPos2().toShortString());
//                System.out.println("minecraft box has corners ");
//            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{
            if(this.printer==null)
                this.printer=new Printer(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            this.runPrinter=false;
            this.printer=null;
        });
    }
}