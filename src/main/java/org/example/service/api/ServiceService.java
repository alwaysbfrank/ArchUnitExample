package org.example.service.api;

public interface ServiceService {
    Service get(String id);

    void create(Service service);
}
