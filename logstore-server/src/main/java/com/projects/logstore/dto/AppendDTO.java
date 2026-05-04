package com.projects.logstore.dto;

import lombok.Data;
import lombok.Setter;

@Data
@Setter
public class AppendDTO {
    String stream;
    int tabletId;
    long offset;
}
