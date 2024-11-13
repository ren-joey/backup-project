package com.delta.dms.community.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrcSyncSignIn {
    private DrcSyncUser user;
    private DrcSyncTokens tokens;
}
