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

import jakarta.inject.Inject;
import org.apache.camel.demo.model.Booking;
import org.apache.camel.demo.model.Product;
import org.apache.camel.demo.model.Supply;
import org.apache.camel.demo.model.event.BookingCompletedEvent;
import org.apache.camel.demo.model.event.ShippingEvent;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.kafka.endpoint.KafkaEndpoint;
import org.citrusframework.quarkus.CitrusSupport;
import org.junit.jupiter.api.Test;

import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.actions.SleepAction.Builder.delay;
import static org.citrusframework.container.Iterate.Builder.iterate;
import static org.citrusframework.container.Parallel.Builder.parallel;
import static org.citrusframework.dsl.JsonSupport.marshal;
import static org.citrusframework.kafka.endpoint.builder.KafkaEndpoints.kafka;

@QuarkusTest
@CitrusSupport
class FoodMarketApplicationTest {

    private final KafkaEndpoint products = kafka()
            .asynchronous()
            .topic("products")
            .build();

    private final KafkaEndpoint bookings = kafka()
            .asynchronous()
            .topic("bookings")
            .build();

    private final KafkaEndpoint supplies = kafka()
            .asynchronous()
            .topic("supplies")
            .build();

    @CitrusResource
    private TestCaseRunner t;

    @Inject
    ObjectMapper mapper;

    @Test
    void shouldCompleteOnSupply() {
        Product product = new Product("Watermelon");
        t.when(send()
            .endpoint(products)
            .message().body(marshal(product, mapper)));

        t.then(delay().seconds(1L));

        Booking booking = new Booking("citrus-test", product, 100, 0.99D);
        t.when(send()
                .endpoint(bookings)
                .message().body(marshal(booking, mapper)));

        Supply supply = new Supply(product, 100, 0.99D);
        t.when(send()
                .endpoint(supplies)
                .message().body(marshal(supply, mapper)));

        BookingCompletedEvent completedEvent = BookingCompletedEvent.from(booking);
        completedEvent.setStatus(Booking.Status.COMPLETED.name());

        ShippingEvent shippingEvent = new ShippingEvent(booking.getClient(), product.getName(), supply.getAmount(), "@ignore@");
        t.then(parallel().actions(
            receive()
                .endpoint("kafka:completed?timeout=10000")
                .message().body(marshal(completedEvent, mapper)),
            receive()
                .endpoint("kafka:shipping?timeout=10000")
                .message().body(marshal(shippingEvent, mapper))
        ));
    }

    @Test
    void shouldCompleteOnBooking() {
        Product product = new Product("Pineapple");

        Supply supply = new Supply(product, 100, 0.99D);
        t.when(send()
                .endpoint(supplies)
                .message().body(marshal(supply, mapper)));

        Booking booking = new Booking("citrus-test", product, 100, 0.99D);
        t.when(send()
                .endpoint(bookings)
                .message().body(marshal(booking, mapper)));

        BookingCompletedEvent completedEvent = BookingCompletedEvent.from(booking);
        completedEvent.setStatus(Booking.Status.COMPLETED.name());

        ShippingEvent shippingEvent = new ShippingEvent(booking.getClient(), product.getName(), supply.getAmount(), "@ignore@");
        t.then(parallel().actions(
            receive()
                .endpoint("kafka:completed?timeout=10000")
                .message().body(marshal(completedEvent, mapper)),
            receive()
                .endpoint("kafka:shipping?timeout=10000")
                .message().body(marshal(shippingEvent, mapper))
        ));
    }

    @Test
    void shouldCompleteAllMatchingBookings() {
        Product product = new Product("Apple");
        t.variable("product", product);

        Booking booking = new Booking("citrus-test", product, 10, 1.99D);
        t.when(iterate()
            .condition((i, context) -> i < 10)
            .actions(
                send()
                    .endpoint(bookings)
                    .message().body(marshal(booking, mapper))));
        t.variable("booking", booking);

        t.$(delay().milliseconds(1000L));

        Supply supply = new Supply(product, 100, 0.99D);

        BookingCompletedEvent completedEvent = BookingCompletedEvent.from(booking);
        completedEvent.setStatus(Booking.Status.COMPLETED.name());

        ShippingEvent shippingEvent = new ShippingEvent(booking.getClient(), product.getName(), booking.getAmount(), "@ignore@");

        t.then(parallel().actions(
            send()
                .endpoint(supplies)
                .message().body(marshal(supply, mapper)),
            iterate()
                .condition((i, context) -> i < 10)
                .actions(
                    receive()
                        .endpoint("kafka:completed?timeout=10000")
                        .message().body(marshal(completedEvent, mapper))
                ),
            iterate()
                .condition((i, context) -> i < 10)
                .actions(
                    receive()
                        .endpoint("kafka:shipping?timeout=10000")
                        .message().body(marshal(shippingEvent, mapper))
                )
        ));
    }
}
