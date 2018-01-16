package com.github.dotkebi.video.property;

import org.springframework.stereotype.Component;

@Component
public class YoutubeProperty {

    private String user;

    public YoutubeProperty() {
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
