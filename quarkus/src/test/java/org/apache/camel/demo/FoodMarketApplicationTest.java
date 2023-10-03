/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
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

package org.apache.camel.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;

import org.apache.camel.demo.model.Booking;
import org.apache.camel.demo.model.Product;
import org.apache.camel.demo.model.Supply;
import org.apache.camel.demo.model.event.BookingCompletedEvent;
import org.apache.camel.demo.model.event.ShippingEvent;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.kafka.endpoint.KafkaEndpoint;
import org.citrusframework.message.builder.ObjectMappingPayloadBuilder;
import org.citrusframework.quarkus.CitrusSupport;
import org.citrusframework.spi.BindToRegistry;
import org.junit.jupiter.api.Test;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.actions.SleepAction.Builder.delay;
import static org.citrusframework.container.Iterate.Builder.iterate;
import static org.citrusframework.container.Parallel.Builder.parallel;
import static org.citrusframework.kafka.endpoint.builder.KafkaEndpoints.kafka;

@QuarkusTest
@CitrusSupport
class FoodMarketApplicationTest {

    @BindToRegistry
    private final KafkaEndpoint products = kafka()
            .asynchronous()
            .topic("products")
            .build();

    @BindToRegistry
    private final KafkaEndpoint bookings = kafka()
            .asynchronous()
            .topic("bookings")
            .build();

    @BindToRegistry
    private final KafkaEndpoint supplies = kafka()
            .asynchronous()
            .topic("supplies")
            .build();;

    @CitrusResource
    private TestCaseRunner t;

    @BindToRegistry
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldCompleteOnSupply() {
        Product product = new Product("Watermelon");
        t.when(send()
            .endpoint(products)
            .message().body(new ObjectMappingPayloadBuilder(product)));

        t.then(delay().seconds(1L));

        Booking booking = new Booking("christoph", product, 100, 0.99D);
        t.when(send()
                .endpoint(bookings)
                .message().body(new ObjectMappingPayloadBuilder(booking)));

        Supply supply = new Supply(product, 100, 0.99D);
        t.when(send()
                .endpoint(supplies)
                .message().body(new ObjectMappingPayloadBuilder(supply)));

        BookingCompletedEvent completedEvent = BookingCompletedEvent.from(booking);
        completedEvent.setStatus(Booking.Status.COMPLETED.name());
        t.then(parallel().actions(
            receive()
                .endpoint("kafka:completed?timeout=10000")
                .message().body(new ObjectMappingPayloadBuilder(completedEvent)),
            receive()
                .endpoint("kafka:shipping?timeout=10000")
                .message().body(new ObjectMappingPayloadBuilder(new ShippingEvent(booking.getClient(), product.getName(), supply.getAmount(), "@ignore@")))
        ));
    }

    @Test
    void shouldCompleteOnBooking() {
        Product product = new Product("Pineapple");

        Supply supply = new Supply(product, 100, 0.99D);
        t.when(send()
                .endpoint(supplies)
                .message().body(new ObjectMappingPayloadBuilder(supply)));

        Booking booking = new Booking("christoph", product, 100, 0.99D);
        t.when(send()
                .endpoint(bookings)
                .message().body(new ObjectMappingPayloadBuilder(booking)));

        BookingCompletedEvent completedEvent = BookingCompletedEvent.from(booking);
        completedEvent.setStatus(Booking.Status.COMPLETED.name());
        t.then(parallel().actions(
            receive()
                .endpoint("kafka:completed?timeout=10000")
                .message().body(new ObjectMappingPayloadBuilder(completedEvent)),
            receive()
                .endpoint("kafka:shipping?timeout=10000")
                .message().body(new ObjectMappingPayloadBuilder(new ShippingEvent(booking.getClient(), product.getName(), supply.getAmount(), "@ignore@")))
        ));
    }

    @Test
    void shouldCompleteAllMatchingBookings() {
        Product product = new Product("Apple");
        t.variable("product", product);

        Booking booking = new Booking("christoph", product, 10, 1.99D);
        t.when(iterate()
                .condition((i, context) -> i < 10)
                .actions(
                        send()
                            .endpoint(bookings)
                            .message().body(new ObjectMappingPayloadBuilder(booking))));
        t.variable("booking", booking);

        t.$(delay().milliseconds(1000L));

        Supply supply = new Supply(product, 100, 0.99D);

        BookingCompletedEvent completedEvent = BookingCompletedEvent.from(booking);
        completedEvent.setStatus(Booking.Status.COMPLETED.name());

        t.then(parallel().actions(
            send()
                .endpoint(supplies)
                .message().body(new ObjectMappingPayloadBuilder(supply)),
            iterate()
                .condition((i, context) -> i < 10)
                .actions(
                    receive()
                        .endpoint("kafka:completed?timeout=10000")
                        .message().body(new ObjectMappingPayloadBuilder(completedEvent))
                ),
            iterate()
                .condition((i, context) -> i < 10)
                .actions(
                    receive()
                        .endpoint("kafka:shipping?timeout=10000")
                        .message().body(new ObjectMappingPayloadBuilder(new ShippingEvent(booking.getClient(), product.getName(), booking.getAmount(), "@ignore@")))
                )
        ));
    }
}
