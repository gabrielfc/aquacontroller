package com.backtothereef;

import com.amazonaws.services.lambda.runtime.Context;
        import com.amazonaws.services.lambda.runtime.RequestHandler;
        import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
        import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
        import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
        import software.amazon.awssdk.regions.Region;
        import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
        import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
        import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
        import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
        import com.google.gson.Gson;

        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;

public class GetDevicesByUserController implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName = System.getenv("TABLE_DISPOSITIVO");
    private final String adminToken = System.getenv("ADMIN_TOKEN");
    private final Gson gson = new Gson();

    public GetDevicesByUserController() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(System.getenv("AWS_REGION")))
                .build();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        // Verificar admin token
        String requestAdminToken = event.getHeaders() != null ? event.getHeaders().get("X-Admin-Token") : null;
        if (requestAdminToken == null || !requestAdminToken.equals(adminToken)) {
            return new APIGatewayProxyResponseEvent().withStatusCode(401).withBody("Unauthorized");
        }

        // Obter idUsuario do path
        String idUsuario = event.getPathParameters() != null ? event.getPathParameters().get("idUsuario") : null;
        if (idUsuario == null) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("ID do usuário obrigatório");
        }

        // Query no GSI por idUsuario
        Map<String, AttributeValue> expressionValues = Map.of(":idUsuario", AttributeValue.fromS(idUsuario));
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .indexName("idUsuario-index") // Nome do GSI, deve ser criado na tabela
                .keyConditionExpression("idUsuario = :idUsuario")
                .expressionAttributeValues(expressionValues)
                .build();

        try {
            QueryResponse result = dynamoDbClient.query(queryRequest);
            List<Map<String, Object>> responseData = new ArrayList<>();

            // Mapear resultados para formato JSON
            for (Map<String, AttributeValue> item : result.items()) {
                Map<String, Object> device = new HashMap<>();
                device.put("id", item.get("id").s());
                device.put("idUsuario", item.get("idUsuario").s());
                device.put("token", item.get("token").s());
                device.put("nome", item.get("nome").s());
                device.put("tipo", item.get("tipo").s());
                device.put("situacao", item.get("situacao").s());
                responseData.add(device);
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(responseData))
                    .withHeaders(Map.of("Content-Type", "application/json"));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Erro ao buscar dispositivos: " + e.getMessage());
        }
    }
}