package com.wisdge.cloud.fs.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ESError {
    private boolean grouped;
    private String phase;
    private String reason;
    private int line;
    private int col;
    private List<Object> failed_shards;
    private String type;
    private List<ESRootCause> root_cause;
}
