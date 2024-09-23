package org.rockerle.airprinter.airprinter.client;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Printer {
    private MinecraftClient mc;
    private SchematicPlacement loadedSchematic;// = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
    private int range = 3;
    private int blocksPerTick = 1;
    private Direction specificPlacement;
    private List<BlockPos> placed;
    public Printer(MinecraftClient mcClient){
        this.mc=mcClient;
        this.placed=new ArrayList<>();
    }

//    print routine
//    1. check for next missing blockpos
//    (1.1 check if next block needs a support block)
//    (1.2 check if support block can be placed -> skip both blocks if not)
//    2. check for block in inventory
//    3. either skip or select+rotate+place the block
    public void tick(){
        if(this.loadedSchematic==null)
            this.loadedSchematic = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        Pair<BlockPos,BlockState> next = nearestMissingBlock();
        if(next!=null){
            if(!mc.player.getMainHandStack().isOf(next.getRight().getBlock().asItem())){
                selectItem(next.getRight().getBlock().asItem().getDefaultStack());
                return;
            }

            BlockPos bP = new BlockPos(next.getLeft());
//            List<Property<?>> props = SchematicWorldHandler.getSchematicWorld().getBlockState(bP).getProperties().stream().filter(p -> next.getRight().contains(p)).toList();
//            if(!props.isEmpty()){
//                System.out.println("Found some Block Properties: "+props.toString());
            rotate(SchematicWorldHandler.getSchematicWorld().getBlockState(bP));

//            System.out.println("sending interact packet");
            placeBlock(bP);
            placed.add(bP);
            }else{
//                System.out.println("Next closest ");
            }
        if(mc.player.age%5==0 && !placed.isEmpty())
            placed.remove(0);
    }

    public void reset(){
        System.out.println("HUHH? Printer needs a reset????");
    }

    private void selectItem(ItemStack iS){
        int itemSlot = mc.player.getInventory().getSlotWithStack(iS);
        System.out.println("Selecting "+iS.getItem().getName());
        if(itemSlot>=9){
            mc.interactionManager.pickFromInventory(itemSlot);
            mc.player.getInventory().updateItems();
        }
        mc.player.getInventory().selectedSlot = mc.player.getInventory().getSlotWithStack(iS);
    }

    private void rotate(BlockState bS){
        AtomicBoolean alreadyRightRotation = new AtomicBoolean(false);
        AtomicReference<Float> yaw = new AtomicReference<>(mc.player.lastRenderYaw);
        AtomicReference<Float> pitch = new AtomicReference<>(mc.player.lastRenderPitch);
        System.out.print("Props: ");
        bS.getProperties().stream().filter(property->bS.contains(property)).forEach(p->{
            System.out.print(p.getName()+";");
            switch(p.getName()){
                case "axis":{
                    System.out.println("BlockState has propertie Axis with values "+p.getValues());
                    Direction.Axis axis = bS.get(Properties.AXIS);
                    /*switch(bS.get(Properties.AXIS)){
                        case X -> {
                            System.out.println("X-Axis to be found true");
                            pitch.set(dirToPitch(Direction.from(Direction.Axis.X, Direction.AxisDirection.POSITIVE)));
                            this.specificPlacement = Direction.from(Direction.Axis.X, Direction.AxisDirection.POSITIVE);
                        }
                        case Y -> {
                            System.out.println("Y-Axis to be found true");yaw.set(dirToYaw(Direction.DOWN));}
                        case Z -> {
                            System.out.println("Z-Axis to be found true");
                            pitch.set(dirToPitch(Direction.from(Direction.Axis.Z, Direction.AxisDirection.POSITIVE)));
                            this.specificPlacement = Direction.from(Direction.Axis.Z, Direction.AxisDirection.POSITIVE);
                        }
                    }*/
                    float neccessaryPitch = dirToPitch(Direction.from(axis, Direction.AxisDirection.POSITIVE));
//                    if(Float.compare(mc.player.getPitch(),neccessaryPitch)==0) {
//                        alreadyRightRotation.set(true);
//                        break;
//                    }
                    pitch.set(neccessaryPitch);
                    this.specificPlacement = Direction.from(axis, Direction.AxisDirection.POSITIVE);
                    break;
                }
                case "facing":{
                    System.out.println("BlockState "+bS.getBlock().getName().getString()+" has property facing with values "+p.getValues());
                    float neccessaryPitch = dirToPitch(bS.get(TrapdoorBlock.FACING).getOpposite());
//                    if(Float.compare(mc.player.getPitch(),neccessaryPitch)==0) {
//                        alreadyRightRotation.set(true);
//                        break;
//                    }
                    pitch.set(dirToPitch(bS.get(TrapdoorBlock.FACING).getOpposite()));
                    this.specificPlacement = bS.get(TrapdoorBlock.FACING).getOpposite();
                    System.out.println("Set pitch to direction: "+this.specificPlacement.toString());
                    break;
                }
                case "half":{
                    System.out.println("BlockState "+bS.getBlock().getName().getString()+" has property half with values "+p.getValues());
                    switch(bS.get(Properties.BLOCK_HALF)) {
                        case TOP -> {
                            yaw.set(dirToYaw(Direction.DOWN));
                            this.specificPlacement = Direction.DOWN;
                        }
                        case BOTTOM -> {
                            this.specificPlacement = Direction.UP;
                        }
                    }
                    break;
                    }
                default: {
                    System.out.println("No interesting rotation dependend features found");
//                    pitch.set(0.0f);
//                    yaw.set(mc.player.lastRenderYaw);
                }
            }
        });
        System.out.println("Trying to fake rotate to yaw{"+yaw.get()+"} and pitch{"+pitch.get()+"}");
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw.get(), pitch.get(), mc.player.isOnGround()));
    }
    private void placeBlock(BlockPos bP){
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(bP),
                        specificPlacement==null?Direction.UP:specificPlacement,
                        bP,
                        true),
                1
        ));
        this.specificPlacement = null;
    }
    @Nullable
    private Pair<BlockPos, BlockState> nearestMissingBlock(){
        BlockPos res = null;
        for(int x=-this.range;x<this.range;x++){
            for(int z=-this.range;z<this.range;z++){
                for(int y=-this.range;y<this.range;y++){
                    res = mc.player.getBlockPos().add(x,y,z);
                    if(this.placed.contains(res)){
                        continue;
                    }
                    Box schemBox = loadedSchematic.getEclosingBox();
                    if(schemBox==null) {
                        loadedSchematic.toggleRenderEnclosingBox();
                        loadedSchematic.toggleRenderEnclosingBox();
                        schemBox = loadedSchematic.getEclosingBox();
                    }
                    if(!boundingBoxContains(schemBox,res.getX(),res.getY(),res.getZ())){
                        continue;
                    }
                    Block specialCheck = SchematicWorldHandler.getSchematicWorld().getBlockState(res).getBlock();
                    if(specialCheck instanceof DyedCarpetBlock){
                        if(mc.world.getBlockState(res.offset(Direction.DOWN)).isAir())
                            continue;
                    }else if(specialCheck instanceof CactusBlock){
                        if(!mc.world.getBlockState(res.offset(Direction.DOWN)).isOf(Blocks.SAND))
                            continue;
                    }
                    if(!SchematicWorldHandler.getSchematicWorld().getBlockState(res).equals(Blocks.AIR.getDefaultState())
                        && mc.world.getBlockState(res).isAir()){

                        System.out.println("Need to place "+res.toShortString());
                        BlockState bS = SchematicWorldHandler.getSchematicWorld().getBlockState(res);
                        System.out.println("Currently is: "+mc.world.getBlockState(res).toString()+ " , but needs to be "+bS.toString());
                        if(mc.player.getInventory().contains(bS.getBlock().asItem().getDefaultStack()))
                            return new Pair(res,bS);
                    }
                }
            }
        }
        return null;
    }

    private boolean boundingBoxContains(Box bb, double x, double y, double z) {
        int minX = Math.min(bb.getPos1().getX(),bb.getPos2().getX());
        int minY = Math.min(bb.getPos1().getY(),bb.getPos2().getY());
        int minZ = Math.min(bb.getPos1().getZ(),bb.getPos2().getZ());
        int maxX = Math.max(bb.getPos1().getX(),bb.getPos2().getX());
        int maxY = Math.max(bb.getPos1().getY(),bb.getPos2().getY());
        int maxZ = Math.max(bb.getPos1().getZ(),bb.getPos2().getZ());
        return x >= minX &&
                x <= maxX &&
                y >= minY &&
                y <= maxY &&
                z >= minZ &&
                z <= maxZ;
    }

    private float dirToYaw(Direction d){
        System.out.println("setting dir "+d.toString()+" to yaw");
        return switch (d) {
            case NORTH -> 180.0f;
            case EAST -> 270.0f;
            case SOUTH -> 0.0f;
            case WEST -> 90.0f;
            default -> mc.player.getYaw();
        };
    }
    private float dirToPitch(Direction d){
        System.out.println("setting dir "+d.toString()+" to pitch");
        return switch (d) {
            case UP -> -90.0f;
            case DOWN -> 90.0f;
            default -> mc.player.getPitch();
        };
    }
}