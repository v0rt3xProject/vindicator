package ru.v0rt3x.vindicator.service;

public interface Service extends Runnable {

    void onInit();
    void onShutDown();
}
