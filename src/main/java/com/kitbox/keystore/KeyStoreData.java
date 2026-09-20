package com.kitbox.keystore;

import java.util.ArrayList;
import java.util.List;

/**
 * 密钥库数据（加密前的明文载荷）。
 */
public class KeyStoreData {

    private int version = 1;
    private List<String> scenarios = new ArrayList<>();
    private List<KeyEntry> entries = new ArrayList<>();

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<String> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<String> scenarios) {
        this.scenarios = scenarios;
    }

    public List<KeyEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<KeyEntry> entries) {
        this.entries = entries;
    }
}
