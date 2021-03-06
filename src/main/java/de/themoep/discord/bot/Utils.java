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
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static List<String> getList(Config config, String path) {
        List<String> list = new ArrayList<>();
        try {
            ConfigValue value = config.getValue(path);
            if (value.valueType() == ConfigValueType.LIST) {
                for (Object o : ((List<?>) value.unwrapped())) {
                    list.add(String.valueOf(o));
                }
            } else {
                list.add(String.valueOf(value.unwrapped()));
            }
        } catch (ConfigException ignored) {}
        return list;
    }

    public static Color getRandomColor() {
        return new Color((int) (ActivityBot.RANDOM.nextDouble() * 0x1000000));
    }

    public static String replacePlaceholders(String response) {
        return replace(response, "version", ActivityBot.VERSION, "name", ActivityBot.NAME);
    }

    public static String replace(String string, String... replacements) {
        for (int i = 0; i+1 < replacements.length; i+=2) {
            string = string.replace("%" + replacements[i] + "%", replacements[i+1]);
        }
        return string;
    }

    private static String getInputString(HttpURLConnection con) throws IOException {
        return readInputStream(con.getInputStream());
    }

    private static String readInputStream(InputStream inputStream) throws IOException {
        StringBuilder msg = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = in.readLine()) != null) {
            if (msg.length() != 0) {
                msg.append("\n");
            }
            msg.append(line);
        }
        in.close();
        return msg.toString();
    }
}
