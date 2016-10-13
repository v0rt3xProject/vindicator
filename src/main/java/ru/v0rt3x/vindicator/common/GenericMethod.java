package ru.v0rt3x.vindicator.common;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface GenericMethod {
    String value();
}
