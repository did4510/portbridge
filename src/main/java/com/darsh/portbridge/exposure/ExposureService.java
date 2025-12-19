package com.darsh.portbridge.exposure;

import org.slf4j.Logger;
import com.darsh.portbridge.PortBridge;
import java.util.UUID;

public abstract class ExposureService {
    protected static final Logger LOGGER = PortBridge.LOGGER;

    protected String lastError;
    protected boolean active;
    protected String publicAddress;
    protected String exposureMethod;

    public ExposureService(String method) {
        this.exposureMethod = method;
        this.active = false;
        this.lastError = null;
    }

    public abstract boolean start(int internalPort, int externalPort);

    public abstract void stop();

    public boolean isActive() {
        return active;
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public String getLastError() {
        return lastError;
    }

    public String getExposureMethod() {
        return exposureMethod;
    }

    protected void setPublicAddress(String address) {
        this.publicAddress = address;
    }

    protected void setError(String error) {
        this.lastError = error;
    }
}
