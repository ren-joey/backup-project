package com.delta.dms.community.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DrcSyncUser {
    private String id;
    private String username;
    private String email;
    private String role;
    private int level;
}
