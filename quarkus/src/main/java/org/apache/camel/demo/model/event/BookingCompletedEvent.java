package org.apache.camel.demo.model.event;

import org.apache.camel.demo.model.Booking;

public class BookingCompletedEvent {

    private String client;
    private String product;
    private Integer amount;
    private String status;

    public static BookingCompletedEvent from(Booking booking) {
        BookingCompletedEvent event = new BookingCompletedEvent();
        event.setClient(booking.getClient());
        event.setProduct(booking.getProduct().getName());
        event.setAmount(booking.getAmount());
        event.setStatus(booking.getStatus().name());
        return event;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
