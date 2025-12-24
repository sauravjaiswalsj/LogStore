package com.projects.logstore.server;

import com.projects.logstore.dto.AppendDTO;
import com.projects.logstore.model.Data;
import com.projects.logstore.tablet.RegistryTablet;
import com.projects.logstore.tablet.Tablet;
import com.projects.logstore.tablet.TabletRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TabletServer {
    @Autowired
    private TabletRouter tabletRouter;
    @Autowired
    private RegistryTablet registry;

    public AppendDTO append(Data data){
        int tabletId = getTabletRoute(data.getKey());
        Tablet tablet = getTablet(tabletId);
        long offSet = tablet.append(data.getKey(), data.getValue());
        AppendDTO appendDTO = new AppendDTO();
        appendDTO.setTabletId(tabletId);
        appendDTO.setOffset(offSet);
        return appendDTO;
    }

    private Tablet getTablet(int tabletId){
        return registry.getTabletById(tabletId);
    }

    private int getTabletRoute(String key){
        return tabletRouter.route(key);
    }
}
