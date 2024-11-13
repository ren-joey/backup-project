package com.delta.dms.community.model;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;

public class CustomByteArrayResource extends ByteArrayResource {

  private final String fileName;

  public CustomByteArrayResource(String fileName, byte[] byteArray) {
    super(byteArray);
    this.fileName = fileName;
  }

  @Override
  public String getFilename() {
    return fileName;
  }

  @Override
  public boolean equals(Object other) {
    return super.equals(other)
        && (other instanceof CustomByteArrayResource
            && StringUtils.equals(((CustomByteArrayResource) other).getFilename(), this.fileName));
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
