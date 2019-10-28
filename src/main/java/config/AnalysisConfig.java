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

    private AnalysisConfig() {
    }

    public static Map<String, String> analysis(String configFile) {
        Map<String, String> configMap = null;
        File file = new File(configFile);
        if (file.exists() && !(file.length() > 1024)) {
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
                        configMap.put(args[0], args[1]);
                    }
                } while (StringEnvoy.isNotEmpty(line));
            }
        }
        return configMap;
    }
}
