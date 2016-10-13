package ru.v0rt3x.vindicator.service;

import ru.v0rt3x.vindicator.common.GenericManager;

public class ServiceManager extends GenericManager<Service> {

    @Override
    protected void onDiscover(Class<? extends Service> object) {
        try {
            Service service = object.newInstance();
            objects.put(object, service);
            service.onInit();
            core.executeThread(service);
            logger.info("Service[{}]: Started", object.getSimpleName());
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("Service[{}]: Unable to start service: {}", object.getSimpleName(), e.getMessage());
        }
    }

    @Override
    public void onShutDown() {
        objects.keySet().stream()
            .filter(component -> objects.get(component) != null)
            .forEach(component -> objects.get(component).onShutDown());
    }
}
