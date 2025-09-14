package org.gtlcore.gtlcore.integration.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.inv.CraftingSimulationState;

public interface ICraftingTreeProcess {

    void fastRequest(CraftingSimulationState inv, long times) throws CraftBranchFailure, InterruptedException;

    IPatternDetails getDetails();

    boolean getPossible();

    long getOutputCountTest(AEKey what);

    boolean limitsQuantityTest();
}
