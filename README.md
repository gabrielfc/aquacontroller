# Aqua Controller

Bem-vindo ao **Aqua Controller**, um projeto de software livre para monitoramento e controle remoto de aquários. Este sistema permite que aquaristas configurem, parametrizem, monitorem e controlem seus aquários de forma prática e eficiente, utilizando uma arquitetura serverless na nuvem e um aplicativo móvel em Flutter. O projeto é open-source, permitindo contribuições da comunidade para aprimorar suas funcionalidades.

## Sobre o Projeto

O Aquarium Controller é uma solução completa para automação de aquários, projetada para oferecer controle remoto e monitoramento em tempo real de parâmetros como temperatura e umidade. Desenvolvido com tecnologias modernas, o sistema é escalável, de baixo custo e acessível, rodando na camada gratuita da AWS (Free Tier) para protótipos e pequenos aquários. O aplicativo Flutter proporciona uma interface amigável para usuários, enquanto a API serverless garante integração robusta com dispositivos IoT.

### Funcionalidades

- **Configuração e Parametrização**: Registre usuários e dispositivos (sensores) para personalizar o monitoramento de cada aquário.
- **Monitoramento Remoto**: Acesse dados de temperatura e umidade em tempo real, coletados por sensores conectados ao aquário, seja notificado em caso de mudanças, falhas ou indisponibilidade.
- **Controle Remoto**: (Futuro) Permite ajustar parâmetros como iluminação ou bombas diretamente pelo aplicativo.
- **Autenticação Segura**: Login de usuários com validação de credenciais e gerenciamento de dispositivos associados.
- **Armazenamento Eficiente**: Dados de sensores são salvos com TTL de 7 dias no DynamoDB, otimizando custos.
- **Interface Intuitiva**: Aplicativo Flutter para Android/iOS, com telas para login, lista de dispositivos e visualização de dados.

## Arquitetura

O projeto é composto por:

- **Backend (AWS)**:
  - **Amazon API Gateway**: Expondo endpoints RESTful para login, gerenciamento de usuários/dispositivos e coleta de dados.
  - **AWS Lambda**: Funções serverless em Java 21 para processar requisições e interagir com o banco.
  - **Amazon DynamoDB**: Banco NoSQL para armazenar dados de usuários, dispositivos e medições (temperatura e umidade).
- **Frontend**:
  - **Flutter App**: Aplicativo multiplataforma para login, visualização de dispositivos e dados dos sensores.
- **Dispositivos IoT**: Sensores de temperatura e umidade que enviam dados via HTTP para a API, autenticados por tokens.

## Pré-requisitos

- **AWS Account**: Conta AWS com acesso ao Free Tier para API Gateway, Lambda e DynamoDB.
- **Flutter**: SDK instalado (versão 3.0.0 ou superior) para rodar o aplicativo.
- **Dispositivos IoT**: Sensores configurados para enviar dados via HTTP com token de autenticação.

## Como Configurar

1. **Backend (AWS)**:

   - Crie as tabelas DynamoDB (`Usuario`, `Dispositivo`, `Dispositivo_Dados`) com GSIs (`login-index`, `idUsuario-index`, `token-index`) e TTL habilitado.
   - Deploy as funções Lambda usando os ZIPs gerados pelo `pom.xml` (instruções no diretório `backend/`).
   - Configure o API Gateway com os endpoints:
     - `POST /login`
     - `POST /usuarios`, `PUT /usuarios/{id}`
     - `POST /dispositivos`, `PUT /dispositivos/{id}`
     - `POST /dados`
     - `GET /dados/{idDispositivo}`
     - `GET /dispositivos/usuario/{idUsuario}`
   - Defina as variáveis de ambiente nas Lambdas: `TABLE_USUARIO`, `TABLE_DISPOSITIVO`, `TABLE_DADOS`, `ADMIN_TOKEN`, `AWS_REGION`.

2. **Frontend (Flutter)**:

   - Clone o repositório e navegue até `frontend/`.
   - Atualize o `pubspec.yaml` e execute `flutter pub get`.
   - Substitua a URL base da API e o `ADMIN_TOKEN` nos arquivos `home_screen.dart` e `device_data_screen.dart`.
   - Execute o aplicativo: `flutter run`.

3. **Dispositivos IoT**:
   - Configure sensores para enviar POST para `/dados` com header `X-Token` e corpo JSON `{ "temperatura": 25.5, "humidade": 60.0 }`.

## Como Usar

1. Abra o aplicativo Flutter e faça login com suas credenciais.
2. Na tela inicial, visualize a lista de dispositivos associados ao seu usuário.
3. Toque em um dispositivo para ver os dados de temperatura e umidade coletados.
4. Use o botão de logout para sair da sessão.

## Contribuindo

Este é um projeto open-source, e contribuições são bem-vindas! Para contribuir:

- Fork o repositório.
- Crie uma branch para sua feature (`git checkout -b feature/nova-funcionalidade`).
- Commit suas alterações (`git commit -m 'Adiciona nova funcionalidade'`).
- Envie um pull request.

## Licença

Este projeto é licenciado sob a **GNU General Public License v3.0**, garantindo que o software e todos os seus derivados permaneçam livres e de código aberto. Isso significa que qualquer modificação ou uso do código deve ser distribuído sob a mesma licença, com o código-fonte disponibilizado. Veja o arquivo `LICENSE` para detalhes completos.

## Contato

Para dúvidas, sugestões ou suporte, abra uma issue no GitHub ou entre em contato com a comunidade de aquaristas!
