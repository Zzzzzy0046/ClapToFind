package com.claptofind.phone.data

/**
 * Represents the available sound effects for phone-finding alerts.
 * Premium sounds require subscription (gated in UI).
 */
enum class SoundEffect(
    val displayName: String,
    val rawResId: Int = -1,
    val isPremium: Boolean = false
) {
    AIR_HORN("Air Horn", isPremium = false),
    SIREN("Siren", isPremium = false),
    POLICE_SIREN("Police Siren", isPremium = false),
    GUITAR_STRUM("Guitar Strum", isPremium = true),
    DUCK_QUACK("Duck Quack", isPremium = false),
    PIANO_TONE("Piano Tone", isPremium = true),
    COW_MOO("Cow Moo", isPremium = false),
    CARTOON_BOOM("Cartoon Boom", isPremium = false),
    BIRD_CHIRP("Bird Chirp", isPremium = false),
    DRUM_BEAT("Drum Beat", isPremium = true),
    CAT_MEOW("Cat Meow", isPremium = false),
    DOORBELL_CHIME("Doorbell Chime", isPremium = false),
    WOOD_BLOCK("Wood Block", isPremium = true),
    HORSE_NEIGH("Horse Neigh", isPremium = false),
    GAME_COIN("Game Coin / Pop", isPremium = true),
    DOG_BARK("Dog Bark", isPremium = false),
    ELEPHANT("Elephant", isPremium = false),
    VIOLIN_SNIP("Violin Snip", isPremium = true),
    RADAR_PING("Radar Ping", isPremium = false),
    METRONOME_TICK("Metronome Tick", isPremium = false),
    HI_HAT("Hi-hat", isPremium = false),
    WHITE_NOISE("White Noise", isPremium = false),
    ROOSTER_CROW("Rooster Crow", isPremium = false),
    CRICKET_CHIRP("Cricket Chirp", isPremium = false);

    companion object {
        fun fromDisplayName(name: String): SoundEffect =
            entries.find { it.displayName == name } ?: AIR_HORN

        val freeSounds = entries.filter { !it.isPremium }
        val premiumSounds = entries.filter { it.isPremium }
    }
}

enum class FlashlightMode(val displayName: String, val isPremium: Boolean = false) {
    QUICK_BLINK("Quick Blink"),
    MEDIUM_BLINK("Medium Blink"),
    SLOW_BLINK("Slow Blink"),
    SOS_BLINK("SOS Blink", isPremium = true),
    CONTINUOUS_BLINK("Continuous Blink", isPremium = true),
    RANDOM_BLINK("Random Blink", isPremium = true);

    companion object {
        fun fromDisplayName(name: String): FlashlightMode =
            entries.find { it.displayName == name } ?: QUICK_BLINK
    }
}

enum class VibrateMode(val displayName: String, val isPremium: Boolean = false) {
    HIGH_FREQUENCY("High Frequency"),
    MEDIUM_FREQUENCY("Medium Frequency"),
    SLOW_FREQUENCY("Slow Frequency"),
    SOS_PATTERN("SOS Pattern", isPremium = true),
    RHYTHMIC_PATTERN("Rhythmic Pattern", isPremium = true),
    RANDOM_PATTERN("Random Pattern", isPremium = true);

    companion object {
        fun fromDisplayName(name: String): VibrateMode =
            entries.find { it.displayName == name } ?: HIGH_FREQUENCY
    }
}

enum class SoundSensitivity(val displayName: String, val dbThreshold: Int) {
    VERY_HIGH("Very High", 45),
    HIGH("High", 55),
    MEDIUM("Medium", 65);

    companion object {
        fun fromDisplayName(name: String): SoundSensitivity =
            entries.find { it.displayName == name } ?: HIGH
    }
}

enum class SoundDuration(val displayName: String, val seconds: Int) {
    FIFTEEN_SEC("15s", 15),
    THIRTY_SEC("30s", 30),
    ONE_MIN("1min", 60),
    THREE_MIN("3mins", 180);
}

enum class SupportedLanguage(val displayName: String, val code: String) {
    SYSTEM("System", ""),
    ENGLISH("English", "en"),
    ARABIC("العربية", "ar"),
    CHINESE_SIMPLIFIED("简体中文", "zh-CN"),
    CHINESE_TRADITIONAL("繁體中文", "zh-TW"),
    FRENCH("Français", "fr"),
    GERMAN("Deutsch", "de"),
    ITALIAN("Italiano", "it"),
    INDONESIAN("Indonesia", "id"),
    JAPANESE("日本語", "ja"),
    KOREAN("한국어", "ko"),
    PORTUGUESE("Português", "pt"),
    RUSSIAN("русские", "ru"),
    SPANISH("Español", "es"),
    THAI("ภาษาไทย", "th"),
    TURKISH("Türkçe", "tr");

    companion object {
        fun fromDisplayName(name: String): SupportedLanguage =
            entries.find { it.displayName == name } ?: SYSTEM
    }
}
