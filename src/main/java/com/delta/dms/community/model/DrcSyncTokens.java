package com.delta.dms.community.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrcSyncTokens {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
}
