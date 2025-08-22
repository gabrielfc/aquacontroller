package com.backtothereef;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.Map;

public class UpdateDeviceController implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName = System.getenv("TABLE_DISPOSITIVO");
    private final String adminToken = System.getenv("ADMIN_TOKEN");

    public UpdateDeviceController() {
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

        String id = event.getPathParameters().get("id");
        if (id == null) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("ID obrigatório");
        }

        record DispositivoUpdateRequest(String idUsuario, String token, String nome, String tipo, String situacao) {
        }
        DispositivoUpdateRequest req;
        try {
            req = new com.google.gson.Gson().fromJson(event.getBody(), DispositivoUpdateRequest.class);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Erro ao parsear JSON");
        }

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        StringBuilder updateExpression = new StringBuilder("SET ");
        boolean hasUpdate = false;

        if (req.idUsuario != null) {
            updateExpression.append("idUsuario = :idUsuario, ");
            expressionValues.put(":idUsuario", AttributeValue.fromS(req.idUsuario));
            hasUpdate = true;
        }
        if (req.token != null) {
            updateExpression.append("token = :token, ");
            expressionValues.put(":token", AttributeValue.fromS(req.token));
            hasUpdate = true;
        }
        if (req.nome != null) {
            updateExpression.append("nome = :nome, ");
            expressionValues.put(":nome", AttributeValue.fromS(req.nome));
            hasUpdate = true;
        }
        if (req.tipo != null) {
            updateExpression.append("tipo = :tipo, ");
            expressionValues.put(":tipo", AttributeValue.fromS(req.tipo));
            hasUpdate = true;
        }
        if (req.situacao != null && ("online".equals(req.situacao) || "offline".equals(req.situacao))) {
            updateExpression.append("situacao = :situacao, ");
            expressionValues.put(":situacao", AttributeValue.fromS(req.situacao));
            hasUpdate = true;
        }

        if (!hasUpdate) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Nenhum campo para atualizar");
        }

        updateExpression.setLength(updateExpression.length() - 2);

        Map<String, AttributeValue> key = Map.of("id", AttributeValue.fromS(id));

        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression(updateExpression.toString())
                    .expressionAttributeValues(expressionValues)
                    .conditionExpression("attribute_exists(id)")
                    .build());
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Dispositivo atualizado");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Erro ao atualizar dispositivo");
        }
    }
}
