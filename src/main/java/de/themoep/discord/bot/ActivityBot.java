package de.themoep.discord.bot;

/*
 * discord-activity-bot - activity-bot
 * Copyright (C) 2021 Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ActivityBot {
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

    public static String VERSION = "Unknown Version";
    public static String NAME = ActivityBot.class.getSimpleName();
    public static Random RANDOM = new Random();

    private Config config = ConfigFactory.load();

    private ScheduledExecutorService scheduler;

    private DiscordApi discordApi;

    private List<ActivityConfig> activities;

    private ActivityConfig currentActivity = null;

    public static void main(String[] args) {
        try {
            Properties p = new Properties();
            InputStream is = ActivityBot.class.getResourceAsStream("/META-INF/app.properties");
            if (is != null) {
                p.load(is);
                NAME = p.getProperty("application.name", "");
                VERSION = p.getProperty("application.version", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log(Level.INFO, "Starting " + NAME + " v" + VERSION);
        new ActivityBot();
    }

    // TODO: Proper logging
    public static void log(Level level, String message) {
        System.out.println(TIME_FORMAT.format(new Date(System.currentTimeMillis())) + " " + level.getName() + " " + message);
    }

    public static void log(Level level, String message, Throwable throwable) {
        log(level, message);
        throwable.printStackTrace();
    }

    public ActivityBot() {
        loadConfig();

        notifyOperators("Started " + NAME + " v" + VERSION);
        synchronized (ActivityBot.this) {
            try {
                wait();
            } catch (InterruptedException ignored) { }
            log(Level.INFO, "Shutting down " + NAME + " v" + VERSION);
            discordApi.disconnect();
            try {
                wait(1000);
            } catch (InterruptedException ignored) { }
        }
        log(Level.INFO, "Bye!");
        System.exit(0);
    }

    private void notifyOperators(String message) {
        List<CompletableFuture<User>> operators = new ArrayList<>();
        operators.add(getDiscordApi().getOwner());
        for (String id : getConfig().getStringList("discord.operators")) {
            operators.add(getDiscordApi().getUserById(id));
        }
        for (CompletableFuture<User> future : operators) {
            try {
                future.get().sendMessage(message).get();
            } catch (InterruptedException | ExecutionException ignored) {}
        }
    }

    private void loadConfig() {
        if (discordApi != null) {
            discordApi.disconnect();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        config = getConfig("bot");

        activities = new ArrayList<>();
        if (getConfig().hasPath("activities")) {
            for (Object activity : getConfig().getList("activities").unwrapped()) {
                if (activity instanceof Map) {
                    try {
                        ActivityConfig activityConfig = new ActivityConfig((Map<String, Object>) activity);
                        activities.add(activityConfig);
                        log(Level.INFO, "Loaded " + activityConfig);
                    } catch (IllegalArgumentException | ClassCastException e) {
                        log(Level.SEVERE, "Error while loading activities config!", e);
                    }
                } else {
                    log(Level.WARNING, activity + " is not a Config!");
                }
            }
        }

        try {
            discordApi = new DiscordApiBuilder().setToken(getConfig().getString("discord.token")).login().join();

            scheduler = Executors.newScheduledThreadPool(1);

            scheduler.scheduleAtFixedRate(this::checkActivities, 0, config.getInt("refresh-interval"), TimeUnit.SECONDS);

            log(Level.INFO, "You can invite the bot by using the following url: "
                    + discordApi.createBotInvite(
                            new PermissionsBuilder()
                                    .setAllowed(PermissionType.READ_MESSAGES)
                                    .setAllowed(PermissionType.SEND_MESSAGES)
                                    .setAllowed(PermissionType.CHANGE_NICKNAME)
                                    .build()));
        } catch (CompletionException e) {
            log(Level.SEVERE, "Error connecting to discord! Is the token correct?");
        }
    }

    private void checkActivities() {
        for (ActivityConfig activity : activities) {
            if (applies(activity)) {
                if (currentActivity != activity) {
                    currentActivity = activity;
                    log(Level.INFO, "Changed to " + activity);
                    if (getDiscordApi().getStatus() != activity.getStatus()) {
                        getDiscordApi().updateStatus(activity.getStatus());
                    }
                    //getDiscordApi().updateActivity(activity.getType(), activity.getMessage());
                    for (Server server : getDiscordApi().getServers()) {
                        getDiscordApi().getYourself().updateNickname(server, activity.getMessage());
                        if (activity.getColor() != null) {
                            Optional<Color> color = getDiscordApi().getYourself().getRoleColor(server);
                            if (color.isPresent()) {
                                for (Role role : getDiscordApi().getYourself().getRoles(server)) {
                                    if (role.isManaged() && !activity.getColor().equals(role.getColor().orElse(null))) {
                                        role.createUpdater()
                                                .setColor(activity.getColor())
                                                .setAuditLogReason("Changed activity to " + activity)
                                                .update();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    private boolean applies(ActivityConfig activity) {
        boolean retMatches = true;
        String source = activity.getSource();
        if (source.startsWith("!")) {
            source = source.substring(1);
            retMatches = false;
        }

        return applies(source) == retMatches;
    }

    private boolean applies(String source) {
        if (source.startsWith("file:")) {
            File file = new File(source.substring("file:".length()));
            return file.exists();
        } else if (source.startsWith("time:")) {
            String[] parts = source.substring("time:".length()).split("-");
            if (parts.length == 2) {
                try {
                    int startTime = Integer.parseInt(parts[0]);
                    int endTime = Integer.parseInt(parts[1]);

                    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

                    Calendar.getInstance().set(Calendar.HOUR_OF_DAY, startTime);

                    if (endTime > startTime) {
                        return hour >= startTime && hour < endTime;
                    } else if (endTime < startTime) {
                        return hour >= startTime || hour < endTime;
                    }
                    return hour == endTime;
                } catch (NumberFormatException e) {
                    log(Level.SEVERE, "Unable to parse time from " + source, e);
                }
            }
        } else {
            return true;
        }
        return false;
    }

    public void saveResource(String name) {
        InputStream inputStream = getClass().getResourceAsStream("/" + name);
        if (inputStream != null) {
            File file = new File(name);
            if (!file.exists()) {
                try {
                    Files.copy(inputStream, file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            log(Level.WARNING, "No resource " + name + " found!");
        }
    }

    public Config getConfig(String name) {
        saveResource(name + ".conf");
        Config fallbackConfig;
        try {
            fallbackConfig = ConfigFactory.parseResourcesAnySyntax(name + ".conf");
        } catch (ConfigException e) {
            log(Level.SEVERE, "Error while loading " + name + ".conf fallback config!", e);
            fallbackConfig = ConfigFactory.empty("Empty " + name + ".conf fallback due to loading error: " + e.getMessage());
        }
        try {
            return ConfigFactory.parseFile(new File(name + ".conf")).withFallback(fallbackConfig);
        } catch (ConfigException e) {
            log(Level.SEVERE, "Error while loading " + name + ".conf config!", e);
            return fallbackConfig;
        }
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public User getUser(String discordId) {
        if (discordId != null) {
            if (discordId.contains("#")) {
                return getDiscordApi().getCachedUserByDiscriminatedName(discordId).orElseGet(() -> {
                    for (Server server : getDiscordApi().getServers()) {
                        Optional<User> member = server.getMemberByDiscriminatedName(discordId);
                        if (member.isPresent()) {
                            return member.get();
                        }
                    }
                    return null;
                });
            } else {
                return getDiscordApi().getCachedUserById(discordId).orElseGet(() -> {
                    for (Server server : getDiscordApi().getServers()) {
                        Optional<User> member = server.getMemberById(discordId);
                        if (member.isPresent()) {
                            return member.get();
                        }
                    }
                    return null;
                });
            }
        }
        return null;
    }

    private Config getConfig() {
        return config;
    }

    private DiscordApi getDiscordApi() {
        return discordApi;
    }
}
