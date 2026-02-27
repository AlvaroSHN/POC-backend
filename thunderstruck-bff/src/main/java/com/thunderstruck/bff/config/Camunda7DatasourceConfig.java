package com.thunderstruck.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class Camunda7DatasourceConfig {

    @Bean(name = {"dataSource", "camundaBpmDataSource"})
    @Primary
    public DataSource camundaDataSource(
            @Value("${spring.datasource.url:jdbc:h2:mem:camunda;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE}") String url,
            @Value("${spring.datasource.driver-class-name:org.h2.Driver}") String driver,
            @Value("${spring.datasource.username:sa}") String user,
            @Value("${spring.datasource.password:}") String pass
    ) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driver);
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(pass);
        return ds;
    }

    @Bean(name = {"transactionManager", "camundaBpmTransactionManager"})
    @Primary
    public PlatformTransactionManager camundaTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
