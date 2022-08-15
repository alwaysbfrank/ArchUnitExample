package org.example.service.creation.internal;

import lombok.RequiredArgsConstructor;
import org.example.service.api.Service;
import org.example.service.creation.api.ServiceCreator;
import org.example.validation.api.IdValidator;

@RequiredArgsConstructor
public class ServiceCreatorImpl implements ServiceCreator {

    private final IdValidator idValidator;

    @Override
    public void create(Service service) {
        idValidator.validate(service.getId());
    }
}
