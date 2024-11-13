package com.delta.dms.community.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfiguration;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

public enum MariaDB4jManager {
  INSTANCE;

  DB db = null;
  Connection connection = null;

  private MariaDB4jManager() {}

  public void create() throws ManagedProcessException, SQLException {
    if (db == null) {
      DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
      configBuilder.setPort(9999);
      DBConfiguration config = configBuilder.build();
      db = DB.newEmbeddedDB(config);
      db.start();
      db.createDB("dms_community");
      db.source("sql/schema.sql");
      connection = DriverManager.getConnection(configBuilder.getURL("dms_community"), "root", "");
    }
  }

  public DB getDb() {
    return db;
  }

  public Connection getConnection() {
    return connection;
  }
}
