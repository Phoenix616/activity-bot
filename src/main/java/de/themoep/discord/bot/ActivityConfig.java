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

import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;

import java.awt.Color;
import java.util.Locale;
import java.util.Map;

public class ActivityConfig {
    private final String source;
    private final ActivityType type;
    private final String message;
    private final UserStatus status;
    private final Color color;

    public ActivityConfig(Map<String, Object> activity) {
        source = (String) activity.get("source");
        type = ActivityType.valueOf(((String) activity.get("type")).toUpperCase(Locale.ROOT));
        message = (String) activity.get("message");
        status = UserStatus.valueOf(((String) activity.get("status")).toUpperCase(Locale.ROOT));
        if (activity.containsKey("color")) {
            String[] parts = ((String) activity.get("color")).split(",");
            color = new Color(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            );
        } else {
            color = null;
        }
    }

    public String getSource() {
        return source;
    }

    public ActivityType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "ActivityConfig{" +
                "source='" + source + '\'' +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", status=" + status +
                ", color=" + color +
                '}';
    }
}
