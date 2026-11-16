package com.example.cobro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Ajustes de esquema que {@code ddl-auto=update} no realiza por sí solo.
 *
 * <p>Hibernate 6 crea las columnas de enums como ENUM (H2) o con una restricción CHECK (PostgreSQL)
 * que lista los valores existentes al crear la tabla. En bases creadas antes de agregar
 * {@code EstadoPago.PAGADO} ese valor sería rechazado, así que la columna se convierte a VARCHAR
 * o se elimina el CHECK obsoleto. Se ejecuta antes que cualquier otro CommandLineRunner.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MigracionEsquema implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionEsquema.class);
    private static final String[] TABLAS_CON_ESTADO = {"prestamos", "pagos"};

    private final DataSource dataSource;

    public MigracionEsquema(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection con = dataSource.getConnection(); Statement st = con.createStatement()) {
            String motor = con.getMetaData().getDatabaseProductName().toLowerCase();
            for (String tabla : TABLAS_CON_ESTADO) {
                String sql;
                if (motor.contains("h2")) {
                    sql = "ALTER TABLE " + tabla + " ALTER COLUMN estado SET DATA TYPE VARCHAR(20)";
                } else if (motor.contains("postgres")) {
                    sql = "ALTER TABLE " + tabla + " DROP CONSTRAINT IF EXISTS " + tabla + "_estado_check";
                } else {
                    return;
                }
                try {
                    st.execute(sql);
                } catch (Exception e) {
                    log.warn("No se pudo ajustar la columna estado de {}: {}", tabla, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Migración de esquema omitida: {}", e.getMessage());
        }
    }
}
