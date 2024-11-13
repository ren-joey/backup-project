package com.delta.dms.community.dao.entity;

import lombok.Data;

@Data(staticConstructor="of")
public class ReplyFloor {
    private final int floor;
    private final int subFloor;
}
