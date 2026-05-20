package de.serav.flowmetrix.ai.splunk;

import com.splunk.logging.EventHeaderSerializer;
import com.splunk.logging.HttpEventCollectorEventInfo;
import com.splunk.logging.hec.MetadataTags;

import java.math.BigDecimal;
import java.util.Map;

public class JsonEventSerializer implements EventHeaderSerializer {

    @Override
    public Map<String, Object> serializeEventHeader(HttpEventCollectorEventInfo eventInfo, final Map<String, Object> metadata) {
        metadata.put(MetadataTags.TIME, BigDecimal.valueOf(eventInfo.getTime()).toString());
        return metadata;
    }

}
