package com.example.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;

@SpringBootApplication(exclude = {
    JpaRepositoriesAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    SqlInitializationAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    TransactionAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration.class,
    org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration.class
})
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
