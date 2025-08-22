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
        import org.mindrot.jbcrypt.BCrypt;
        import com.google.gson.Gson;

        import java.util.HashMap;
        import java.util.Map;

public class LoginUserController implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName = System.getenv("TABLE_USUARIO");
    private final Gson gson = new Gson();

    public LoginUserController() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.of(System.getenv("AWS_REGION")))
                .build();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        record LoginRequest(String login, String senha) {}
        LoginRequest req;
        try {
            req = gson.fromJson(event.getBody(), LoginRequest.class);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Erro ao parsear JSON");
        }

        if (req.login == null || req.senha == null) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Login e senha obrigatórios");
        }

        // Query por login (assume GSI "login-index" na tabela Usuario com "login" como chave de partição)
        Map<String, AttributeValue> expressionValues = Map.of(":login", AttributeValue.fromS(req.login));
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .indexName("login-index")
                .keyConditionExpression("login = :login")
                .expressionAttributeValues(expressionValues)
                .limit(1)
                .build();

        try {
            QueryResponse result = dynamoDbClient.query(queryRequest);
            if (result.count() == 0) {
                return new APIGatewayProxyResponseEvent().withStatusCode(401).withBody("Usuário ou senha inválidos");
            }

            Map<String, AttributeValue> item = result.items().get(0);
            String storedSenha = item.get("senha").s();
            String situacao = item.get("situacao").s();

            if (!BCrypt.checkpw(req.senha, storedSenha) || !"ativo".equals(situacao)) {
                return new APIGatewayProxyResponseEvent().withStatusCode(401).withBody("Usuário ou senha inválidos");
            }

            String id = item.get("id").s();
            Map<String, Object> response = Map.of("authenticated", true, "id", id);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(response))
                    .withHeaders(Map.of("Content-Type", "application/json"));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Erro ao processar login");
        }
    }
}