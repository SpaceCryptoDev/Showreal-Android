package com.showreal.app.data.model;

import java.util.List;

import nl.qbusict.cupboard.annotation.Ignore;

public class DeviceReel {

    public Long _id;
    @Ignore public List<Video> videos;
    public boolean dummyReel;
}
