package org.example.service.internal;

import lombok.RequiredArgsConstructor;
import org.example.service.api.Service;
import org.example.service.api.ServiceService;
import org.example.service.creation.api.ServiceCreator;

@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceCreator serviceCreator;
    private final ServiceServiceHelper helper;

    @Override
    public Service get(String id) {
        helper.help();
        return null;
    }

    @Override
    public void create(Service service) {
        serviceCreator.create(service);
    }
}
