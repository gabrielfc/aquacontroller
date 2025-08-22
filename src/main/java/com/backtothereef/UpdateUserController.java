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
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

public class UpdateUserController implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName = System.getenv("TABLE_USUARIO");
    private final String adminToken = System.getenv("ADMIN_TOKEN");

    public UpdateUserController() {
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

        // Parse body
        record UsuarioUpdateRequest(String login, String email, String senha, String situacao) {}
        UsuarioUpdateRequest req;
        try {
            req = new com.google.gson.Gson().fromJson(event.getBody(), UsuarioUpdateRequest.class);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Erro ao parsear JSON");
        }

        // Preparar update
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        StringBuilder updateExpression = new StringBuilder("SET ");
        boolean hasUpdate = false;

        if (req.login != null) {
            updateExpression.append("login = :login, ");
            expressionValues.put(":login", AttributeValue.fromS(req.login));
            hasUpdate = true;
        }
        if (req.email != null) {
            updateExpression.append("email = :email, ");
            expressionValues.put(":email", AttributeValue.fromS(req.email));
            hasUpdate = true;
        }
        if (req.senha != null) {
            String hashed = BCrypt.hashpw(req.senha, BCrypt.gensalt());
            updateExpression.append("senha = :senha, ");
            expressionValues.put(":senha", AttributeValue.fromS(hashed));
            hasUpdate = true;
        }
        if (req.situacao != null && ("ativo".equals(req.situacao) || "inativo".equals(req.situacao))) {
            updateExpression.append("situacao = :situacao, ");
            expressionValues.put(":situacao", AttributeValue.fromS(req.situacao));
            hasUpdate = true;
        }

        if (!hasUpdate) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Nenhum campo para atualizar");
        }

        // Remover última vírgula
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
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Usuário atualizado");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Erro ao atualizar usuário");
        }
    }
}