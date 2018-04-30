package com.mark.zumo.client.core.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.mark.zumo.client.core.util.EntityHelper;

import java.util.List;

/**
 * Created by mark on 18. 4. 30.
 */

@Entity
public class Order {

    @PrimaryKey public final long id;
    public final long customerUserSessionId;
    public final long storeSessionId;
    public final List<Long> menuItemIds;
    public final long createdDate;
    public final int totalPrice;

    public Order(long id, long customerUserSessionId, long storeSessionId, List<Long> menuItemIds, long createdDate, int totalPrice) {
        this.id = id;
        this.customerUserSessionId = customerUserSessionId;
        this.storeSessionId = storeSessionId;
        this.menuItemIds = menuItemIds;
        this.createdDate = createdDate;
        this.totalPrice = totalPrice;
    }

    @Override
    public String toString() {
        return EntityHelper.toString(this, this.getClass());
    }
}
