# 🌾 Sistema de Venda de Produtos Agrícolas Interprovincial

Plataforma web desenvolvida em **Java com Spring Boot** que conecta produtores agrícolas de diferentes províncias de Moçambique a compradores, permitindo a publicação de ofertas, realização de pedidos e processamento de pagamentos de forma digital.

---

## 📋 Índice

- [Sobre o Projecto](#sobre-o-projecto)
- [Funcionalidades](#funcionalidades)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Pré-requisitos](#pré-requisitos)
- [Instalação e Configuração](#instalação-e-configuração)
- [Estrutura do Projecto](#estrutura-do-projecto)
- [Modelos de Dados](#modelos-de-dados)
- [Fluxo de Utilização](#fluxo-de-utilização)
- [Perfis de Utilizador](#perfis-de-utilizador)

---

## 📖 Sobre o Projecto

O **SGPA** (Sistema de Gestão de Produtos Agrícolas) é uma aplicação web MVC que visa facilitar o comércio interprovincial de produtos agrícolas em Moçambique. A plataforma permite que produtores publiquem as suas ofertas com localização geográfica detalhada (província, distrito e bairro), e que compradores pesquisem, façam pedidos e paguem de forma digital através de carteiras móveis (M-Pesa, e-Mola, MKesh) ou transferência bancária.

---

## ✅ Funcionalidades

### Para Produtores
- Registo e autenticação com foto de perfil
- Publicação de ofertas com imagem, categoria, quantidade, preço e validade
- Gestão de ofertas activas, inactivas e expiradas
- Recepção e gestão de pedidos de compradores
- Histórico de transacções e vendas
- Possibilidade de actuar também como comprador

### Para Compradores
- Registo e autenticação
- Pesquisa e filtro de ofertas por categoria e localização
- Realização de pedidos e acompanhamento do estado
- Pagamento via conta móvel (M-Pesa, e-Mola, MKesh) ou banco
- Histórico de compras
- Possibilidade de actuar também como produtor

### Para Administradores
- Painel de controlo com visão geral do sistema
- Gestão de produtores e compradores (ativar/suspender)
- Visualização de todas as vendas e transacções
- Registo de histórico de acções administrativas (log de auditoria)
- Gestão de outros administradores

---

## 🛠️ Tecnologias Utilizadas

| Camada | Tecnologia |
|---|---|
| Back-end | Java 17, Spring Boot 3.5.x |
| Persistência | Spring Data JPA, Hibernate, MySQL 8 |
| Front-end | Thymeleaf, Bootstrap 5.3, jQuery 3.7.1 |
| Build | Maven |
| IDE recomendada | Eclipse / VS Code |

### Dependências principais

- `spring-boot-starter-web` — Servidor web embutido (Tomcat)
- `spring-boot-starter-thymeleaf` — Motor de templates HTML
- `spring-boot-starter-data-jpa` — Acesso a dados com JPA/Hibernate
- `spring-boot-starter-validation` — Validação de formulários
- `mysql-connector-j` — Conector MySQL
- `webjars/bootstrap` + `webjars/jquery` — UI responsiva
- `jquery-mask-plugin` — Máscaras de campos de formulário
- `spring-boot-devtools` — Hot reload em desenvolvimento

---

## ⚙️ Pré-requisitos

Antes de executar o projecto, certifique-se de ter instalado:

- **Java JDK 17** ou superior
- **Maven 3.6+**
- **MySQL 8.0+**
- Git (opcional)

---

## 🚀 Instalação e Configuração

### 1. Clonar o repositório

bash
git clone <https://github.com/MariodeMarioNhanengue/SGPA.git>
cd Sistema_De_Venda_De_Produtos_Agricolas_Interprovincial


### 2. Configurar a base de dados

Crie o utilizador MySQL (ou use o `root`) e garanta que o MySQL está em execução. O sistema cria automaticamente a base de dados `sgpa` ao arrancar.

### 3. Editar o ficheiro de configuração

Abra `src/main/resources/application.properties` e ajuste as credenciais:

properties
# Base de dados
spring.datasource.url=jdbc:mysql://localhost:3306/sgpa?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=SUA_SENHA_AQUI

# Servidor
server.port=8010


> ⚠️ **Importante:** Não envie as suas credenciais reais para um repositório público. Use variáveis de ambiente em produção.

### 4. Executar o projecto
bash
mvn spring-boot:run


A aplicação estará disponível em: **http://localhost:8010**

### 5. Upload de imagens

Os ficheiros enviados (fotos de perfil e imagens de ofertas) são armazenados localmente. O tamanho máximo por ficheiro é **20 MB**.

---

## 📁 Estrutura do Projecto


src/
└── main/
    ├── java/com/example/demo/
    │   └── web/
    │       ├── config/
    │       │   ├── DataInitializer.java       # Dados iniciais (províncias, distritos, bairros)
    │       │   ├── DatabaseMigrationRunner.java
    │       │   └── WebMvcConfig.java          # Configuração de recursos estáticos e uploads
    │       ├── controller/
    │       │   ├── AdminController.java       # Painel de administração
    │       │   ├── CompradorController.java   # Área do comprador
    │       │   ├── ProdutorController.java    # Área do produtor
    │       │   ├── LoginController.java       # Autenticação
    │       │   ├── HomeControler.java         # Página inicial
    │       │   └── LocalizacaoController.java # API de dados geográficos
    │       ├── domain/                        # Entidades JPA
    │       │   ├── Produtor.java
    │       │   ├── Comprador.java
    │       │   ├── Admin.java / AdminLog.java
    │       │   ├── Oferta.java
    │       │   ├── Pedido.java
    │       │   ├── Transacao.java
    │       │   ├── Produto.java / Categoria.java
    │       │   ├── Provincia.java / Distrito.java / Bairro.java
    │       │   └── ProdutorProduto.java
    │       └── repository/                    # Repositórios Spring Data JPA
    └── resources/
        ├── application.properties
        ├── static/
        │   ├── css/                           # Estilos (comprador, produtor, login, registo)
        │   └── js/                            # Scripts (dashboards, geográfica, pagamentos)
        └── templates/
            ├── index.html / login.html / como-funciona.html
            ├── admin/                         # Templates do painel admin
            ├── comprador/                     # Templates da área do comprador
            └── produtor/                      # Templates da área do produtor


---

## 🗄️ Modelos de Dados

### Entidades principais

| Entidade | Descrição |
|---|---|
| `Produtor` | Utilizador que publica ofertas agrícolas |
| `Comprador` | Utilizador que realiza pedidos |
| `Admin` | Administrador do sistema com log de acções |
| `Oferta` | Publicação de produto com preço, quantidade e validade |
| `Pedido` | Encomenda de um comprador a uma oferta |
| `Transacao` | Registo de pagamento associado a um pedido |
| `Produto` / `Categoria` | Catálogo de produtos agrícolas |
| `Provincia` / `Distrito` / `Bairro` | Hierarquia geográfica de Moçambique |

### Estados de uma Oferta


ATIVO  →  INATIVO  →  EXPIRADO


### Métodos de Pagamento (Transacao)

- **Conta Móvel:** M-Pesa, e-Mola, MKesh
- **Banco:** transferência bancária

### Estados de Transação

- `PENDENTE` → `CONFIRMADO` / `CANCELADO`

---

## 🔄 Fluxo de Utilização


Produtor regista-se
    └─► Publica oferta (produto, preço, localização, imagem)
            └─► Comprador pesquisa e faz pedido
                    └─► Comprador realiza pagamento (conta móvel ou banco)
                            └─► Produtor confirma e entrega
                                    └─► Transacção registada no histórico


---

## 👥 Perfis de Utilizador

O sistema suporta **duplo perfil**: um utilizador registado como Comprador pode tornar-se também Produtor (e vice-versa), sem necessidade de criar uma conta separada.

| Perfil | Acesso | URL base |
|---|---|---|
| Administrador | Gestão global do sistema | `/admin/...` |
| Produtor | Gestão de ofertas e pedidos | `/produtor/...` |
| Comprador | Pesquisa, pedidos e pagamentos | `/comprador/...` |

---

## 📄 Licença

Este projecto foi desenvolvido para fins académicos na **UJC** (Universidade). Todos os direitos reservados aos autores: **Mário**, **Castigo** e **Álvaro**.
