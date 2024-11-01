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

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", "Bearer " + gptConfig.getSecretKey());
        con.setRequestProperty("Content-Type", "application/json");

        JSONObject bodyObject = new JSONObject();
        bodyObject.put("model", gptConfig.getVersion());

        JSONArray messagesArray = new JSONArray();

        JSONObject messageSystem = new JSONObject();
        messageSystem.put("role", "system");
        messageSystem.put("content", systemText);
        messagesArray.put(messageSystem);


        JSONObject messageUser = new JSONObject();
        messageUser.put("role", "user");
        messageUser.put("content", message);

        messagesArray.put(messageUser);

        bodyObject.put("messages", messagesArray);

        String body = bodyObject.toString();
        con.setDoOutput(true);

        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(con.getOutputStream());
        outputStreamWriter.write(body);
        outputStreamWriter.flush();
        outputStreamWriter.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;

        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return extractContentFromResponse(response.toString());
    }

    public static String extractContentFromResponse(String response) {
        int startMarker = response.indexOf("content") + 11;
        int endMarker = response.indexOf("\"", startMarker);
        return response.substring(startMarker, endMarker);
    }
}
