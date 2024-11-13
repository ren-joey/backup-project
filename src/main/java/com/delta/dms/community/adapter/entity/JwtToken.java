package com.delta.dms.community.adapter.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class JwtToken {
  private String accessToken;
  private String tokenType;
  private String expiresIn;
  private String employeeType;
}
