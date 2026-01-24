package de.halbmann.sam;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

@Startup
@ApplicationScoped
public class DatabaseConfig {

  @Inject DataSource dataSource;

  @PostConstruct
  void configure() throws SQLException {
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {

      s.execute("SET pg_trgm.similarity_threshold = 0.35");
    }
  }
}
