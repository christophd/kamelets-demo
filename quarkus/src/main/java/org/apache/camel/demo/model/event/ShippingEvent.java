package org.apache.camel.demo.model.event;

import com.github.javafaker.Faker;

public class ShippingEvent {

    private String client;
    private String product;
    private Integer amount;

    private String address;

    private static final Faker faker = new Faker();

    public ShippingEvent() {
    }

    public ShippingEvent(String client, String product, Integer amount) {
        this(client, product, amount, String.format("%s, %s", faker.name().fullName(), faker.address().streetAddress()));
    }

    public ShippingEvent(String client, String product, Integer amount, String address) {
        this.client = client;
        this.product = product;
        this.amount = amount;
        this.address = address;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
