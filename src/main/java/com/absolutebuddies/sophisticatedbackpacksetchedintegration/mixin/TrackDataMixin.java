package com.absolutebuddies.sophisticatedbackpacksetchedintegration.mixin;

import gg.moonflower.etched.api.record.TrackData;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.io.FileUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.net.URL;

@Mixin(value = TrackData.class, remap = false)
public class TrackDataMixin {

    @Inject(
        method = "save(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;",
        at = @At("RETURN"),
        remap = false
    )
    private void OnSave(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> cir) {
        // Only process if this track has a URL (i.e. it's a stream)
        if (!nbt.contains("Url", 8)) return;

        CompoundTag result = cir.getReturnValue();
        result.putInt("Duration", GetDurationTicks(nbt.getString("Url")));
    }

    @Unique
    private int GetDurationTicks(String Url) {
        try {
            URL url = new URL(Url);
            String path = url.getPath();
            String ext = path.substring(path.lastIndexOf("."));
            File tempFile = File.createTempFile("audio_cache", ext);
            tempFile.deleteOnExit();

            FileUtils.copyURLToFile(url, tempFile);

            AudioFile audioFile = AudioFileIO.read(tempFile);
            double duration = audioFile.getAudioHeader().getPreciseTrackLength();
            int ticks = (int) Math.round(duration * 20.0);

            System.out.println("[SBEI] Duration: " + ticks + " ticks");
            return ticks;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[SBEI] GetDurationTicks error!");
            return 200; // Fallback: 10 seconds
        }
    }
}
