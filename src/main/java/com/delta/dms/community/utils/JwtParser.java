package com.delta.dms.community.utils;

import com.delta.datahive.utils.JWTParser.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public class JwtParser {

  public static final JwtParser instance = new JwtParser();
  private com.delta.datahive.utils.JWTParser parser;

  public JwtParser() {
    parser = new com.delta.datahive.utils.JWTParser();
  }

  public Jws<Claims> parse(String jwt) {
    return parser.parse(jwt);
  }

  private UserEntity parseUser(String jwt) {
    return parser.parseUser(jwt);
  }

  public String parseUserId(String jwt) {
    return parseUser(jwt).getUuid();
  }

  public String parseUserName(String jwt) {
    return parseUser(jwt).getName();
  }

  public long parseExp(String jwt) {
    return parser.parseExp(jwt);
  }
}
