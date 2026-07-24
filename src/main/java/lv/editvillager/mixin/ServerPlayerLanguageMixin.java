package lv.editvillager.mixin;

import lv.editvillager.LanguageManager;
import lv.editvillager.NbtCompat;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerLanguageMixin {

    //? if 1.21.10 || 1.21.11 {
    //? if neoforge {
    // NeoForge runtime is Mojmap: annotation method names are not remapped via refmap.
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "writeCustomData", at = @At("TAIL"), require = 0)
    //?}
    private void ev$saveLanguage(net.minecraft.storage.WriteView view, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        view.putString("EvLanguage", LanguageManager.getLanguage(self));
    }

    //? if neoforge {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "readCustomData", at = @At("TAIL"), require = 0)
    //?}
    private void ev$loadLanguage(net.minecraft.storage.ReadView view, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        LanguageManager.loadSavedLanguage(self.getUuid(), view.getString("EvLanguage", "ru"));
    }
    //?} else {
    //? if neoforge {
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"), require = 0)
    //?}
    private void ev$saveLanguage(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        nbt.putString("EvLanguage", LanguageManager.getLanguage(self));
    }

    //? if neoforge {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"), require = 0)
    //?}
    private void ev$loadLanguage(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        LanguageManager.loadSavedLanguage(self.getUuid(), NbtCompat.getString(nbt, "EvLanguage", "ru"));
    }
    //?}
}
