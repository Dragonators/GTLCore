package org.gtlcore.gtlcore.mixin.gtm.api.machine;

import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.api.recipe.lookup.RecipeIterator;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.ResearchStationMachine;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.network.chat.Component;

import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(RecipeLogic.class)
public abstract class RecipeLogicMixin implements ILockRecipe, IRecipeStatus {

    @Persisted
    @Getter
    private boolean isLock = false;
    @Persisted
    @Getter
    @Setter
    private GTRecipe lockRecipe;
    @Getter
    @Setter
    private RecipeResult recipeStatus;
    @Getter
    @Setter
    private RecipeResult workingStatus;

    @Mutable
    @Final
    @Shadow(remap = false)
    public final IRecipeLogicMachine machine;
    @Mutable
    @Final
    @Shadow(remap = false)
    protected final Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches;
    @Shadow(remap = false)
    public List<GTRecipe> lastFailedMatches;
    @Shadow(remap = false)
    protected @Nullable GTRecipe lastRecipe;
    @Shadow(remap = false)
    protected @Nullable GTRecipe lastOriginRecipe;
    @Shadow(remap = false)
    protected OCParams ocParams;
    @Shadow(remap = false)
    protected OCResult ocResult;
    @Shadow(remap = false)
    protected boolean recipeDirty;
    @Shadow(remap = false)
    protected int progress;
    @Shadow(remap = false)
    protected int duration;
    @Shadow(remap = false)
    private boolean isActive;
    @Shadow(remap = false)
    private RecipeLogic.Status status;
    @Shadow(remap = false)
    protected long totalContinuousRunningTime;

    @Shadow(remap = false)
    protected abstract Iterator<GTRecipe> searchRecipe();

    @Shadow(remap = false)
    public abstract void markLastRecipeDirty();

    @Shadow(remap = false)
    public abstract void setStatus(RecipeLogic.Status status);

    @Shadow(remap = false)
    public abstract RecipeLogic.Status getStatus();

    @Shadow(remap = false)
    public abstract void updateTickSubscription();

    @Shadow(remap = false)
    public abstract GTRecipe.ActionResult handleTickRecipe(GTRecipe recipe);

    @Shadow(remap = false)
    public abstract void interruptRecipe();

    @Shadow(remap = false)
    public abstract void setWaiting(@Nullable Component reason);

    @Shadow(remap = false)
    protected abstract void doDamping();

    public RecipeLogicMixin(IRecipeLogicMachine machine, Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches) {
        this.machine = machine;
        this.chanceCaches = chanceCaches;
    }

    public void setLock(boolean look) {
        isLock = look;
        lockRecipe = null;
        updateTickSubscription();
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    protected boolean handleRecipeIO(GTRecipe recipe, IO io) {
        if (!(this.machine.hasProxies() && io != IO.BOTH)) return false;
        return RecipeRunnerHelper.handleRecipeInput(this.machine, recipe);
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void handleRecipeWorking() {
        assert this.lastRecipe != null;
        GTRecipe.ActionResult result = this.handleTickRecipe(this.lastRecipe);
        if (result.isSuccess()) {
            this.setStatus(RecipeLogic.Status.WORKING);
            if (!this.machine.onWorking()) {
                this.interruptRecipe();
                return;
            }
            ++this.progress;
            ++this.totalContinuousRunningTime;
        } else {
            this.setWaiting(result.reason().get());
        }
        if (this.status == RecipeLogic.Status.WAITING) {
            this.doDamping();
        }
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void setupRecipe(GTRecipe recipe) {
        if (!this.machine.beforeWorking(recipe)) {
            this.setStatus(RecipeLogic.Status.IDLE);
            this.progress = 0;
            this.duration = 0;
            this.isActive = false;
            return;
        }
        if (this.handleRecipeIO(recipe, IO.IN)) {
            if (this.lastRecipe != null && !recipe.equals(this.lastRecipe)) {
                this.chanceCaches.clear();
            }
            this.recipeDirty = false;
            this.lastRecipe = recipe;
            this.setStatus(RecipeLogic.Status.WORKING);
            this.progress = 0;
            this.duration = recipe.duration;
            this.isActive = true;
        }
    }

    /**
     * @author Dragons
     * @reason 删除lastFailedMatches操作
     */
    @Overwrite(remap = false)
    private void handleSearchingRecipes(Iterator<GTRecipe> matches) {
        while (matches != null && matches.hasNext()) {
            GTRecipe match = matches.next();
            if (match != null) {
                if (this.checkMatchedRecipeAvailable(match)) {
                    return;
                }

                if (this.lastFailedMatches == null) {
                    this.lastFailedMatches = new ObjectArrayList<>();
                }

                this.lastFailedMatches.add(match);
            }
        }
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void findAndHandleRecipe() {
        this.lastRecipe = null;
        this.lastFailedMatches = null;
        if (this.isLock && lockRecipe != null) {
            this.lastOriginRecipe = lockRecipe;
            GTRecipe modified = machine.fullModifyRecipe(lastOriginRecipe.copy(), this.ocParams, this.ocResult);
            if (modified != null && this.gtlcore$checkLastRecipe(modified)) {
                setupRecipe(modified);
            }
        } else {
            this.lastOriginRecipe = null;
            this.handleSearchingRecipes(this.machine instanceof ResearchStationMachine ? this.searchResearchRecipe() : this.searchRecipe());
        }
        this.recipeDirty = false;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public boolean checkMatchedRecipeAvailable(GTRecipe match) {
        GTRecipe modified = this.machine.fullModifyRecipe(match.copy(), this.ocParams, this.ocResult);
        if (modified != null) {
            if (gtlcore$checkLastRecipe(modified)) {
                this.setupRecipe(modified);
            }
            if (this.lastRecipe != null && this.getStatus() == RecipeLogic.Status.WORKING) {
                this.lastOriginRecipe = match;
                this.lastFailedMatches = null;
                if (this.isLock) this.lockRecipe = match;
                return true;
            }
        }
        return false;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void onRecipeFinish() {
        this.machine.afterWorking();
        if (this.lastRecipe != null) {
            RecipeRunnerHelper.handleRecipeOutput(this.machine, this.lastRecipe);
            if (this.machine.alwaysTryModifyRecipe()) {
                if (this.lastOriginRecipe != null) {
                    GTRecipe modified = this.machine.fullModifyRecipe(this.lastOriginRecipe.copy(), this.ocParams, this.ocResult);
                    if (modified == null) {
                        this.markLastRecipeDirty();
                    } else {
                        this.lastRecipe = modified;
                    }
                } else {
                    this.markLastRecipeDirty();
                }
            }
            if (!this.recipeDirty && gtlcore$checkLastRecipe(this.lastRecipe)) {
                this.setupRecipe(this.lastRecipe);
            } else {
                this.setStatus(RecipeLogic.Status.IDLE);
                this.progress = 0;
                this.duration = 0;
                this.isActive = false;
            }
        }
    }

    @Unique
    protected @Nullable Iterator<GTRecipe> searchResearchRecipe() {
        IRecipeLogicMachine holder = this.machine;
        if (!holder.hasProxies()) return null;
        else {
            RecipeIterator iterator = this.machine.getRecipeType().getLookup().getRecipeIterator(holder, (r) -> {
                if (r.isFuel || !holder.hasProxies()) return false;
                else {
                    GTRecipe.ActionResult result = r.matchRecipeContents(IO.IN, holder, r.inputs, false);
                    if (!result.isSuccess()) {
                        RecipeResult.of(this.machine, RecipeResult.FAIL_FIND);
                        return false;
                    } else if (r.hasTick()) {
                        result = r.matchRecipeContents(IO.IN, holder, r.tickInputs, true);
                        if (!result.isSuccess() && result.reason() != null) {
                            String s = result.reason().get().getSiblings().get(1).toString();
                            if (s.contains("eu")) RecipeResult.of(holder, RecipeResult.FAIL_NO_ENOUGH_EU_IN);
                            else if (s.contains("cwu")) RecipeResult.of(holder, RecipeResult.FAIL_NO_ENOUGH_CWU_IN);
                        }
                        return result.isSuccess();
                    } else return true;
                }
            });
            boolean any = false;
            GTRecipe recipe = null;
            while (iterator.hasNext()) {
                recipe = iterator.next();
                if (recipe != null) {
                    any = true;
                    break;
                }
            }
            if (any) {
                iterator.reset();
                return Collections.singleton(recipe).iterator();
            } else {
                for (GTRecipeType.ICustomRecipeLogic logic : this.machine.getRecipeType().getCustomRecipeLogicRunners()) {
                    recipe = logic.createCustomRecipe(holder);
                    if (recipe != null) {
                        return Collections.singleton(recipe).iterator();
                    }
                }
                return Collections.emptyIterator();
            }
        }
    }

    @Unique
    private boolean gtlcore$checkLastRecipe(GTRecipe lastRecipe) {
        return RecipeRunnerHelper.matchRecipe(this.machine, lastRecipe) &&
                lastRecipe.matchTickRecipe(this.machine).isSuccess() &&
                lastRecipe.checkConditions(this.machine.getRecipeLogic()).isSuccess();
    }
}
