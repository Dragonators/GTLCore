package org.gtlcore.gtlcore.data.recipe;

import org.gtlcore.gtlcore.GTLCore;
import org.gtlcore.gtlcore.api.machine.multiblock.GTLCleanroomType;
import org.gtlcore.gtlcore.common.data.GTLItems;
import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.common.data.machines.AdditionalMultiBlockMachine;
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine;
import org.gtlcore.gtlcore.common.recipe.condition.GravityCondition;
import org.gtlcore.gtlcore.config.ConfigHolder;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.data.chemical.material.stack.UnificationEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.common.data.*;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.povstalec.sgjourney.common.init.BlockInit;

import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import committee.nova.mods.avaritia.init.registry.ModItems;

import java.util.function.Consumer;

import static appeng.core.definitions.AEItems.*;
import static com.glodblock.github.extendedae.common.EPPItemAndBlock.*;
import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTItems.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;
import static net.povstalec.sgjourney.common.init.ItemInit.*;
import static org.gtlcore.gtlcore.common.data.GTLBlocks.*;
import static org.gtlcore.gtlcore.common.data.GTLItems.*;
import static org.gtlcore.gtlcore.common.data.GTLMaterials.*;
import static org.gtlcore.gtlcore.common.data.GTLRecipeTypes.*;
import static org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine.*;
import static org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA.STAR_ULTIMATE_MATERIAL_FORGE_FACTORY;
import static org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineB.PRIMITIVE_VOID_ORE;
import static org.gtlcore.gtlcore.utils.Registries.getItemStack;

public class Misc {

    public static void init(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addShapelessRecipe(provider, "structure_detect", GTLItems.STRUCTURE_DETECT.asStack(),
                "A", Items.PAPER);
        VanillaRecipeHelper.addShapelessRecipe(provider, GTLCore.id("me_pattern_buffer_copy"),
                GTLItems.ME_PATTERN_BUFFER_COPY.asStack(),
                "A", MEMORY_CARD);
        VanillaRecipeHelper.addShapedRecipe(provider, true, GTLCore.id("fast_infinity_cell_0"),
                GTLItems.FAST_INFINITY_CELL.asStack(),
                "AAA", "BCD", "AAA",
                'A', QUANTUM_ENTANGLED_SINGULARITY.stack(),
                'B', GTLItems.ITEM_INFINITY_CELL.asStack(),
                'C', ZERO_POINT_MODULE.asStack(),
                'D', GTLItems.FLUID_INFINITY_CELL.asStack());
        VanillaRecipeHelper.addShapedRecipe(provider, true, GTLCore.id("fast_infinity_cell_1"),
                GTLItems.FAST_INFINITY_CELL.asStack(),
                "AAA", "DCB", "AAA",
                'A', QUANTUM_ENTANGLED_SINGULARITY.stack(),
                'B', GTLItems.ITEM_INFINITY_CELL.asStack(),
                'C', ZERO_POINT_MODULE.asStack(),
                'D', GTLItems.FLUID_INFINITY_CELL.asStack());
        if (ConfigHolder.INSTANCE.enablePrimitiveVoidOre) {
            VanillaRecipeHelper.addShapedRecipe(provider, true, "primitive_void_ore_recipes",
                    PRIMITIVE_VOID_ORE.asStack(), "DCD", "CGC", "DCD",
                    'D', Blocks.DIRT.asItem(),
                    'C', Items.STONE_PICKAXE.asItem(),
                    'G', new UnificationEntry(TagPrefix.block, GTMaterials.Iron));
            PRIMITIVE_VOID_ORE_RECIPES.recipeBuilder("primitive_void_ore_recipes")
                    .inputFluids(GTMaterials.Steam.getFluid(1000))
                    .duration(200)
                    .save(provider);
        }

        VanillaRecipeHelper.addShapelessRecipe(provider, "simulation_machine",
                AdvancedMultiBlockMachine.SIMULATION_MACHINE.asStack(), "A", Blocks.STONE);

        VanillaRecipeHelper.addShapedRecipe(provider, true, GTLCore.id("creative_chest"),
                GTMachines.CREATIVE_ITEM.asStack(),
                " A ", " B ", "C D",
                'A', ModItems.neutron_ring.get(),
                'B', BlockInit.UNIVERSE_STARGATE.get().asItem(),
                'C', ModItems.infinity_ring.get(),
                'D', ModItems.infinity_umbrella.get());

        WOOD_DISTILLATION_RECIPES.recipeBuilder("wood_distillation_recipes")
                .inputItems(ItemTags.LOGS, 16)
                .inputFluids(Nitrogen.getFluid(1000))
                .outputItems(dust, DarkAsh, 8)
                .outputFluids(Water.getFluid(800))
                .outputFluids(Carbon.getFluid(490))
                .outputFluids(Methanol.getFluid(480))
                .outputFluids(Benzene.getFluid(350))
                .outputFluids(CarbonMonoxide.getFluid(340))
                .outputFluids(Creosote.getFluid(300))
                .outputFluids(Dimethylbenzene.getFluid(240))
                .outputFluids(AceticAcid.getFluid(160))
                .outputFluids(Methane.getFluid(130))
                .outputFluids(Acetone.getFluid(80))
                .outputFluids(Phenol.getFluid(75))
                .outputFluids(Toluene.getFluid(75))
                .outputFluids(Ethylene.getFluid(20))
                .outputFluids(Hydrogen.getFluid(20))
                .outputFluids(MethylAcetate.getFluid(16))
                .outputFluids(Ethanol.getFluid(16))
                .duration(200).EUt(VA[MV])
                .save(provider);

        AUTOCLAVE_RECIPES.recipeBuilder("water_agar_mix").EUt(VA[HV]).duration(600)
                .inputItems(dust, Gelatin)
                .inputFluids(DistilledWater.getFluid(1000))
                .outputFluids(WaterAgarMix.getFluid(1000))
                .cleanroom(CleanroomType.STERILE_CLEANROOM)
                .save(provider);

        DEHYDRATOR_RECIPES.recipeBuilder("agar")
                .inputFluids(WaterAgarMix.getFluid(1000))
                .outputItems(dust, Agar, 1)
                .duration(420).EUt(VA[MV])
                .cleanroom(CleanroomType.STERILE_CLEANROOM)
                .save(provider);

        CHEMICAL_RECIPES.recipeBuilder("soda_ash_from_carbon_dioxide")
                .circuitMeta(2)
                .inputItems(dust, SodiumHydroxide, 6)
                .inputFluids(CarbonDioxide.getFluid(1000))
                .outputItems(dust, SodaAsh, 6)
                .outputFluids(Water.getFluid(1000))
                .duration(80).EUt(VA[HV])
                .save(provider);

        BLAST_RECIPES.recipeBuilder("engraved_crystal_chip_from_emerald")
                .inputItems(plate, Emerald)
                .inputItems(RAW_CRYSTAL_CHIP)
                .inputFluids(Helium.getFluid(1000))
                .outputItems(ENGRAVED_CRYSTAL_CHIP)
                .blastFurnaceTemp(5000)
                .duration(900).EUt(VA[HV])
                .addCondition(new GravityCondition(true))
                .save(provider);

        BLAST_RECIPES.recipeBuilder("engraved_crystal_chip_from_olivine")
                .inputItems(plate, Olivine)
                .inputItems(RAW_CRYSTAL_CHIP)
                .inputFluids(Helium.getFluid(1000))
                .outputItems(ENGRAVED_CRYSTAL_CHIP)
                .blastFurnaceTemp(5000)
                .duration(900).EUt(VA[HV])
                .addCondition(new GravityCondition(true))
                .save(provider);

        CHEMICAL_BATH_RECIPES.recipeBuilder("quantum_star")
                .inputItems(gem, NetherStar)
                .inputFluids(Radon.getFluid(1250))
                .outputItems(QUANTUM_STAR)
                .duration(1920).EUt(VA[HV])
                .addCondition(new GravityCondition(true))
                .save(provider);

        AUTOCLAVE_RECIPES.recipeBuilder("gravi_star")
                .inputItems(QUANTUM_STAR)
                .inputFluids(Neutronium.getFluid(L * 2))
                .outputItems(GRAVI_STAR)
                .duration(480).EUt(VA[IV])
                .addCondition(new GravityCondition(true))
                .save(provider);

        CHEMICAL_BATH_RECIPES.recipeBuilder("quantum_eye")
                .inputItems(gem, EnderEye)
                .inputFluids(Radon.getFluid(250))
                .outputItems(QUANTUM_EYE)
                .duration(480).EUt(VA[HV])
                .addCondition(new GravityCondition(true))
                .save(provider);

        LIGHTNING_PROCESSOR_RECIPES.recipeBuilder("ender_pearl_dust").duration(400).EUt(VA[LV])
                .inputItems(dust, Beryllium)
                .inputItems(dust, Potassium, 4)
                .inputFluids(Nitrogen.getFluid(5000))
                .circuitMeta(1)
                .outputItems(dust, EnderPearl, 10)
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder("molecular_assembler_matrix")
                .inputItems(frameGt, NaquadahAlloy, 16)
                .inputItems(FIELD_GENERATOR_UV, 8)
                .inputItems(ULTIMATE_BATTERY)
                .inputItems(CustomTags.UHV_CIRCUITS, 16)
                .inputItems(wireFine, RutheniumTriniumAmericiumNeutronate, 64)
                .inputItems(wireFine, RutheniumTriniumAmericiumNeutronate, 64)
                .inputItems(plate, AbyssalAlloy, 16)
                .inputItems(EX_INTERFACE.asItem(), 8)
                .inputItems(INGREDIENT_BUFFER.asItem(), 8)
                .inputItems(EX_IO_PORT.asItem(), 8)
                .inputItems(EX_PATTERN_PROVIDER.asItem(), 8)
                .inputItems(EX_ASSEMBLER.asItem(), 8)
                .inputFluids(MutatedLivingSolder.getFluid(2304))
                .inputFluids(Highurabilityompoundteel.getFluid(1728))
                .inputFluids(Antimatter.getFluid(4000))
                .inputFluids(Polyetheretherketone.getFluid(2340))
                .outputItems(AdditionalMultiBlockMachine.MOLECULAR_ASSEMBLER_MATRIX)
                .EUt(VA[9]).duration(200)
                .stationResearch((b) -> b.researchStack(EX_ASSEMBLER.asItem().getDefaultInstance())
                        .dataStack(GTItems.TOOL_DATA_MODULE.asStack()).EUt(VA[9]).CWUt(256))
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder("me_molecular_assembler_io")
                .inputItems(frameGt, Neutronium, 16)
                .inputItems(SENSOR_UV, 8)
                .inputItems(EMITTER_UV, 8)
                .inputItems(CustomTags.UHV_CIRCUITS, 4)
                .inputItems(EX_INTERFACE.asItem(), 32)
                .inputItems(INGREDIENT_BUFFER.asItem(), 32)
                .inputItems(EX_IO_PORT.asItem(), 32)
                .inputItems(QUANTUM_ENTANGLED_SINGULARITY.asItem(), 4)
                .inputItems(SPATIAL_128_CELL_COMPONENT.asItem(), 16)
                .inputFluids(MutatedLivingSolder.getFluid(1152))
                .inputFluids(Naquadria.getFluid(1728))
                .inputFluids(Plutonium241.getFluid(2340))
                .inputFluids(Mithril.getFluid(2340))
                .outputItems(GTLMachines.GTAEMachines.ME_MOLECULAR_ASSEMBLER_IO)
                .EUt(VA[9]).duration(200)
                .stationResearch((b) -> b.researchStack(EX_IO_PORT.asItem().getDefaultInstance())
                        .dataStack(GTItems.TOOL_DATA_MODULE.asStack()).EUt(VA[9]).CWUt(128))
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder("me_craft_speed_core")
                .inputItems(GTMachines.WORLD_ACCELERATOR[UV], 2)
                .inputItems(FLUID_REGULATOR_UV, 8)
                .inputItems(CONVEYOR_MODULE_UV, 8)
                .inputItems(Registries.getItemStack("kubejs:bioware_processing_core", 4))
                .inputItems(SPEED_CARD.asItem(), 64)
                .inputItems(SINGULARITY.asItem(), 64)
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.RED, 64))
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.RED, 64))
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.RED, 64))
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.RED, 64))
                .inputFluids(MutatedLivingSolder.getFluid(1152))
                .inputFluids(Nobelium.getFluid(1728))
                .inputFluids(Orichalcum.getFluid(2340))
                .inputFluids(Mithril.getFluid(2340))
                .outputItems(GTLMachines.GTAEMachines.ME_CRAFT_SPEED_CORE)
                .EUt(VA[9]).duration(200)
                .stationResearch((b) -> b.researchStack(GTMachines.WORLD_ACCELERATOR[UV].asStack())
                        .dataStack(GTItems.TOOL_DATA_MODULE.asStack()).EUt(VA[9]).CWUt(128))
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder("me_craft_parallel_core")
                .inputItems(ELECTRIC_MOTOR_UV, 16)
                .inputItems(FIELD_GENERATOR_UV, 4)
                .inputItems(Registries.getItemStack("kubejs:bioware_processing_core", 4))
                .inputItems(EX_ASSEMBLER.asItem(), 64)
                .inputItems(CRAFTING_CARD.asItem(), 64)
                .inputItems(SINGULARITY.asItem(), 64)
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.PURPLE, 64))
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.PURPLE, 64))
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.PURPLE, 64))
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.PURPLE, 64))
                .inputFluids(MutatedLivingSolder.getFluid(1152))
                .inputFluids(Neptunium.getFluid(1728))
                .inputFluids(Orichalcum.getFluid(2340))
                .inputFluids(Mithril.getFluid(2340))
                .outputItems(GTLMachines.GTAEMachines.ME_CRAFT_PARALLEL_CORE)
                .EUt(VA[9]).duration(200)
                .stationResearch((b) -> b.researchStack(AEBlocks.MOLECULAR_ASSEMBLER.stack())
                        .dataStack(GTItems.TOOL_DATA_MODULE.asStack()).EUt(VA[9]).CWUt(128))
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder("me_craft_pattern_container")
                .inputItems(GTMachines.HULL[UV])
                .inputItems(FIELD_GENERATOR_UV, 4)
                .inputItems(CustomTags.UV_CIRCUITS)
                .inputItems(EX_PATTERN_PROVIDER.asItem(), 64)
                .inputItems(CAPACITY_CARD.asItem(), 64)
                .inputItems(SINGULARITY.asItem(), 64)
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.BLUE, 64))
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.BLUE, 64))
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.BLUE, 64))
                .inputItems(COLORED_LUMEN_PAINT_BALL.stack(AEColor.BLUE, 64))
                .inputFluids(MutatedLivingSolder.getFluid(1152))
                .inputFluids(Mendelevium.getFluid(1728))
                .inputFluids(DamascusSteel.getFluid(2304))
                .inputFluids(Titanium50.getFluid(2304))
                .outputItems(GTLMachines.GTAEMachines.ME_CRAFT_PATTERN_CONTAINER)
                .EUt(VA[9]).duration(200)
                .stationResearch((b) -> b.researchStack(EX_PATTERN_PROVIDER.asItem().getDefaultInstance())
                        .dataStack(GTItems.TOOL_DATA_MODULE.asStack()).EUt(VA[9]).CWUt(128))
                .save(provider);

        PRECISION_ASSEMBLER_RECIPES.recipeBuilder(GTLCore.id("reaction_chamber"))
                .inputItems(getItemStack("kubejs:relativistic_spinorial_memory_system", 64))
                .inputItems(getItemStack("gtceu:uv_solar_panel", 2))
                .inputItems(getItemStack("kubejs:ctc_computational_unit", 12))
                .inputItems(getItemStack("kubejs:infinity_antimatter_fuel_rod", 64))
                .inputFluids(CosmicElement.getFluid(57600000))
                .inputFluids(Radox.getFluid(1000000))
                .inputFluids(SuperMutatedLivingSolder.getFluid(1000000))
                .inputFluids(UUMatter.getFluid(1000000))
                .outputItems(getItemStack("sgjourney:reaction_chamber"))
                .EUt(V[MAX])
                .duration(5000)
                .cleanroom(GTLCleanroomType.LAW_CLEANROOM)
                .save(provider);

        ASSEMBLY_LINE_RECIPES.recipeBuilder(GTLCore.id("universe_stargate_chevron"))
                .inputItems(DIMENSION_CONNECTION_CASING, 64)
                .inputItems(DIMENSIONALLY_TRANSCENDENT_CASING, 64)
                .inputItems(INFINITY_GLASS, 64)
                .inputItems(getItemStack("gtceu:magmatter_block", 64))
                .inputItems(getItemStack("gtceu:long_magmatter_rod", 16))
                .inputItems(getItemStack("sgjourney:reaction_chamber", 8))
                .inputItems(getItemStack("gtceu:magnetohydrodynamicallyconstrainedstarmatter_plate", 8))
                .inputItems(getItemStack("gtceu:magnetohydrodynamicallyconstrainedstarmatter_frame", 16))
                .inputItems(getItemStack("gtceu:exquisite_ruby_gem", 64))
                .inputItems(getItemStack("gtceu:exquisite_jasper_gem", 64))
                .inputItems(getItemStack("gtceu:exquisite_sapphire_gem", 64))
                .inputItems(getItemStack("gtceu:exquisite_magneto_resonatic_gem", 64))
                .inputItems(EMITTER_MAX, 64)
                .inputItems(ELECTRIC_PISTON_MAX, 64)
                .inputItems(FIELD_GENERATOR_MAX, 16)
                .inputItems(CustomTags.MAX_CIRCUITS, 32)
                .inputFluids(HeavyQuarkDegenerateMatter.getFluid(1024000))
                .inputFluids(ExcitedDtsc.getFluid(512000))
                .inputFluids(ExcitedDtec.getFluid(512000))
                .inputFluids(TranscendentMetal.getFluid(256000))
                .outputItems(getItemStack("sgjourney:universe_stargate_chevron"))
                .EUt(V[MAX])
                .duration(5000)
                .stationResearch(b -> b.researchStack(getItemStack("gtceu:magnetohydrodynamicallyconstrainedstarmatter_plate"))
                        .dataStack(GTItems.TOOL_DATA_MODULE.asStack())
                        .EUt(VA[MAX])
                        .CWUt(262144))
                .save(provider);

        SUPRACHRONAL_ASSEMBLY_LINE_RECIPES.recipeBuilder(GTLCore.id("heartofthesmogus"))
                .inputItems(getItemStack("expatternprovider:fishbig", 8))
                .inputItems(DIMENSIONALLY_TRANSCENDENT_PLASMA_FORGE, 16)
                .inputItems(STAR_ULTIMATE_MATERIAL_FORGE_FACTORY, 16)
                .inputItems(CREATE_COMPUTATION, 8)
                .inputItems(EYE_OF_HARMONY, 8)
                .inputItems(CREATE_AGGREGATION, 8)
                .inputItems(DOOR_OF_CREATE, 8)
                .inputItems(getItemStack("sgjourney:universe_stargate_chevron", 32))
                .inputFluids(MagnetohydrodynamicallyConstrainedStarMatter.getFluid(1000000000))
                .inputFluids(Miracle.getFluid(1000000000))
                .inputFluids(SpaceTime.getFluid(1000000000))
                .inputFluids(Eternity.getFluid(1000000000))
                .outputItems(getItemStack("kubejs:heartofthesmogus"))
                .EUt(V[MAX])
                .duration(8000)
                .stationResearch(b -> b.researchStack(getItemStack("expatternprovider:fishbig"))
                        .dataStack(GTItems.TOOL_DATA_MODULE.asStack())
                        .EUt(VA[MAX])
                        .CWUt(262144))
                .save(provider);

        ASSEMBLER_MODULE_RECIPES.recipeBuilder("crystal_base")
                .inputItems(getItemStack("kubejs:create_ultimate_battery", 16))
                .inputItems(getItemStack("kubejs:topological_manipulator_unit", 64))
                .inputItems(getItemStack("kubejs:dark_matter", 32))
                .inputItems(getItemStack("gtceu:eternity_nanoswarm", 64))
                .inputItems(getItemStack("gtceu:cosmic_plate", 32))
                .inputItems(getItemStack("expatternprovider:fishbig", 64))
                .inputItems(getItemStack("kubejs:supracausal_mainframe", 32))
                .inputItems(getItemStack("gtceu:double_cosmicneutronium_plate", 32))
                .inputItems(getItemStack("gtceu:crystalmatrix_hex_wire", 64))
                .inputItems(getItemStack("gtceu:crystalmatrix_hex_wire", 64))
                .inputItems(getItemStack("gtceu:crystalmatrix_hex_wire", 64))
                .inputItems(getItemStack("gtceu:crystalmatrix_hex_wire", 64))
                .inputFluids(GradePurifiedWater16.getFluid(14400000000000L))
                .inputFluids(CosmicSuperconductor.getFluid(5760000000000L))
                .inputFluids(Miracle.getFluid(655360000000L))
                .inputFluids(Magmatter.getFluid(1000000000000L))
                .outputItems(CRYSTAL_BASE.get())
                .addData("SEPMTier", 5)
                .EUt(V[MAX])
                .duration(1152000)
                .save(provider);
    }
}
