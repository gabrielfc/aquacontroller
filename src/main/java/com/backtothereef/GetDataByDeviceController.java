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

public class GetDataByDeviceController implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName = System.getenv("TABLE_DADOS");
    private final String adminToken = System.getenv("ADMIN_TOKEN");
    private final Gson gson = new Gson();

    public GetDataByDeviceController() {
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

        // Obter idDispositivo do path
        String idDispositivo = event.getPathParameters() != null ? event.getPathParameters().get("idDispositivo") : null;
        if (idDispositivo == null) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("ID do dispositivo obrigatório");
        }

        // Query na tabela Dispositivo_Dados
        Map<String, AttributeValue> expressionValues = Map.of(":idDispositivo", AttributeValue.fromS(idDispositivo));
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("idDispositivo = :idDispositivo")
                .expressionAttributeValues(expressionValues)
                .build();

        try {
            QueryResponse result = dynamoDbClient.query(queryRequest);
            List<Map<String, Object>> responseData = new ArrayList<>();

            // Mapear resultados para formato JSON
            for (Map<String, AttributeValue> item : result.items()) {
                Map<String, Object> data = new HashMap<>();
                data.put("timestamp", item.get("timestamp").s());
                data.put("temperatura", Double.parseDouble(item.get("temperatura").n()));
                data.put("humidade", Double.parseDouble(item.get("humidade").n()));
                responseData.add(data);
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(responseData))
                    .withHeaders(Map.of("Content-Type", "application/json"));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Erro ao buscar dados: " + e.getMessage());
        }
    }
}