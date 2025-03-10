package com.delta.dms.community.converter;

import java.util.Optional;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

@SuppressWarnings({"rawtypes", "unchecked"})
public class StringToEnumConverter implements ConverterFactory<String, Enum> {

  @Override
  public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
    return new StringToEnum(getEnumType(targetType));
  }

  private class StringToEnum<T extends Enum> implements Converter<String, T> {

    private final Class<T> enumType;

    public StringToEnum(Class<T> enumType) {
      this.enumType = enumType;
    }

    @Override
    public T convert(String source) {
      if (source.isEmpty()) {
        // It's an empty enum identifier: reset the enum value to null.
        return null;
      }

      return (T) Enum.valueOf(this.enumType, source.trim().toUpperCase());
    }
  }

  private static Class<?> getEnumType(Class targetType) {
    Class<?> enumType = targetType;
    while (enumType != null && !enumType.isEnum()) {
      enumType = enumType.getSuperclass();
    }
    if (enumType == null) {
      throw new IllegalArgumentException(
          "The target type "
              + Optional.ofNullable(targetType).map(Class::getName).orElseGet(() -> "")
              + " does not refer to an enum");
    }
    return enumType;
  }
}
