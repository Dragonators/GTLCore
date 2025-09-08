package org.gtlcore.gtlcore.mixin;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AE2VersionPlugin implements IMixinConfigPlugin {

    private static final String AE2_MODID = "ae2";
    private static final ComparableVersion THRESHOLD = new ComparableVersion("15.2.14");

    private static boolean useLatestAE() {
        Optional<String> versionStr;
        ModList modList = ModList.get();

        if (modList == null) {
            versionStr = LoadingModList.get().getMods().stream()
                    .filter(mi -> mi.getModId().equals(AE2_MODID))
                    .findFirst()
                    .map(mi -> mi.getVersion().toString());
        } else {
            versionStr = modList.getModContainerById(AE2_MODID)
                    .map(mc -> mc.getModInfo().getVersion().toString());
        }

        return versionStr
                .map(v -> new ComparableVersion(v).compareTo(THRESHOLD) >= 0)
                .orElse(false);
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("CraftingCpuLogicNewMixin")) {
            return useLatestAE();
        } else if (mixinClassName.contains("CraftingCpuLogicOldMixin")) {
            return !useLatestAE();
        } else return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo iMixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode classNode, String mixinClassName, IMixinInfo iMixinInfo) {}
}
