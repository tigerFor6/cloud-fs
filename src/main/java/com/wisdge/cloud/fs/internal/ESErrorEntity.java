package com.wisdge.cloud.fs.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESErrorEntity {
    private ESError error;
    private int status;
}



