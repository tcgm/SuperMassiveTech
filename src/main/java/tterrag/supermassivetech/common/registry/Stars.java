package tterrag.supermassivetech.common.registry;

import static tterrag.supermassivetech.common.registry.Stars.StarTier.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import tterrag.supermassivetech.api.common.registry.IStar;
import tterrag.supermassivetech.common.util.Utils;

public class Stars
{
    private static int nextStarID = 0;
    public static Stars instance = new Stars();
    public static final String PREFIX = "SMT.star.";

    public enum StarTier
    {
        LOW("tooltip.tier.low"), NORMAL("tooltip.tier.normal"), HIGH("tooltip.tier.high"),

        /**
         * Will not be created by the normal means, nor will it have an ItemStar
         * listed in the game, you must create it in your own class
         */
        SPECIAL("tooltip.tier.special");

        private String name;

        StarTier(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return Utils.localize(name, true);
        }
    }

    public static EnumChatFormatting getEnumColor(StarTier tier)
    {
        switch (tier)
        {
        case LOW:
            return EnumChatFormatting.RED;
        case NORMAL:
            return EnumChatFormatting.GOLD;
        case HIGH:
            return EnumChatFormatting.GREEN;
        case SPECIAL:
            return EnumChatFormatting.AQUA;
        }
        return EnumChatFormatting.OBFUSCATED;
    }

    /**
     * Defines a new type of star, all registered star types will have their own
     * item and a chance to be created when a star heart is used
     * 
     * @author Garrett Spicer-Davis
     */
    private class StarType implements IStar
    {
        public final String name;
        private int id;
        private int color, powerMax, powerPerTick, fuse, mass;
        private EnumChatFormatting textColor;
        StarTier tier;

        /**
         * Creates a new <code>StarType</code> object
         * 
         * @param name - full unlocalized name of the star type
         * @param tier - {@link StarTier} of this star, low &lt; normal &lt;
         *            high, in terms of value
         * @param color - hex color value
         * @param textColor - {@link EnumChatFormatting} to use in tooltips
         * @param powerMax - max amount of power "stored" in this star
         * @param powerPerTick - max power per tick (in RF) this provides in a
         *            star harvester
         * @param fuse - amount of time before exploding once it goes critical
         */
        public StarType(String name, StarTier tier, int color, EnumChatFormatting textColor, int powerMax, int powerPerTick, int fuse)
        {
            this.name = name;
            id = nextStarID++;
            this.tier = tier;
            this.color = color;
            this.textColor = textColor;
            this.powerMax = powerMax;
            this.powerPerTick = powerPerTick;
            this.fuse = fuse;
            this.mass = tier.ordinal();
        }

        /**
         * Simply returns the name of this star, localized
         */
        @Override
        public String toString()
        {
            return Utils.localize(name);
        }

        @Override
        public String getName()
        {
            return name.replace(PREFIX, "");
        }

        /**
         * The automatically assigned numerical ID of this star, used for
         * ordering purposes
         */
        @Override
        public int getID()
        {
            return id;
        }

        @Override
        public int getColor()
        {
            return color;
        }

        @Override
        public EnumChatFormatting getTextColor()
        {
            return textColor;
        }

        @Override
        public int getPowerPerTick()
        {
            return powerPerTick;
        }

        @Override
        public int getFuse()
        {
            return fuse;
        }

        @Override
        public StarTier getTier()
        {
            return this.tier;
        }

        @Override
        public int getMassLevel()
        {
            return mass;
        }

        public StarType setMassLevel(int level)
        {
            this.mass = Math.max(0, level - 1);
            return this;
        }

        @Override
        public int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate)
        {
            return 0; // Oh how I wish I could use java 8
        }

        @Override
        public int extractEnergy(ItemStack stack, int maxExtract, boolean simulate)
        {
            int stored = getEnergyStored(stack);
            int max = Math.min(maxExtract, getPowerPerTick());
            
            if (stack.stackTagCompound != null)
            {
                if (max >= stored)
                {
                    if (simulate) return stored;
                    stack.stackTagCompound.setInteger("energy", 0);
                    return stored;
                }
                else
                {
                    if (simulate) return max;
                    stack.stackTagCompound.setInteger("energy", stored - max);
                    return max;
                }
            }
            return 0;
        }

        @Override
        public int getEnergyStored(ItemStack stack)
        {
            return stack.getTagCompound() == null ? 0 : stack.getTagCompound().getInteger("energy");
        }

        @Override
        public int getMaxEnergyStored(ItemStack stack)
        {
            return powerMax;
        }
    }

    public static int getNextStarID()
    {
        return nextStarID++;
    }

    public HashMap<Integer, IStar> types = new HashMap<Integer, IStar>();

    /**
     * Register a new star type, adds it to the items and creation chance
     * 
     * @param type - {@link StarType} object to add
     */
    public void registerStarType(IStar type)
    {
        types.put(type.getID(), type);
    }

    /**
     * Gets the {@link StarType} object (must be registered) with this ID
     */
    public IStar getTypeByID(int ID)
    {
        return types.get(ID);
    }

    /**
     * Gets the ID of the passed {@link StarType} object
     */
    public int getIDByType(StarType type)
    {
        return type.getID();
    }

    /**
     * Finds a {@link StarType} object by its <code>String</code> name
     * 
     * @param name
     * @return
     */
    public IStar getTypeByName(String name)
    {
        for (IStar s : types.values())
        {
            if (s.getName().equals(name) || s.toString().equals(name))
                return s;
        }
        return null;
    }

    public StarTier getWeightedCreationTier(int offset)
    {
        int tier = new Random().nextInt(Math.max(0, 1000 - (offset * 2)));
        if (tier < 10)
            return HIGH;
        else if (tier < 100)
            return NORMAL;
        else
            return LOW;
    }

    public ArrayList<IStar> getStarsOfTier(StarTier tier)
    {
        ArrayList<IStar> list = new ArrayList<IStar>();
        for (IStar i : types.values())
        {
            if (i.getTier() == tier)
                list.add(i);
        }
        return list;
    }

    public IStar getRandomStarFromType(StarTier tier)
    {
        List<IStar> list = getStarsOfTier(tier);
        return list.get(new Random().nextInt(list.size()));
    }

    public void registerDefaultStars()
    {
        // TODO balance
        registerStarType(new StarType(PREFIX + "yellowDwarf", LOW, 0xd2df00, EnumChatFormatting.YELLOW, 100000000, 80, 600));
        registerStarType(new StarType(PREFIX + "redDwarf", NORMAL, 0xcf3702, EnumChatFormatting.RED, 200000000, 40, 600));
        registerStarType(new StarType(PREFIX + "redGiant", LOW, 0xcf0202, EnumChatFormatting.RED, 50000000, 40, 400));
        registerStarType(new StarType(PREFIX + "blueGiant", NORMAL, 0x314eff, EnumChatFormatting.DARK_BLUE, 400000000, 20, 400));
        registerStarType(new StarType(PREFIX + "supergiant", HIGH, 0xffea59, EnumChatFormatting.WHITE, 1000000000, 160, 1200));
        registerStarType(new StarType(PREFIX + "brownDwarf", LOW, 0xb0752f, EnumChatFormatting.GRAY, 25000000, 20, 2400));
        registerStarType(new StarType(PREFIX + "whiteDwarf", LOW, 0xf5f6c5, EnumChatFormatting.WHITE, 50000000, 160, 1200));
        registerStarType(new StarType(PREFIX + "neutron", SPECIAL, 0x89fbff, EnumChatFormatting.AQUA, Integer.MAX_VALUE, 15, 1).setMassLevel(2));
        registerStarType(new StarType(PREFIX + "pulsar", SPECIAL, 0xbd28cf, EnumChatFormatting.DARK_PURPLE, Integer.MAX_VALUE, 20, 1).setMassLevel(2));

        /*
         * - pulsars are neutron stars, neutrons are formed INSTEAD of black
         * holes. - a critical star could have a chance of forming either of
         * these two, OR a black hole. - the advantage of these would be a low
         * power output for an infinite time (think RTG from IC2). - however,
         * due to their unstable nature, stacking either of these items would
         * cause a catastrophic explosion resulting in a black hole...probably
         * obliterating whatever was holding the items.
         */
    }
}