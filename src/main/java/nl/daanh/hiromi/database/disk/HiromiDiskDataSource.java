package nl.daanh.hiromi.database.disk;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import nl.daanh.hiromi.database.IDatabaseManager;
import nl.daanh.hiromi.exceptions.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Properties;

public class HiromiDiskDataSource implements IDatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(HiromiDiskDataSource.class);
    private static final int EXPIRES_IN = 60; // 60 seconds
    private final HashMap<Long, Properties> guildCache = new HashMap<>();
    private final HashMap<Long, Properties> userCache = new HashMap<>();

    private File getFile(Guild guild) {
        return new File("./guildConfigs/" + guild.getIdLong() + "/config.properties");
    }

    private File getFile(User user) {
        return new File("./userConfigs/" + user.getIdLong() + "/config.properties");
    }

    private void writeKey(String key, String value, Properties properties, File settingsFile, String name, long idLong) {
        try {
            properties.setProperty(key, value);

            if (!settingsFile.exists())
                if (settingsFile.getParentFile().mkdirs())
                    LOGGER.debug(String.format("Created the initial file structure for the guild: %s", name));

            FileWriter writer = new FileWriter(settingsFile);
            properties.store(writer, String.format("ID: %s", idLong));
            writer.close();
        } catch (IOException e) {
            throw new HiromiDiskIOException(e);
        }
    }

    @NotNull
    private Properties fetchSettings(long idLong, File file, HashMap<Long, Properties> cache) {
        if (cache.containsKey(idLong)) {
            Properties setting = cache.get(idLong);
            if (Long.parseLong(setting.getProperty("local_expires_at", "0")) > Instant.now().getEpochSecond()) {
                return setting;
            }
        }

        if (!file.exists()) return new Properties();

        try {
            FileReader reader = new FileReader(file);
            Properties properties = new Properties();
            properties.load(reader);
            reader.close();

            properties.put("local_expires_at", String.valueOf(Instant.now().getEpochSecond() + EXPIRES_IN));
            cache.put(idLong, properties);
            return properties;
        } catch (IOException e) {
            throw new HiromiDiskIOException(e);
        }
    }

    private Properties fetchSettings(Guild guild) {
        return fetchSettings(guild.getIdLong(), this.getFile(guild), guildCache);
    }

    private Properties fetchSettings(User user) {
        return fetchSettings(user.getIdLong(), this.getFile(user), userCache);
    }

    @Override
    public String getKey(Guild guild, String key) {
        Properties setting = this.fetchSettings(guild);
        if (setting.containsKey(key)) {
            return setting.getProperty(key);
        }

        return this.getDefaultSetting(key);
    }

    @Override
    public void writeKey(Guild guild, String key, String value) {
        writeKey(key, value, this.fetchSettings(guild), this.getFile(guild), guild.getName(), guild.getIdLong());
    }

    @Override
    public String getKey(User user, String key) {
        Properties setting = this.fetchSettings(user);
        if (setting.containsKey(key)) {
            return setting.getProperty(key);
        }

        return this.getDefaultSetting(key);
    }

    @Override
    public void writeKey(User user, String key, String value) {
        writeKey(key, value, this.fetchSettings(user), this.getFile(user), user.getName(), user.getIdLong());
    }

    @Override
    public String getKey(Member member, String key) {
        throw new NotImplementedException();
    }

    @Override
    public void writeKey(Member member, String key, String value) {
        throw new NotImplementedException();
    }
}