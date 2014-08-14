package tterrag.supermassivetech.common.entity.item;

import static tterrag.supermassivetech.SuperMassiveTech.*;

import java.util.LinkedList;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import tterrag.supermassivetech.common.network.PacketHandler;
import tterrag.supermassivetech.common.network.message.MessageStarHeartParticle;
import tterrag.supermassivetech.common.registry.Achievements;
import tterrag.supermassivetech.common.util.BlockCoord;
import tterrag.supermassivetech.common.util.Utils;
import cpw.mods.fml.common.ObfuscationReflectionHelper;

public class EntityItemStarHeart extends EntityItemIndestructible
{
    public EntityItemStarHeart(World world)
    {
        super(world);
    }

    public EntityItemStarHeart(World world, double posX, double posY, double posZ, ItemStack itemstack, double motionX, double motionY, double motionZ, int delay)
    {
        super(world, posX, posY, posZ, itemstack, motionX, motionY, motionZ, delay);
    }

    private boolean ready;
    private int explodeTimer = -1, particlesLeft = 0, powerLevel, postTimer = 20;
    private final int TIMER_MAX = 60, RADIUS = 10;
    private BlockCoord toRemove = null;

    private LinkedList<BlockCoord> fire = new LinkedList<BlockCoord>();

    @Override
    public void onUpdate()
    {
        super.onUpdate();

        if (worldObj.isRemote)
        {
            return;
        }

        if (explodeTimer < 0)
        {
            if (this.isBurning() && ready)
            {
                explodeTimer = TIMER_MAX;
            }
            else
                ready = !this.isBurning();
        }
        else if (explodeTimer == TIMER_MAX)
        {
            getFire();
            delayBeforeCanPickup = 1000000;
            explodeTimer--;
            powerLevel = fire.size();
        }
        else if (explodeTimer == 0)
        {
            if (!fire.isEmpty())
                explodeTimer++;
            else if (postTimer > 0)
                postTimer--;
            else
                changeToStar();
        }

        if (explodeTimer > 0 && explodeTimer < TIMER_MAX)
        {
            if (fire.size() > 0)
            {
                if (particlesLeft <= 0)
                {
                    toRemove = fire.remove(new Random().nextInt(fire.size()));

                    particlesLeft = extinguish(toRemove) ? 4 + new Random().nextInt(2) - 1 : 0;
                    explodeTimer--;
                }
                else
                {
                    sendParticlePacket(toRemove.x, toRemove.y, toRemove.z);
                    particlesLeft--;
                }
            }
            else
            {
                explodeTimer = 0;
            }
        }
    }

    private void sendParticlePacket(int x, int y, int z)
    {
        PacketHandler.INSTANCE.sendToAll(new MessageStarHeartParticle((int) posX, (int) posY, (int) posZ, x, y, z));
    }

    private void changeToStar()
    {
        ItemStack star = new ItemStack(itemRegistry.star, this.getEntityItem().stackSize);

        // Sets the type of the star to a random type
        Utils.setType(star, starRegistry.getRandomStarFromType(starRegistry.getWeightedCreationTier(powerLevel)));

        worldObj.newExplosion(this, posX, posY, posZ, 3.0f + (this.getEntityItem().stackSize), true, true);

        EntityItemIndestructible starEntity = new EntityItemIndestructible(worldObj, posX, posY, posZ, star, 0, 0, 0, 0);
        EntityItemIndestructible depletedEntity = new EntityItemDepletedNetherStar(worldObj, posX, posY, posZ, new ItemStack(itemRegistry.depletedNetherStar, star.stackSize),
                0, 0, 0, 0);

        starEntity.func_145799_b(this.func_145800_j());
        depletedEntity.func_145799_b(this.func_145800_j());

        Utils.spawnItemInWorldWithRandomMotion(starEntity);
        Utils.spawnItemInWorldWithRandomMotion(depletedEntity);

        Achievements.unlock(Achievements.getValidItemStack(star), (EntityPlayerMP) worldObj.getPlayerEntityByName(this.func_145800_j()));

        this.setDead();
    }

    @Override
    public boolean isBurning()
    {
        boolean flag = this.worldObj != null && this.worldObj.isRemote;
        // TODO PR forge or AT
        Integer fire = ObfuscationReflectionHelper.getPrivateValue(Entity.class, this, "fire", "field_70151_c");
        return (fire > 0 || flag && this.getFlag(0));
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2)
    {
        if (par1DamageSource.isFireDamage())
            return false;
        else
            return super.attackEntityFrom(par1DamageSource, par2);
    }

    @SuppressWarnings("unused")
    private boolean isInValidState()
    {
        for (int i = -1; i <= 1; i++)
        {
            for (int j = -1; j <= 1; j++)
            {
                if (i != 0)
                {
                    if (worldObj.getBlock((int) posX + i, (int) posY, (int) posZ + j) != Blocks.fire)
                        return false;
                }
            }
        }

        return true;
    }

    private void getFire()
    {
        for (int x = -RADIUS; x <= RADIUS; x++)
        {
            for (int y = -RADIUS; y <= RADIUS; y++)
            {
                for (int z = -RADIUS; z <= RADIUS; z++)
                {
                    if (worldObj.getBlock((int) posX + x, (int) posY + y, (int) posZ + z) == Blocks.fire)
                    {
                        fire.add(new BlockCoord((int) posX + x, (int) posY + y, (int) posZ + z));
                    }
                }
            }
        }
    }

    private boolean extinguish(BlockCoord coord)
    {
        if (worldObj.getBlock(toRemove.x, toRemove.y, toRemove.z) == Blocks.fire)
        {
            worldObj.setBlockToAir(toRemove.x, toRemove.y, toRemove.z);
            return true;
        }
        return false;
    }
}