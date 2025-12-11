package com.chess.service;

import com.chess.dao.UserDAO;
import com.chess.models.User;
import org.mindrot.jbcrypt.BCrypt;
import org.json.JSONObject;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AuthService {
    private final UserDAO userDAO;
    private static final String SECRET_KEY = "enpassant";
    private final Map<String, Integer> tokenCache = new HashMap<>();

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public String register(String username, String password) throws SQLException {
        if (userDAO.usernameExists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (username.length() < 3 || username.length() > 20) {
            throw new IllegalArgumentException("Username must be 3-20 characters");
        }

        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        User user = new User(username, passwordHash);
        userDAO.createUser(user);

        return generateToken(user.getUserId(), username);
    }

    public String login(String username, String password) throws SQLException {
        User user = userDAO.findByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return generateToken(user.getUserId(), username);
    }

    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            if (tokenCache.containsKey(token)) {
                return true;
            }

            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];

            String computedSignature = hmacSha256(header + "." + payload, SECRET_KEY);
            if (!computedSignature.equals(signature)) {
                return false;
            }

            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
            JSONObject payloadObj = new JSONObject(decodedPayload);

            long expiration = payloadObj.getLong("exp");
            if (System.currentTimeMillis() > expiration) {
                return false;
            }

            int userId = payloadObj.getInt("userId");
            tokenCache.put(token, userId);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getUserIdFromToken(String token) {
        try {
            if (tokenCache.containsKey(token)) {
                return tokenCache.get(token);
            }

            String[] parts = token.split("\\.");
            String payload = parts[1];

            String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
            JSONObject payloadObj = new JSONObject(decodedPayload);

            int userId = payloadObj.getInt("userId");
            tokenCache.put(token, userId);

            return userId;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    public void invalidateToken(String token) {
        tokenCache.remove(token);
    }

    private String generateToken(int userId, String username) {
        try {
            JSONObject header = new JSONObject();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(header.toString().getBytes());
            JSONObject payload = new JSONObject();
            payload.put("userId", userId);
            payload.put("username", username);
            payload.put("exp", System.currentTimeMillis() + 86400000); // 24 hours
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payload.toString().getBytes());

            String data = encodedHeader + "." + encodedPayload;
            String signature = hmacSha256(data, SECRET_KEY);

            tokenCache.put(data + "." + signature, userId);

            return data + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256HMAC.init(secretKey);
        byte[] hash = sha256HMAC.doFinal(data.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}