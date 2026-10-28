package su.nightexpress.excellentcrates.animation.style.pickone.codec;

import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentcrates.animation.style.pickone.PickOneSettings;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.codec.ConfigCodec;
import su.nightexpress.nightcore.configuration.codec.ConfigCodecs;
import su.nightexpress.nightcore.configuration.exception.CodecReadException;

@NullMarked
public class PickOneSettingsCodec implements ConfigCodec<PickOneSettings> {

    public static final PickOneSettingsCodec INSTANCE = new PickOneSettingsCodec();

    @Override
    public PickOneSettings read(FileConfig config, String path) throws CodecReadException {
        PickOneSettings defaults = PickOneSettings.defaultSettings();
        String name = config.getOrSet(path + ".name", ConfigCodecs.STRING, defaults.name());
        int choices = config.getOrSet(path + ".choices", ConfigCodecs.INT, defaults.choices());

        return new PickOneSettings(name, choices);
    }

    @Override
    public void write(FileConfig config, String path, PickOneSettings value) {
        config.set(path + ".name", value.name());
        config.set(path + ".choices", value.choices());
    }
}
