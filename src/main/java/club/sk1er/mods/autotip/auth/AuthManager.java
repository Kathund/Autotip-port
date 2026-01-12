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

package club.sk1er.mods.autotip.auth;

import club.sk1er.mods.autotip.Autotip;
import club.sk1er.mods.autotip.api.AutotipAPIRequestFactory;
import club.sk1er.mods.autotip.api.AutotipHttpClient;
import club.sk1er.mods.autotip.api.records.LoginRecord;
import club.sk1er.mods.autotip.api.records.LogoutRecord;
import com.google.gson.JsonObject;
import org.apache.http.HttpStatus;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.apache.http.client.methods.HttpUriRequest;

public class AuthManager {
    private static final SecureRandom RANDOM = new SecureRandom();
    public static boolean loggedIn = false;
    public static String sessionKey = null;
    public static String getNextSalt() {
        return new BigInteger(130, RANDOM).toString(32);
    }

    public static String hash(String str) {
        try {
            byte[] digest = digest(str);
            return new BigInteger(digest).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static byte[] digest(String str) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        return md.digest(strBytes);
    }

    public void login() {
        if (!loggedIn) {
            User user = Minecraft.getInstance().getUser();
            String token = user.getAccessToken();

            String uuid = user.getProfileId().toString().replace("-", "");
            String serverHash = hash(uuid + getNextSalt());

            int statusCode = this.authenticate(token, uuid, serverHash);
            if (statusCode / 100 != 2) {
                Autotip.getInstance().getMessageUtil().send("Error {} during authentication: Session servers down?", statusCode);
                return;
            }

            HttpUriRequest request = AutotipAPIRequestFactory.createLoginRequest(user, serverHash);

            try {
                AutotipHttpClient client = Autotip.getInstance().getAutotipHttpClient();
                String responseBody = client.executeRequest(request);
                LoginRecord loginRecord = LoginRecord.fromResponseBody(responseBody);
                if (loginRecord.success()) {
                    sessionKey = loginRecord.sessionKey();
                    loggedIn = true;
                    Autotip.getInstance().getMessageUtil().send("Successfully connected to Autotip API!"); // TODO: Prettier / Toggleable message
                } else {
                    Autotip.getInstance().getMessageUtil().send("Error during login: {}", loginRecord.cause() == null ? "null" : loginRecord.cause());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logout() {
        if (loggedIn) {
            HttpUriRequest request = AutotipAPIRequestFactory.createLogoutRequest(sessionKey);

            try {
                AutotipHttpClient client = Autotip.getInstance().getAutotipHttpClient();
                String responseBody = client.executeRequest(request);
                LogoutRecord logoutRecord = LogoutRecord.fromResponseBody(responseBody);
                if (logoutRecord.success()) {
                    sessionKey = null;
                    loggedIn = false;
                    Autotip.getInstance().getMessageUtil().send("Successfully disconnected from Autotip API!"); // TODO: Prettier / Toggleable message
                } else {
                    Autotip.getInstance().getMessageUtil().send("Error during logout: {}", logoutRecord.cause() == null ? "null" : logoutRecord.cause());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int authenticate(String token, String uuid, String serverHash) {
        try {
            URL url = URI.create("https://sessionserver.mojang.com/session/minecraft/join").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            JsonObject obj = new JsonObject();
            obj.addProperty("accessToken", token);
            obj.addProperty("selectedProfile", uuid);
            obj.addProperty("serverId", serverHash);

            byte[] jsonBytes = obj.toString().getBytes(StandardCharsets.UTF_8);

            conn.setFixedLengthStreamingMode(jsonBytes.length);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.connect();

            try (OutputStream out = conn.getOutputStream()) {
                out.write(jsonBytes);
            }

            return conn.getResponseCode();
        } catch (IOException e) {
            return HttpStatus.SC_BAD_REQUEST;
        }
    }
}
