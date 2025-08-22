package com.backtothereef;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateDeviceController implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName = System.getenv("TABLE_DISPOSITIVO");
    private final String adminToken = System.getenv("ADMIN_TOKEN");

    public CreateDeviceController() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(System.getenv("AWS_REGION")))
                .build();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        // Verificar admin token
        String requestAdminToken = event.getHeaders().get("X-Admin-Token");
        if (requestAdminToken == null || !requestAdminToken.equals(adminToken)) {
            return new APIGatewayProxyResponseEvent().withStatusCode(401).withBody("Unauthorized");
        }

        record DispositivoRequest(String idUsuario, String token, String nome, String tipo, String situacao) {}
        DispositivoRequest req;
        try {
            req = new com.google.gson.Gson().fromJson(event.getBody(), DispositivoRequest.class);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Erro ao parsear JSON");
        }

        if (req.idUsuario == null || req.token == null || req.nome == null || req.tipo == null ||
                (!"online".equals(req.situacao) && !"offline".equals(req.situacao))) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Campos inválidos");
        }

        String id = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS(id));
        item.put("idUsuario", AttributeValue.fromS(req.idUsuario));
        item.put("token", AttributeValue.fromS(req.token));
        item.put("nome", AttributeValue.fromS(req.nome));
        item.put("tipo", AttributeValue.fromS(req.tipo));
        item.put("situacao", AttributeValue.fromS(req.situacao));

        try {
            dynamoDbClient.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
            return new APIGatewayProxyResponseEvent().withStatusCode(201).withBody("{\"id\": \"" + id + "\"}");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Erro ao salvar dispositivo");
        }
    }
}

