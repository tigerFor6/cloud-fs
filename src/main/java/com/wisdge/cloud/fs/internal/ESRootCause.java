package com.wisdge.cloud.fs.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESRootCause {
    private String reason;
    private String type;
    private int line;
    private int col;
}
