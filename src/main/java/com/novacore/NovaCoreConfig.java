package com.novacore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * NovaCore 双预设配置系统 — v1.2.1
 * 纯配置，不做任何 Minecraft 字段访问
 */
public class NovaCoreConfig {

    public enum Preset { STANDARD, EXTREME, CUSTOM }

    public static Preset preset = Preset.STANDARD;

    public static boolean enabled = true;
    public static boolean lightingEnabled = true;
    public static boolean chunkLoadingEnabled = true;
    public static boolean memoryFixEnabled = true;
    public static boolean entityCullingEnabled = true;
    public static boolean mathLookupTable = true;
    public static boolean openglEnabled = true;

    public static int entityRenderDistDefault = 64;
    public static int chunkIOThreads = 4;
    public static int chunkPreloadRadius = 5;
    public static int glBatchSize = 256;
    public static int sinTableSize = 65536;

    // EXTREME
    public static boolean tickRateEnabled = false;
    public static int entityTickIntervalNear = 1;
    public static int entityTickIntervalMid = 2;
    public static int entityTickIntervalFar = 4;
    public static boolean skipRemoteRandomTicks = false;
    public static int tileEntityTickInterval = 4;

    public static boolean particleLimiterEnabled = false;
    public static int particleGlobalCap = 200;
    public static int particleDistanceCutoff = 16;
    public static boolean reduceParticleLifetime = false;
    public static boolean disableRainParticles = false;

    public static boolean renderAggressionEnabled = false;
    public static boolean disableEntityShadows = false;
    public static boolean disableClouds = false;
    public static boolean disableWeather = false;
    public static boolean reduceChunkUpdates = false;
    public static double entityRenderDistanceRatio = 0.75;

    private static void applyPreset(Preset p) {
        switch (p) {
            case STANDARD:
                tickRateEnabled = false;
                particleLimiterEnabled = false;
                renderAggressionEnabled = false;
                entityCullingEnabled = true;
                lightingEnabled = true;
                chunkLoadingEnabled = true;
                memoryFixEnabled = true;
                mathLookupTable = true;
                openglEnabled = true;
                entityRenderDistDefault = 64;
                chunkPreloadRadius = 5;
                chunkIOThreads = 4;
                glBatchSize = 256;
                break;
            case EXTREME:
                entityCullingEnabled = true;
                lightingEnabled = true;
                chunkLoadingEnabled = true;
                memoryFixEnabled = true;
                mathLookupTable = true;
                openglEnabled = true;
                entityRenderDistDefault = 48;
                chunkPreloadRadius = 3;
                chunkIOThreads = 8;
                glBatchSize = 512;
                tickRateEnabled = true;
                entityTickIntervalNear = 1;
                entityTickIntervalMid = 2;
                entityTickIntervalFar = 4;
                skipRemoteRandomTicks = true;
                tileEntityTickInterval = 4;
                particleLimiterEnabled = true;
                particleGlobalCap = 200;
                particleDistanceCutoff = 16;
                reduceParticleLifetime = true;
                disableRainParticles = true;
                renderAggressionEnabled = true;
                disableEntityShadows = true;
                disableClouds = true;
                disableWeather = true;
                reduceChunkUpdates = true;
                break;
            case CUSTOM:
                break;
        }
    }

    public static void setPreset(Preset p) {
        preset = p;
        if (p != Preset.CUSTOM) applyPreset(p);
    }

    private static File getConfigFile() {
        return new File("config/novacore.properties");
    }

    public static void loadConfig() {
        File f = getConfigFile();
        if (!f.exists()) { applyPreset(Preset.STANDARD); saveConfig(); return; }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(f)) { props.load(in); }
        catch (IOException e) { applyPreset(Preset.STANDARD); return; }
        try { preset = Preset.valueOf(props.getProperty("preset", "STANDARD")); }
        catch (IllegalArgumentException e) { preset = Preset.STANDARD; }
        if (preset != Preset.CUSTOM) { applyPreset(preset); }
        else {
            enabled = getBool(props, "enabled", true);
            entityCullingEnabled = getBool(props, "entityCullingEnabled", true);
            lightingEnabled = getBool(props, "lightingEnabled", true);
            chunkLoadingEnabled = getBool(props, "chunkLoadingEnabled", true);
            memoryFixEnabled = getBool(props, "memoryFixEnabled", true);
            mathLookupTable = getBool(props, "mathLookupTable", true);
            openglEnabled = getBool(props, "openglEnabled", true);
            tickRateEnabled = getBool(props, "tickRateEnabled", false);
            particleLimiterEnabled = getBool(props, "particleLimiterEnabled", false);
            renderAggressionEnabled = getBool(props, "renderAggressionEnabled", false);
            entityRenderDistDefault = getInt(props, "entityRenderDistDefault", 64);
            chunkPreloadRadius = getInt(props, "chunkPreloadRadius", 5);
            chunkIOThreads = getInt(props, "chunkIOThreads", 4);
            glBatchSize = getInt(props, "glBatchSize", 256);
            particleGlobalCap = getInt(props, "particleGlobalCap", 200);
            entityTickIntervalFar = getInt(props, "entityTickIntervalFar", 4);
            tileEntityTickInterval = getInt(props, "tileEntityTickInterval", 4);
            skipRemoteRandomTicks = getBool(props, "skipRemoteRandomTicks", false);
            disableEntityShadows = getBool(props, "disableEntityShadows", false);
            disableClouds = getBool(props, "disableClouds", false);
            disableWeather = getBool(props, "disableWeather", false);
            reduceChunkUpdates = getBool(props, "reduceChunkUpdates", false);
            reduceParticleLifetime = getBool(props, "reduceParticleLifetime", false);
            disableRainParticles = getBool(props, "disableRainParticles", false);
        }
    }

    public static void saveConfig() {
        Properties props = new Properties();
        props.setProperty("preset", preset.name());
        props.setProperty("enabled", String.valueOf(enabled));
        props.setProperty("entityCullingEnabled", String.valueOf(entityCullingEnabled));
        props.setProperty("lightingEnabled", String.valueOf(lightingEnabled));
        props.setProperty("chunkLoadingEnabled", String.valueOf(chunkLoadingEnabled));
        props.setProperty("memoryFixEnabled", String.valueOf(memoryFixEnabled));
        props.setProperty("mathLookupTable", String.valueOf(mathLookupTable));
        props.setProperty("openglEnabled", String.valueOf(openglEnabled));
        props.setProperty("tickRateEnabled", String.valueOf(tickRateEnabled));
        props.setProperty("particleLimiterEnabled", String.valueOf(particleLimiterEnabled));
        props.setProperty("renderAggressionEnabled", String.valueOf(renderAggressionEnabled));
        props.setProperty("entityRenderDistDefault", String.valueOf(entityRenderDistDefault));
        props.setProperty("chunkPreloadRadius", String.valueOf(chunkPreloadRadius));
        props.setProperty("chunkIOThreads", String.valueOf(chunkIOThreads));
        props.setProperty("glBatchSize", String.valueOf(glBatchSize));
        props.setProperty("particleGlobalCap", String.valueOf(particleGlobalCap));
        props.setProperty("entityTickIntervalFar", String.valueOf(entityTickIntervalFar));
        props.setProperty("tileEntityTickInterval", String.valueOf(tileEntityTickInterval));
        props.setProperty("skipRemoteRandomTicks", String.valueOf(skipRemoteRandomTicks));
        props.setProperty("disableEntityShadows", String.valueOf(disableEntityShadows));
        props.setProperty("disableClouds", String.valueOf(disableClouds));
        props.setProperty("disableWeather", String.valueOf(disableWeather));
        props.setProperty("reduceChunkUpdates", String.valueOf(reduceChunkUpdates));
        props.setProperty("reduceParticleLifetime", String.valueOf(reduceParticleLifetime));
        props.setProperty("disableRainParticles", String.valueOf(disableRainParticles));
        File f = getConfigFile();
        f.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) { props.store(out, "NovaCore Configuration"); }
        catch (IOException e) { System.err.println("[NovaCore] Failed to save config: " + e); }
    }

    private static boolean getBool(Properties p, String key, boolean def) {
        return Boolean.parseBoolean(p.getProperty(key, String.valueOf(def)));
    }
    private static int getInt(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
}