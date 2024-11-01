package bashkirov.store_original.service;

import bashkirov.store_original.config.GptConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class ChatGptService {
    private final GptConfig gptConfig;

    @SneakyThrows
    public String chatGpt(String message, String systemText) {
        URL url = new URL(gptConfig.getUrl());

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + gptConfig.getSecretKey());
            connection.setRequestProperty("Content-Type", "application/json");

            JSONObject bodyObject = new JSONObject();
            bodyObject.put("model", gptConfig.getVersion());

            JSONObject messageSystem = new JSONObject();
            messageSystem.put("role", "system");
            messageSystem.put("content", systemText);

            JSONArray messagesArray = new JSONArray();
            messagesArray.put(messageSystem);


            JSONObject messageUser = new JSONObject();
            messageUser.put("role", "user");
            messageUser.put("content", message);

            messagesArray.put(messageUser);

            bodyObject.put("messages", messagesArray);

            String body = bodyObject.toString();
            connection.setDoOutput(true);

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;

            StringBuffer response = new StringBuffer();
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();

            return extractContentFromResponse(response.toString());
        } catch (Exception e) {
            System.out.println("Error connecting to ChatGPT API:" + e.getMessage());
            return "Вибачне, сталася помилка під час обробки вашого запиту. Будь ласка, спробуйте ще раз пізніше.";
        }
    }

    public static String extractContentFromResponse(String response) {
        int startMarker = response.indexOf("content") + 11;
        int endMarker = response.indexOf("\"", startMarker);
        return response.substring(startMarker, endMarker);
    }
}
