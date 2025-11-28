package services;

import com.typesafe.config.Config;
import play.libs.Json;
import play.libs.ws.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletionStage;

@Singleton
public class SawkiSmsClient {

    private final WSClient ws;
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String sender;
    private final String dlrUrl;

    @Inject
    public SawkiSmsClient(WSClient ws, Config config) {
        this.ws = ws;

        Config c = config.getConfig("sawki-sms");

        this.baseUrl  = c.getString("baseUrl");
        this.username = c.getString("username");
        this.password = c.getString("password");
        this.sender   = c.getString("sender");
        this.dlrUrl   = c.getString("dlr-url");
    }

    public CompletionStage<WSResponse> sendSms(String phoneE164, String message) {

        ObjectNode body = Json.newObject();

        body.put("to", Long.parseLong(phoneE164));
        body.put("from", sender);
        body.put("content", message);
        body.put("dlr", "yes");
        body.put("dlr-level", 3);
        body.put("dlr-method", "GET");
        body.put("dlr-url", dlrUrl);

        return ws.url(baseUrl + "/secure/send")
                .setContentType("application/json")
                .setAuth(username, password, WSAuthScheme.BASIC)
                .post(body);
    }
}
