/*
 * This file ("TileEntityLaserRelayEnergy.java") is part of the Actually Additions mod for Minecraft.
 * It is created and owned by Ellpeck and distributed
 * under the Actually Additions License to be found at
 * http://ellpeck.de/actaddlicense
 * View the source code at https://github.com/Ellpeck/ActuallyAdditions
 *
 * © 2015-2016 Ellpeck
 */

package de.ellpeck.actuallyadditions.mod.tile;

import de.ellpeck.actuallyadditions.api.ActuallyAdditionsAPI;
import de.ellpeck.actuallyadditions.api.laser.IConnectionPair;
import de.ellpeck.actuallyadditions.api.laser.LaserType;
import de.ellpeck.actuallyadditions.api.laser.Network;
import de.ellpeck.actuallyadditions.mod.ActuallyAdditions;
import de.ellpeck.actuallyadditions.mod.config.values.ConfigBoolValues;
import de.ellpeck.actuallyadditions.mod.util.compat.TeslaUtil;
import net.darkhax.tesla.api.ITeslaConsumer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TileEntityLaserRelayEnergy extends TileEntityLaserRelay{

    public static final int CAP = 1000;
    public final ConcurrentHashMap<EnumFacing, TileEntity> receiversAround = new ConcurrentHashMap<EnumFacing, TileEntity>();

    private final IEnergyStorage[] energyStorages = new IEnergyStorage[6];

    public TileEntityLaserRelayEnergy(String name){
        super(name, LaserType.ENERGY);

        for(int i = 0; i < this.energyStorages.length; i++){
            final EnumFacing facing = EnumFacing.values()[i];
            this.energyStorages[i] = new IEnergyStorage(){

                @Override
                public int receiveEnergy(int amount, boolean simulate){
                    return TileEntityLaserRelayEnergy.this.transmitEnergy(facing, amount, simulate);
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate){
                    return 0;
                }

                @Override
                public int getEnergyStored(){
                    return 0;
                }

                @Override
                public int getMaxEnergyStored(){
                    return TileEntityLaserRelayEnergy.this.getEnergyCap();
                }

                @Override
                public boolean canExtract(){
                    return false;
                }

                @Override
                public boolean canReceive(){
                    return true;
                }
            };
        }
    }

    public TileEntityLaserRelayEnergy(){
        this("laserRelay");
    }

    private int transmitEnergy(EnumFacing from, int maxTransmit, boolean simulate){
        int transmitted = 0;
        if(maxTransmit > 0){
            Network network = ActuallyAdditionsAPI.connectionHandler.getNetworkFor(this.pos, this.world);
            if(network != null){
                transmitted = this.transferEnergyToReceiverInNeed(from, network, maxTransmit, simulate);
            }
        }
        return transmitted;
    }

    @Override
    public IEnergyStorage getEnergyStorage(EnumFacing facing){
        return this.energyStorages[facing == null ? 0 : facing.ordinal()];
    }

    @Override
    public boolean shouldSaveDataOnChangeOrWorldStart(){
        return true;
    }

    @Override
    public void saveDataOnChangeOrWorldStart(){
        Map<EnumFacing, TileEntity> old = new HashMap<EnumFacing, TileEntity>(this.receiversAround);
        boolean change = false;

        this.receiversAround.clear();
        for(EnumFacing side : EnumFacing.values()){
            BlockPos pos = this.getPos().offset(side);
            TileEntity tile = this.world.getTileEntity(pos);
            if(tile != null && !(tile instanceof TileEntityLaserRelay)){
                if((ActuallyAdditions.teslaLoaded && tile.hasCapability(TeslaUtil.teslaConsumer, side.getOpposite())) || tile.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())){
                    this.receiversAround.put(side, tile);

                    TileEntity oldTile = old.get(side);
                    if(oldTile == null || !tile.equals(oldTile)){
                        change = true;
                    }
                }
            }
        }

        if(change){
            Network network = ActuallyAdditionsAPI.connectionHandler.getNetworkFor(this.getPos(), this.getWorld());
            if(network != null){
                network.changeAmount++;
            }
        }
    }

    private int transferEnergyToReceiverInNeed(EnumFacing from, Network network, int maxTransfer, boolean simulate){
        int transmitted = 0;
        //Keeps track of all the Laser Relays and Energy Acceptors that have been checked already to make nothing run multiple times
        List<BlockPos> alreadyChecked = new ArrayList<BlockPos>();

        List<TileEntityLaserRelayEnergy> relaysThatWork = new ArrayList<TileEntityLaserRelayEnergy>();
        int totalReceiverAmount = 0;

        for(IConnectionPair pair : network.connections){
            for(BlockPos relay : pair.getPositions()){
                if(relay != null && !alreadyChecked.contains(relay)){
                    alreadyChecked.add(relay);
                    TileEntity relayTile = this.world.getTileEntity(relay);
                    if(relayTile instanceof TileEntityLaserRelayEnergy){
                        TileEntityLaserRelayEnergy theRelay = (TileEntityLaserRelayEnergy)relayTile;
                        int amount = theRelay.receiversAround.size();
                        if(amount > 0){
                            relaysThatWork.add(theRelay);
                            totalReceiverAmount += amount;
                        }
                    }
                }
            }
        }

        if(totalReceiverAmount > 0 && !relaysThatWork.isEmpty()){
            int amountPer = maxTransfer/totalReceiverAmount;
            if(amountPer <= 0){
                amountPer = maxTransfer;
            }

            for(TileEntityLaserRelayEnergy theRelay : relaysThatWork){
                double highestLoss = Math.max(theRelay.getLossPercentage(), this.getLossPercentage());
                int lowestCap = Math.min(theRelay.getEnergyCap(), this.getEnergyCap());
                for(Map.Entry<EnumFacing, TileEntity> receiver : theRelay.receiversAround.entrySet()){
                    if(receiver != null){
                        EnumFacing side = receiver.getKey();
                        EnumFacing opp = side.getOpposite();
                        TileEntity tile = receiver.getValue();
                        if(!alreadyChecked.contains(tile.getPos())){
                            alreadyChecked.add(tile.getPos());
                            if(theRelay != this || side != from){
                                if(tile.hasCapability(CapabilityEnergy.ENERGY, opp)){
                                    IEnergyStorage cap = tile.getCapability(CapabilityEnergy.ENERGY, opp);
                                    if(cap != null){
                                        int theoreticalReceived = cap.receiveEnergy(Math.min(amountPer, lowestCap), true);
                                        if(theoreticalReceived > 0){
                                            int deduct = this.calcDeduction(theoreticalReceived, highestLoss);
                                            if(deduct >= theoreticalReceived){ //Happens with small numbers
                                                deduct = 0;
                                            }

                                            transmitted += cap.receiveEnergy(theoreticalReceived-deduct, simulate);
                                            transmitted += deduct;
                                        }
                                    }
                                }
                                else if(ActuallyAdditions.teslaLoaded && tile.hasCapability(TeslaUtil.teslaConsumer, opp)){
                                    ITeslaConsumer cap = tile.getCapability(TeslaUtil.teslaConsumer, opp);
                                    if(cap != null){
                                        int theoreticalReceived = (int)cap.givePower(Math.min(amountPer, lowestCap), true);
                                        if(theoreticalReceived > 0){
                                            int deduct = this.calcDeduction(theoreticalReceived, highestLoss);
                                            if(deduct >= theoreticalReceived){ //Happens with small numbers
                                                deduct = 0;
                                            }

                                            transmitted += cap.givePower(theoreticalReceived-deduct, simulate);
                                            transmitted += deduct;
                                        }
                                    }
                                }

                                //If everything that could be transmitted was transmitted
                                if(transmitted >= maxTransfer){
                                    return transmitted;
                                }
                            }
                        }
                    }
                }
            }
        }

        return transmitted;
    }

    private int calcDeduction(int theoreticalReceived, double highestLoss){
        return ConfigBoolValues.LASER_RELAY_LOSS.isEnabled() ? MathHelper.ceil(theoreticalReceived*(highestLoss/100)) : 0;
    }

    public int getEnergyCap(){
        return CAP;
    }

    public double getLossPercentage(){
        return 5;
    }
}
