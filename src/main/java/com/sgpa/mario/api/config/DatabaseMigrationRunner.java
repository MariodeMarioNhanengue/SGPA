package com.sgpa.mario.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Corre automaticamente ao arrancar a aplicação.
 * Adiciona o valor CANCELADO ao ENUM 'estado' da tabela 'pedido'
 * caso ainda não exista — operação idempotente e segura para re-execuções.
 */
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final JdbcTemplate jdbc;

    public DatabaseMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrarEnumEstadoPedido();
    }

    private void migrarEnumEstadoPedido() {
        try {
            // Consulta os valores actuais do ENUM directamente no information_schema
            String sql = """
                    SELECT COLUMN_TYPE
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME   = 'pedido'
                      AND COLUMN_NAME  = 'estado'
                    """;

            String columnType = jdbc.queryForObject(sql, String.class);

            if (columnType != null && !columnType.contains("CANCELADO") ) {
                log.info("[Migração] Adicionando CANCELADO ao ENUM estado da tabela pedido...");
                jdbc.execute("""
                        ALTER TABLE pedido
                          MODIFY COLUMN estado
                            ENUM('PENDENTE','ACEITE','REJEITADO','CANCELADO')
                            NOT NULL
                        """);
                log.info("[Migração] ENUM actualizado com sucesso.");
            } else {
                log.debug("[Migração] ENUM estado já contém CANCELADO — nada a fazer.");
            }

        } catch (Exception e) {
            // Não impede o arranque da aplicação; regista o erro para diagnóstico
            log.error("[Migração] Falha ao migrar ENUM estado: {}", e.getMessage(), e);
        }
    }
}
