# 🌾 SGPA — Sistema de Venda de Produtos Agrícolas Interprovincial

Plataforma em **Java / Spring Boot** que liga produtores agrícolas de várias províncias de Moçambique a compradores: publicação de ofertas, pedidos e pagamento digital (M-Pesa, e-Mola, MKesh ou banco).

O projecto expõe **duas interfaces sobre o mesmo domínio**:

| Interface | Base | Autenticação | Para quê |
|---|---|---|---|
| **API REST** (`/api/v1`) | JSON | JWT (Bearer), stateless | Apps web/móveis, integrações |
| **Site MVC** (Thymeleaf) | HTML | sessão própria | Interface web original |

- Documentação interactiva da API: `http://localhost:8010/swagger-ui.html`
- Especificação OpenAPI: `http://localhost:8010/v3/api-docs/v1`
- Referência dos endpoints: [`docs/API.md`](docs/API.md)

---

## 📋 Índice

1. [Tecnologias](#-tecnologias)
2. [Estrutura do projecto](#-estrutura-do-projecto)
3. [Pré-requisitos](#-pré-requisitos)
4. [Configuração](#-configuração)
5. [Executar](#-executar)
6. [Primeiros passos com a API](#-primeiros-passos-com-a-api)
7. [Perfis de utilizador](#-perfis-de-utilizador)
8. [Modelo de dados](#-modelo-de-dados)
9. [Segurança](#-segurança)
10. [Limitações conhecidas e próximos passos](#-limitações-conhecidas-e-próximos-passos)

---

## 🛠️ Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem / framework | Java (versão em `pom.xml`), Spring Boot 3.5.x |
| API | Spring Web, Spring Security (OAuth2 Resource Server, JWT HS256), Bean Validation |
| Documentação | springdoc-openapi (Swagger UI) |
| Persistência | Spring Data JPA, Hibernate, MySQL 8 |
| Site MVC | Thymeleaf, Bootstrap 5.3, jQuery 3.7 |
| E-mail | Spring Mail (SMTP) |
| Build | Maven (wrapper incluído) |

---

## 📁 Estrutura do projecto

```text
src/main/java/com/example/demo/
├── api/                         # API REST (/api/v1)
│   ├── config/                  # SecurityConfig, JWT, OpenAPI, utilizador autenticado
│   ├── controller/              # *ApiController (Auth, Usuario, Oferta, Pedido, Catalogo, Admin)
│   ├── dto/                     # Records de entrada/saída (AuthDtos, OfertaDtos, ...)
│   ├── exception/               # ApiException + ApiExceptionHandler (RFC 7807)
│   ├── repository/              # Consultas específicas da API (pesquisa de ofertas)
│   └── service/                 # Api*Service, mapeadores, filtros
└── web/                         # Domínio partilhado + site MVC
    ├── config/                  # DataInitializer, WebMvcConfig, migrações
    ├── controller/              # Controllers Thymeleaf (Admin, Produtor, Comprador, Login, ...)
    ├── domain/                  # Entidades JPA
    ├── repository/              # Repositórios Spring Data
    └── service/                 # Regras de negócio partilhadas pela API e pelo site
src/main/resources/
├── application.properties       # Configuração sem segredos (usa variáveis de ambiente)
├── application-local.properties # Segredos locais — NÃO versionado (ver .gitignore)
├── static/                      # CSS / JS / imagens do site MVC
└── templates/                   # Páginas Thymeleaf
docs/API.md                      # Referência da API REST
```

Convenção: o nome do *package* coincide sempre com a pasta; a API depende do domínio (`web.domain`) e dos serviços (`web.service`), nunca o contrário.

---

## ⚙️ Pré-requisitos

- **JDK** na versão definida em `pom.xml` (`java.version`)
- **MySQL 8.0+** em execução (a base `sgpa` é criada automaticamente)
- Maven (ou o `mvnw` incluído)

---

## 🔧 Configuração

Os segredos **não** ficam em `application.properties`. Há duas formas de os fornecer:

**A) Desenvolvimento — perfil `local` (por omissão)**

```bash
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
# editar e preencher: senha da BD, conta/palavra-passe de aplicação do Gmail, segredo JWT
```

`application-local.properties` está no `.gitignore`.

**B) Produção — variáveis de ambiente** (com `SPRING_PROFILES_ACTIVE=prod`)

| Variável | Descrição | Por omissão |
|---|---|---|
| `DB_URL` | URL JDBC do MySQL | `jdbc:mysql://localhost:3306/sgpa?...` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais da BD | `root` / *(vazio)* |
| `JWT_SECRET` | Segredo HS256, **mín. 32 caracteres** (`openssl rand -base64 48`) | aleatório no arranque (tokens perdem-se ao reiniciar) |
| `JWT_EXPIRATION_MINUTES` | Validade do token | `60` |
| `CORS_ORIGINS` | Origens permitidas, separadas por vírgula | `http://localhost:3000,http://localhost:5173` |
| `APP_BASE_URL` | Base dos links enviados por e-mail | `http://localhost:8010` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP (Gmail: palavra-passe de aplicação) | *(vazio)* |
| `MAIL_ENABLED` | Liga/desliga o envio de e-mails | `true` |
| `PORT` | Porta HTTP | `8010` |

---

## 🚀 Executar

```bash
./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows
```

- Site: `http://localhost:8010`
- Swagger UI: `http://localhost:8010/swagger-ui.html`

---

## 🧪 Primeiros passos com a API

```bash
BASE=http://localhost:8010/api/v1

# 1) registo de comprador (recebe um e-mail com um código de 6 dígitos)
curl -X POST $BASE/compradores -H 'Content-Type: application/json' \
  -d '{"nome":"Ana","apelido":"Silva","telefone":"841234567","email":"ana@exemplo.com","senha":"segredo1"}'

# 2) confirmar o e-mail com o código recebido (no site: página /confirmar-codigo)
curl -X POST $BASE/auth/confirmar-email -H 'Content-Type: application/json' -d '{"email":"ana@exemplo.com","codigo":"<CODIGO_DO_EMAIL>"}'

# 3) login → accessToken
TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"ana@exemplo.com","senha":"segredo1"}' | jq -r .accessToken)

# 4) pesquisar ofertas (público) e fazer um pedido (autenticado)
curl "$BASE/ofertas?nome=milho&ordem=preco_asc"
curl -X POST $BASE/pedidos -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"ofertaId":1,"quantidade":50}'
```

No Swagger UI: executar `POST /auth/login`, copiar o `accessToken` e usar **Authorize**.

---

## 👥 Perfis de utilizador

| Perfil | Entrada na API | Capacidades |
|---|---|---|
| Público | — | Ver ofertas, categorias e localização; registar-se; recuperar senha |
| `COMPRADOR` | `POST /auth/login` | Pedidos, pagamentos, cancelamentos |
| `PRODUTOR` | `POST /auth/login` | Ofertas, aceitar/rejeitar pedidos |
| `ADMIN` | `POST /auth/admin/login` | Gestão de utilizadores, vendas, auditoria |
| `SUPER_ADMIN` | `POST /auth/admin/login` | + gestão de administradores |

Quem tem conta de Produtor e de Comprador com o mesmo e-mail pode alternar com `POST /auth/trocar-perfil`.

---

## 🗄️ Modelo de dados

| Entidade | Descrição |
|---|---|
| `Produtor`, `Comprador`, `Admin` | Utilizadores (+ `AdminLog` para auditoria) |
| `Oferta` | Produto à venda: preço, quantidade, validade, localização |
| `Pedido` | Encomenda de um comprador a uma oferta |
| `Transacao` | Pagamento associado a um pedido |
| `Produto`, `Categoria` | Catálogo |
| `Provincia`, `Distrito`, `Bairro` | Hierarquia geográfica |
| `TokenVerificacao` | Tokens de confirmação de e-mail e recuperação de senha |

Estados: **Oferta** `ATIVO → INATIVO → EXPIRADO` · **Pedido** `PENDENTE → AGUARDANDO_PAGAMENTO → ACEITE` (ou `REJEITADO` / `CANCELADO`) · **Transação** `PENDENTE → CONFIRMADO | CANCELADO`.

---

## 🔒 Segurança

- Duas cadeias de segurança: `/api/**` (stateless, JWT) e o site MVC (sessão própria).
- Palavras-passe com hash; migração automática de hashes antigos no primeiro login.
- Bloqueio após 5 tentativas falhadas de login (15 min).
- Erros no formato `application/problem+json` (RFC 7807), com `codigo` estável.
- Segredos fora do repositório (perfil `local` ignorado pelo Git / variáveis de ambiente).
- **Se algum segredo já esteve num repositório público (ex.: GitHub), rode-o:** gere nova palavra-passe de aplicação do Gmail e nova senha da BD.

---

## 📌 Limitações conhecidas e próximos passos

- Sem *refresh token* nem *logout* no servidor; suspender uma conta só impede novos logins.
- O limite de tentativas de login é em memória (uma só instância).
- `ddl-auto=update`: adoptar Flyway/Liquibase antes de produção.
- `open-in-view=true` por causa das páginas Thymeleaf; pode passar a `false` se o site MVC for retirado.
- Imagens guardadas em disco local (`/uploads`); com várias instâncias usar armazenamento partilhado.
- Cobertura de testes mínima (apenas `contextLoads`): adicionar testes de integração (MockMvc) para auth, ofertas e pedidos.

---

## 📄 Licença

Projecto académico da **UJC**. Todos os direitos reservados ao autor: **Mário**.
