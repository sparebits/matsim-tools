/*
 * Storage
 * @author : neiko.neikov
 * @created : 3.03.25 г., Monday
 */
package com.sparebits.matsim;

import org.matsim.api.core.v01.network.Network;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class Storage {

    private Map<UUID, Network> networks = new ConcurrentHashMap<>();

    public Map<UUID, Network> getNetworks() {
        return networks;
    }

    public void setNetworks(Map<UUID, Network> networks) {
        this.networks = networks;
    }
}
