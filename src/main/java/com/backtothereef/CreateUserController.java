package com.backtothereef;


// 1. Handler para Criar Usuário (POST /usuarios)
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateUserController implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName = System.getenv("TABLE_USUARIO");
    private final String adminToken = System.getenv("ADMIN_TOKEN");

    public CreateUserController() {
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

        // Parse body (use record para request)
        record UsuarioRequest(String login, String email, String senha, String situacao) {}
        UsuarioRequest req;
        try {
            req = new com.google.gson.Gson().fromJson(event.getBody(), UsuarioRequest.class);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Erro ao parsear JSON");
        }

        // Validações
        if (req.login == null || req.email == null || req.senha == null ||
                (!"ativo".equals(req.situacao) && !"inativo".equals(req.situacao))) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Campos inválidos");
        }

        // Criptografar senha
        String hashedSenha = BCrypt.hashpw(req.senha, BCrypt.gensalt());

        String id = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.fromS(id));
        item.put("login", AttributeValue.fromS(req.login));
        item.put("email", AttributeValue.fromS(req.email));
        item.put("senha", AttributeValue.fromS(hashedSenha));
        item.put("situacao", AttributeValue.fromS(req.situacao));

        try {
            dynamoDbClient.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
            return new APIGatewayProxyResponseEvent().withStatusCode(201).withBody("{\"id\": \"" + id + "\"}");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Erro ao salvar usuário");
        }
    }
}