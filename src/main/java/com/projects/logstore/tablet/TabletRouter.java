package com.projects.logstore.tablet;

import io.swagger.v3.oas.models.security.SecurityScheme;

public class TabletRouter {
    private int totalTablets;

    public TabletRouter(int totalTablets) {
        this.totalTablets= totalTablets;
    }

    public int route(String key){
        if (key == null || key.isEmpty()) {
            key = Integer.toString(Integer.MIN_VALUE);
        }
        return Math.abs(key.hashCode() % this.totalTablets);
    }
}
