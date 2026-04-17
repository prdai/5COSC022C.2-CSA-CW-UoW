package com.w2120198.csa.cw;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS application entry point. The {@link ApplicationPath} value sets
 * the versioned base path, so every resource resolves under
 * {@code /<context-root>/api/v1}. Resource and provider classes are
 * discovered by Jersey's classpath scanning of the
 * {@code com.w2120198.csa.cw} package tree.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
}
