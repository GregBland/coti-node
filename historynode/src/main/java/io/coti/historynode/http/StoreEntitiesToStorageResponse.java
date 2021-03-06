package io.coti.historynode.http;

import io.coti.basenode.data.Hash;
import io.coti.basenode.http.Response;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class StoreEntitiesToStorageResponse extends Response {

    private Map<Hash, Boolean> entitiesSentToStorage;

    public StoreEntitiesToStorageResponse(Map<Hash, Boolean> entitiesSentToStorage) {
        this.entitiesSentToStorage = entitiesSentToStorage;
    }

    public StoreEntitiesToStorageResponse() {
        this.entitiesSentToStorage = new HashMap<>();
    }
}
