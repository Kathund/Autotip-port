/**
 Autotip - tips online boosters, and other online Autotip users.
 You send and receive tips, increasing your coin and experience gain.
 Copyright (C) 2026 QWERTZ, Sk1erLLC

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package club.sk1er.mods.autotip.api;

import club.sk1er.mods.autotip.Autotip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AutotipAPIRequestFactory {
    protected static final String BASE_URL = "https://api.autotip.pro/";
    protected static final String TIP_ENDPOINT = "tip";
    protected static final String LOGIN_ENDPOINT = "login";
    protected static final String LOGOUT_ENDPOINT = "logout";

    public static HttpUriRequest createLoginRequest(User user, String serverHash) {

        String username = URLEncoder.encode(user.getName(), StandardCharsets.UTF_8);
        String uuid = URLEncoder.encode(user.getProfileId().toString().replace("-", ""), StandardCharsets.UTF_8);
        String tips = URLEncoder.encode("0", StandardCharsets.UTF_8); // TODO: Implement tips
        String version = URLEncoder.encode(Autotip.VERSION, StandardCharsets.UTF_8);
        String mcVersion = URLEncoder.encode(Minecraft.getInstance().getLaunchedVersion(), StandardCharsets.UTF_8);
        String os = URLEncoder.encode(System.getProperty("os.name"), StandardCharsets.UTF_8);
        String hash = URLEncoder.encode(serverHash, StandardCharsets.UTF_8);

        String url = String.format(
                "%s?username=%s&uuid=%s&tips=%s&v=%s&mc=%s&os=%s&hash=%s",
                BASE_URL + LOGIN_ENDPOINT, username, uuid, tips, version, mcVersion, os, hash
        );

        return getURIfromUrl(url);
    }

    public static HttpUriRequest createLogoutRequest(String sessionKey) {
        String key = URLEncoder.encode(sessionKey, StandardCharsets.UTF_8);
        String url = String.format(
                "%s?key=%s",
                BASE_URL + LOGOUT_ENDPOINT, key
        );

        return getURIfromUrl(url);
    }

    private static HttpUriRequest getURIfromUrl(String url) {
        try {
            return new HttpGet(new URI(url));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
