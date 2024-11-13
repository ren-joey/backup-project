package com.delta.dms.community.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class GeneralEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

  private final Class<E> type;

  public GeneralEnumTypeHandler(Class<E> type) {
    if (Objects.isNull(type)) {
      throw new IllegalArgumentException("Type argument cannot be null");
    }
    this.type = type;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
      throws SQLException {
    if (Objects.isNull(jdbcType)) {
      ps.setString(i, parameter.toString());
    } else {
      ps.setObject(i, parameter.toString(), jdbcType.TYPE_CODE);
    }
  }

  @Override
  public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String s = rs.getString(columnName);
    return Objects.isNull(s) ? null : fromValue(type, s);
  }

  @Override
  public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String s = rs.getString(columnIndex);
    return Objects.isNull(s) ? null : fromValue(type, s);
  }

  @Override
  public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String s = cs.getString(columnIndex).toUpperCase();
    return Objects.isNull(s) ? null : fromValue(type, s);
  }

  public static <E extends Enum<?>> E fromValue(Class<E> clazz, String value) {
    return Arrays.stream(clazz.getEnumConstants())
        .filter(e -> e.toString().equalsIgnoreCase(value))
        .findFirst()
        .orElse(null);
  }
}
