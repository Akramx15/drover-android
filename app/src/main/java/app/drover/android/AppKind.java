package app.drover.android;

enum AppKind {
    DISCORD("discord", "discord", R.string.app_kind_discord),
    WHATSAPP("whatsapp", "none", R.string.app_kind_whatsapp);

    final String preferenceValue;
    final String nativeStrategy;
    final int displayNameResource;

    AppKind(String preferenceValue, String nativeStrategy, int displayNameResource) {
        this.preferenceValue = preferenceValue;
        this.nativeStrategy = nativeStrategy;
        this.displayNameResource = displayNameResource;
    }

    static AppKind fromPreference(String value) {
        for (AppKind kind : values()) {
            if (kind.preferenceValue.equals(value)) {
                return kind;
            }
        }
        return DISCORD;
    }
}
