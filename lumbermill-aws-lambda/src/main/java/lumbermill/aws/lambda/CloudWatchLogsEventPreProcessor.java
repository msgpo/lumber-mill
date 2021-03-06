/*
 * Copyright 2016 Sony Mobile Communications, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package lumbermill.aws.lambda;

import lumbermill.api.Codecs;
import lumbermill.api.EventProcessor;
import lumbermill.api.JsonEvent;
import rx.Observable;

import static lumbermill.Core.*;


/**
 * Reusable EventProcessor for decoding the contents an event received
 * from Cloud Watch Logs.
 */
@SuppressWarnings("unused")
class CloudWatchLogsEventPreProcessor implements EventProcessor<JsonEvent, JsonEvent> {

    public Observable<JsonEvent> call(Observable<JsonEvent> observable) {
        // Read the actual data from json
        return observable
                .map ( jsonEvent -> Codecs.BYTES.from (jsonEvent.objectChild("awslogs").valueAsString("data")))
                .flatMap ( base64.decode())
                .flatMap ( gzip.decompress())
                .flatMap ( toJsonObject())
                .flatMap ( event -> {

                    // Denormalize, add logGroup and logStream to each event
                    String logGroup = event.valueAsString("logGroup");
                    String logStream = event.valueAsString("logStream");
                    return event.child("logEvents")
                            .each()
                            .map (jsonEvent->
                                    jsonEvent.put("logGroup", logGroup)
                                            .put("logStream", logStream));
                }
        )
        .flatMap ( timestampFromMs("timestamp"))
        .flatMap ( remove("timestamp"));
    }
}