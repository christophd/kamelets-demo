/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// camel-k: trait=mount.configs=secret:kafka-credentials

import org.apache.camel.builder.RouteBuilder;

public class TimerSourceSupplyRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        String supply = "{ \"product\": \"Apple\", \"amount\": 100, \"price\": 1.99 }";

        from("kamelet:timer-source?period=2000&message=" + supply)
            .to("log:info")
            .to("kamelet:kafka-sink?topic=supplies");
    }
}
