package com.example;
import java.util.List;

public class Order {
    public String id;
    public int customerID;
    public List<String> items;

    public Order(String id, int customerID, List<String> items){
        this.id = id;
        this.customerID = customerID;
        this.items = items;
    }

    @Override
    public String toString(){
        return "[Order ID: " + id + ", Customer: " + customerID + ", Items: " + items + "]";
    }
}
