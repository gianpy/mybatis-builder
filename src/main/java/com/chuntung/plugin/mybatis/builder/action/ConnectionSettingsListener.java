package com.chuntung.plugin.mybatis.builder.action;

import com.intellij.util.messages.Topic;

import java.util.EventListener;

public interface ConnectionSettingsListener extends EventListener {
    Topic<ConnectionSettingsListener> TOPIC = Topic.create("MyBatisBuilder.ConnectionSettingsListener", ConnectionSettingsListener.class);

    void settingsChanged();
}
