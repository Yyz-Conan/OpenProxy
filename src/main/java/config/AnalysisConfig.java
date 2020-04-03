package config;

import util.StringEnvoy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 解析配置文件
 */
public class AnalysisConfig {

    private Map<String, String> configMap = null;

    private static AnalysisConfig config;

    private AnalysisConfig() {
    }

    public static AnalysisConfig getInstance() {
        if (config == null) {
            config = new AnalysisConfig();
        }
        return config;
    }

    public void analysis(String configFile) {
        if (StringEnvoy.isEmpty(configFile)) {
            return;
        }
        File file = new File(configFile);
        if (!file.exists() || file.length() > 1024) {
            return;
        }
        configMap = new HashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (reader != null) {
            String line = null;
            do {
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (StringEnvoy.isNotEmpty(line) && !line.startsWith("#") && line.contains("=")) {
                    String[] args = line.split("=");
                    if (args.length == 2) {
                        configMap.put(args[0], args[1]);
                    }
                }
            } while (StringEnvoy.isNotEmpty(line));
        }
    }

    public String getValue(String key) {
        if (configMap != null) {
            return configMap.get(key);
        }
        return null;
    }

    public int getIntValue(String key) {
        if (configMap != null) {
            String value = configMap.get(key);
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public boolean getBooleanValue(String key) {
        if (configMap != null) {
            String value = configMap.get(key);
            return Boolean.parseBoolean(value);
        }
        return false;
    }
}
