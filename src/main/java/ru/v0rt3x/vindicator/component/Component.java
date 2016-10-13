package ru.v0rt3x.vindicator.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.v0rt3x.vindicator.VindicatorCore;

public interface Component {

    VindicatorCore core = VindicatorCore.getInstance();
    Logger logger = LoggerFactory.getLogger(Component.class);

    void onInit();
    void onShutDown();

}
