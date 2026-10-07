# SGPA — API REST v1

Base URL: `http://localhost:8010/api/v1` · Documentação interactiva: `http://localhost:8010/swagger-ui.html`

A API é **stateless** (JWT no cabeçalho `Authorization: Bearer <token>`), devolve JSON e erros no formato
RFC 7807 (`application/problem+json`). O site Thymeleaf continua a funcionar como antes, em paralelo.

## 1. Configuração

Os segredos ficam fora do `application.properties`: em desenvolvimento no perfil `local`
(`application-local.properties`, não versionado — ver `application-local.properties.example`) e em produção
em variáveis de ambiente (`SPRING_PROFILES_ACTIVE=prod`).

| Propriedade | Variável | Descrição |
|---|---|---|
| `app.jwt.secret` | `JWT_SECRET` | **Obrigatório em produção**, mín. 32 caracteres. Sem isto gera-se um segredo temporário e os tokens perdem-se ao reiniciar |
| `app.jwt.expiration-minutes` | `JWT_EXPIRATION_MINUTES` | Validade do token (por omissão 60) |
| `app.cors.allowed-origins` | `CORS_ORIGINS` | Origens permitidas para a app web/móvel |
| `app.base-url` | `APP_BASE_URL` | Base dos links dos e-mails |
| `spring.datasource.*` / `spring.mail.*` | `DB_*` / `MAIL_*` | Base de dados e SMTP |

Ao arrancar, o Hibernate acrescenta a coluna `ativo` às tabelas `produtor` e `comprador` (contas antigas contam como activas).

## 2. Perfis (roles)

| Perfil | Como entra | Pode |
|---|---|---|
| público | — | ver ofertas/categorias/localização, registar-se, recuperar senha |
| `COMPRADOR` | `POST /auth/login` | fazer pedidos, pagar, cancelar |
| `PRODUTOR` | `POST /auth/login` | gerir ofertas, aceitar/rejeitar pedidos |
| `ADMIN` | `POST /auth/admin/login` | área `/admin/**` |
| `SUPER_ADMIN` | `POST /auth/admin/login` | + gerir administradores |

Quem tem conta de Produtor e de Comprador com o mesmo e-mail pode trocar com `POST /auth/trocar-perfil`.

## 3. Endpoints

### Autenticação
| Método | Caminho | Descrição |
|---|---|---|
| POST | `/auth/login` | `{email, senha, tipo?}` → `{accessToken, tokenType, expiresIn, usuario}` |
| POST | `/auth/admin/login` | login de administrador |
| POST | `/auth/trocar-perfil` | `{tipo: "PRODUTOR"\|"COMPRADOR"}` → novo token |
| POST | `/auth/confirmar-email` | `{token}` |
| POST | `/auth/reenviar-confirmacao` | `{email}` |
| POST | `/auth/recuperar-senha` | `{email}` |
| POST | `/auth/redefinir-senha` | `{token, novaSenha, confirmarSenha}` |

### Utilizadores
| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| POST | `/compradores` | público | registo de comprador (201) |
| POST | `/produtores` | público | registo de produtor (201) |
| GET | `/me` | autenticado | o meu perfil |
| PUT | `/me` | autenticado | actualizar perfil |
| PUT | `/me/foto` | autenticado | multipart, campo `foto` |
| PUT | `/me/senha` | autenticado | `{senhaAtual, novaSenha}` (204) |

### Ofertas
| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| GET | `/ofertas` | público | filtros: `nome, categoria, provinciaId, distritoId, bairroId, precoMin, precoMax, produtorId, estado, dataInicio, dataFim`; `ordem=recentes\|preco_asc\|preco_desc\|nome`; `pagina`, `tamanho` (máx. 100) |
| GET | `/ofertas/{id}` | público | detalhe |
| POST | `/ofertas` | PRODUTOR | publicar (201 + `Location`) |
| PUT | `/ofertas/{id}` | PRODUTOR (dono) | editar |
| PATCH | `/ofertas/{id}/estado` | PRODUTOR (dono) | `{estado: ATIVO\|INATIVO\|EXPIRADO}` |
| PUT | `/ofertas/{id}/imagem` | PRODUTOR (dono) | multipart, campo `imagem` |
| DELETE | `/ofertas/{id}` | PRODUTOR (dono) | apagar (409 se tiver pedidos) |
| GET | `/produtor/ofertas` | PRODUTOR | as minhas ofertas |
| GET | `/produtor/resumo` | PRODUTOR | contadores do painel |

### Pedidos e pagamentos
Fluxo: `PENDENTE` → (produtor aceita) `AGUARDANDO_PAGAMENTO` → (comprador paga) `ACEITE`. Ainda: `REJEITADO`, `CANCELADO`.

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| POST | `/pedidos` | COMPRADOR | `{ofertaId, quantidade}` (201) |
| GET | `/pedidos?estado=` | COMPRADOR / PRODUTOR | os meus pedidos / pedidos recebidos |
| GET | `/pedidos/{id}` | dono, produtor da oferta, admin | detalhe com transacção |
| POST | `/pedidos/{id}/aceitar` | PRODUTOR | `{numeroPagamento}` (só se PENDENTE) |
| POST | `/pedidos/{id}/rejeitar` | PRODUTOR | `{motivo}` |
| POST | `/pedidos/{id}/cancelar` | COMPRADOR | só se PENDENTE |
| POST | `/pedidos/{id}/pagamento` | COMPRADOR | `{metodoPagamento: CONTA_MOVEL\|BANCO, operadora, referencia?}` |

Operadoras: `CONTA_MOVEL` → `MPESA, EMOLA, MKESH, OUTROS`; `BANCO` → `BCI, BIM, STANDARD_BANK, VISA, OUTROS`.

### Catálogo e localização
| Método | Caminho | Perfil |
|---|---|---|
| GET | `/categorias`, `/produtos?nome=&categoria=` | público |
| GET | `/provincias`, `/provincias/{id}/distritos`, `/distritos/{id}/bairros` | público |
| POST | `/provincias`, `/distritos`, `/bairros` | ADMIN |

### Administração (`/admin/**`, ROLE ADMIN)
| Método | Caminho | Descrição |
|---|---|---|
| GET | `/admin/resumo` | estatísticas |
| GET / POST | `/admin/produtores`, `/admin/compradores` | listar (`pesquisa`, paginação) / criar |
| GET | `/admin/produtores/{id}`, `/admin/compradores/{id}` | detalhe com contagens |
| POST | `/admin/{produtores\|compradores}/{id}/suspender` · `/ativar` | suspende / reactiva (a conta suspensa não consegue entrar) |
| DELETE | `/admin/{produtores\|compradores}/{id}` | remover (409 se tiver dados associados) |
| GET | `/admin/vendas?estado=` | todos os pedidos |
| GET | `/admin/historico` | auditoria (SUPER_ADMIN vê tudo) |
| GET / POST / DELETE | `/admin/administradores[/{id}]` | só SUPER_ADMIN |

## 4. Exemplo rápido

```bash
# registo + (confirmar o e-mail) + login
curl -X POST localhost:8010/api/v1/compradores -H 'Content-Type: application/json' \
  -d '{"nome":"Ana","apelido":"Silva","telefone":"841234567","email":"ana@exemplo.com","senha":"segredo1"}'

TOKEN=$(curl -s -X POST localhost:8010/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"ana@exemplo.com","senha":"segredo1"}' | jq -r .accessToken)

# pesquisar ofertas de milho em Gaza, ordenadas por preço
curl "localhost:8010/api/v1/ofertas?nome=milho&provinciaId=2&ordem=preco_asc"

# fazer um pedido
curl -X POST localhost:8010/api/v1/pedidos -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"ofertaId":1,"quantidade":50}'
```

## 5. Erros

```json
{ "title": "Validação falhou", "status": 400, "detail": "Dados inválidos. Verifique os campos indicados.",
  "codigo": "VALIDACAO", "erros": { "email": "E-mail inválido." } }
```

| Estado | `codigo` típico |
|---|---|
| 400 | `VALIDACAO`, `TIPO_INVALIDO`, `ESTADO_INVALIDO` |
| 401 | `NAO_AUTENTICADO`, `CREDENCIAIS_INVALIDAS` |
| 403 | `SEM_PERMISSAO`, `EMAIL_NAO_CONFIRMADO`, `CONTA_SUSPENSA` |
| 404 | `NAO_ENCONTRADO` |
| 409 | `CONFLITO`, `OFERTA_COM_PEDIDOS`, `UTILIZADOR_COM_DADOS`, `ADMIN_COM_HISTORICO` |
| 415 | `IMAGEM_INVALIDA` |
| 422 | `REGRA_NEGOCIO` (e-mail duplicado, stock insuficiente, estado inválido…) |
| 429 | `LOGIN_BLOQUEADO` (5 falhas → 15 min) |

## 6. Limitações conhecidas
- Não há refresh token nem logout no servidor: o token vale até expirar (`expiration-minutes`). Suspender uma conta só impede **novos** logins.
- O limite de tentativas de login é em memória (uma única instância).
- Notificações continuam só por e-mail. Os links dos e-mails apontam para `app.base-url` e abrem as páginas MVC `/confirmar-email` e `/redefinir-senha`; numa app separada, aponte-o para o front-end e use `POST /auth/confirmar-email` e `POST /auth/redefinir-senha`.
- Favoritos não existem no modelo de dados.
