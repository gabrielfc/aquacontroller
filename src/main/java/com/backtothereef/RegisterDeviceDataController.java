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
        import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
        import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

        import java.time.Instant;
        import java.time.ZoneId;
        import java.time.format.DateTimeFormatter;
        import java.util.HashMap;
        import java.util.Map;

public class RegisterDeviceDataController implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String dispositivoTable = System.getenv("TABLE_DISPOSITIVO");
    private final String dadosTable = System.getenv("TABLE_DADOS");

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));

    public RegisterDeviceDataController() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(System.getenv("AWS_REGION")))
                .build();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String token = event.getHeaders().get("X-Token");
        if (token == null) {
            return new APIGatewayProxyResponseEvent().withStatusCode(401).withBody("Token obrigatório no header");
        }

        record DadosRequest(Double temperatura, Double humidade) {}
        DadosRequest req;
        try {
            req = new com.google.gson.Gson().fromJson(event.getBody(), DadosRequest.class);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Erro ao parsear JSON");
        }

        // Verificar dispositivo por token (usando GSI)
        Map<String, AttributeValue> expressionValues = Map.of(":token", AttributeValue.fromS(token));
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(dispositivoTable)
                .indexName("token-index")
                .keyConditionExpression("token = :token")
                .expressionAttributeValues(expressionValues)
                .limit(1)
                .build();

        QueryResponse result = dynamoDbClient.query(queryRequest);
        if (result.count() == 0) {
            return new APIGatewayProxyResponseEvent().withStatusCode(401).withBody("Dispositivo não encontrado");
        }

        String idDispositivo = result.items().get(0).get("id").s();

        // Criar timestamp e TTL
        Instant now = Instant.now();
        String timestamp = TIMESTAMP_FORMAT.format(now);
        long ttl = now.plusSeconds(7 * 24 * 3600).getEpochSecond();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("idDispositivo", AttributeValue.fromS(idDispositivo));
        item.put("timestamp", AttributeValue.fromS(timestamp));
        item.put("temperatura", AttributeValue.fromN(String.valueOf(req.temperatura)));
        item.put("humidade", AttributeValue.fromN(String.valueOf(req.humidade)));
        item.put("ttl", AttributeValue.fromN(String.valueOf(ttl)));

        try {
            dynamoDbClient.putItem(PutItemRequest.builder().tableName(dadosTable).item(item).build());
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Dados salvos com sucesso");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Erro ao salvar dados");
        }
    }
}