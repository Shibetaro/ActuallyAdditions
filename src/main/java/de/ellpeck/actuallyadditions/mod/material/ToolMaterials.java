package de.ellpeck.actuallyadditions.mod.material;

import de.ellpeck.actuallyadditions.mod.items.InitItems;
import net.minecraft.item.IItemTier;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.LazyValue;

import java.util.function.Supplier;

/**
 * Complete copy paste from {@link net.minecraft.item.ItemTier}
 * <p>
 * todo: review to ensure all values act as intended
 */
public enum ToolMaterials implements IItemTier {
    QUARTZ(2, 280, 6.5f, 2.0f, 10, () -> Ingredient.fromItems(InitItems.BLACK_QUARTZ.get())),
    RESTONIA(2, 300, 7.0f, 2.25f, 12, () -> Ingredient.fromItems(InitItems.RESTONIA_CRYSTAL.get())),
    PALIS(2, 300, 7.0f, 2.25f, 12, () -> Ingredient.fromItems(InitItems.PALIS_CRYSTAL.get())),
    DIAMATINE(3, 1600, 9.0f, 4.0f, 14, () -> Ingredient.fromItems(InitItems.DIAMATINE_CRYSTAL.get())),
    VOID(2, 280, 6.0f, 2.0f, 8, () -> Ingredient.fromItems(InitItems.VOID_CRYSTAL.get())),
    EMERADIC(4, 2200, 9.5f, 5.55f, 18, () -> Ingredient.fromItems(InitItems.EMERADIC_CRYSTAL.get())),
    ENORI(2, 280, 6.25f, 6.25f, 15, () -> Ingredient.fromItems(InitItems.ENORI_CRYSTAL.get()));

    private final int harvestLevel;
    private final int maxUses;
    private final float efficiency;
    private final float attackDamage;
    private final int enchantability;
    private final LazyValue<Ingredient> repairMaterial;

    ToolMaterials(int harvestLevelIn, int maxUsesIn, float efficiencyIn, float attackDamageIn, int enchantabilityIn, Supplier<Ingredient> repairMaterialIn) {
        this.harvestLevel = harvestLevelIn;
        this.maxUses = maxUsesIn;
        this.efficiency = efficiencyIn;
        this.attackDamage = attackDamageIn;
        this.enchantability = enchantabilityIn;
        this.repairMaterial = new LazyValue<>(repairMaterialIn);
    }

    @Override
    public int getMaxUses() {
        return this.maxUses;
    }

    @Override
    public float getEfficiency() {
        return this.efficiency;
    }

    @Override
    public float getAttackDamage() {
        return this.attackDamage;
    }

    @Override
    public int getHarvestLevel() {
        return this.harvestLevel;
    }

    @Override
    public int getEnchantability() {
        return this.enchantability;
    }

    @Override
    public Ingredient getRepairMaterial() {
        return this.repairMaterial.getValue();
    }
}
