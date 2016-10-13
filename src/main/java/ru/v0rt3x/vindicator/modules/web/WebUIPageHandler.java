package ru.v0rt3x.vindicator.modules.web;

import org.apache.velocity.VelocityContext;

public interface WebUIPageHandler {

    void prepareContext(VelocityContext ctx);

}
