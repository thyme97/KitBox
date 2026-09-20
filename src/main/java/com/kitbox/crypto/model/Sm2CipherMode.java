package com.kitbox.crypto.model;

/** SM2 密文排列方式。 */
public enum Sm2CipherMode {

    C1C3C2("C1C3C2（新标准）", SM2EngineMode.C1C3C2),
    C1C2C3("C1C2C3（旧标准）", SM2EngineMode.C1C2C3);

    private final String display;
    private final SM2EngineMode mode;

    Sm2CipherMode(String display, SM2EngineMode mode) {
        this.display = display;
        this.mode = mode;
    }

    public String getDisplay() {
        return display;
    }

    public SM2EngineMode engineMode() {
        return mode;
    }

    @Override
    public String toString() {
        return display;
    }

    /** 内部隔离 BC 的 Mode 枚举，避免模型层直接依赖实现细节。 */
    public enum SM2EngineMode {
        C1C3C2, C1C2C3
    }
}
