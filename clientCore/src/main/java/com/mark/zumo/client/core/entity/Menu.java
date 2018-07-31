/*
 * Copyright (c) 2018. Mark Soft - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package com.mark.zumo.client.core.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.mark.zumo.client.core.entity.util.EntityHelper;

import java.io.Serializable;

/**
 * Created by mark on 18. 4. 30.
 */

@Entity(tableName = Menu.MENU_TABLE)
public class Menu implements Serializable {
    public static final String MENU_TABLE = "menu";

    @PrimaryKey @NonNull @SerializedName(Schema.uuid) @ColumnInfo(name = Schema.uuid)
    public String uuid;
    @SerializedName(Schema.name) @ColumnInfo(name = Schema.name)
    public String name;
    @SerializedName(Schema.categoryName) @ColumnInfo(name = Schema.categoryName)
    public String categoryName;
    @SerializedName(Schema.storeUuid) @ColumnInfo(name = Schema.storeUuid)
    public String storeUuid;
    @SerializedName(Schema.price) @ColumnInfo(name = Schema.price)
    public int price;
    @SerializedName(Schema.imageUrl) @ColumnInfo(name = Schema.imageUrl)
    public String imageUrl;

    public Menu(@NonNull final String uuid, final String name, final String categoryName,
                final String storeUuid, final int price, final String imageUrl) {

        this.uuid = uuid;
        this.name = name;
        this.categoryName = categoryName;
        this.storeUuid = storeUuid;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return EntityHelper.toString(this, this.getClass());
    }

    public interface Schema {
        String uuid = "menu_uuid";
        String name = "menu_name";
        String storeUuid = "store_uuid";
        String price = "menu_price";
        String imageUrl = "image_url";
        String categoryName = "menu_category_name";
    }
}
