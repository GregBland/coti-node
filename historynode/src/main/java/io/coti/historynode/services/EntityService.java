package io.coti.historynode.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.coti.basenode.data.HistoryNodeConsensusResult;
import io.coti.basenode.data.interfaces.IEntity;
import io.coti.basenode.http.AddEntitiesBulkRequest;
import io.coti.historynode.http.storageConnector.interaces.IStorageConnector;
import io.coti.historynode.services.interfaces.IEntityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public abstract class EntityService implements IEntityService {

    @Autowired
    protected IStorageConnector storageConnector;

    protected String endpoint = null;

    @Value("${storage.server.address}")
    protected String storageServerAddress;
    protected ObjectMapper mapper;

    public void storeEntities(List<? extends IEntity> entities, HistoryNodeConsensusResult historyNodeConsensusResult) {


        AddEntitiesBulkRequest addEntitiesBulkRequest = new AddEntitiesBulkRequest(historyNodeConsensusResult);
        entities.forEach(entity ->
                {
                    try {
                        addEntitiesBulkRequest.getHashToEntityJsonDataMap().put(entity.getHash(), mapper.writeValueAsString(entity));
                    } catch (JsonProcessingException e) {
                        log.error(e.getMessage());
                    }
                }
        );
        storageConnector.putObject(storageServerAddress + endpoint, addEntitiesBulkRequest);
    }
}