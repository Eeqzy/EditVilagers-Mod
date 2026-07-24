package lv.editvillager.mixin;

import lv.editvillager.LanguageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerLanguageMixin {

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V",
            at = @At("TAIL"))
    private void ev$saveLanguage(ValueOutput output, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        output.putString("EvLanguage", LanguageManager.getLanguage(self));
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V",
            at = @At("TAIL"))
    private void ev$loadLanguage(ValueInput input, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        LanguageManager.loadSavedLanguage(self.getUUID(), input.getStringOr("EvLanguage", "ru"));
    }
}
