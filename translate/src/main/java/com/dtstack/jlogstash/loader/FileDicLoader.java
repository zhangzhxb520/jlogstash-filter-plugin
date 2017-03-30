package com.dtstack.jlogstash.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * 从文件加载字典
 * @author zxb
 * @version 1.0.0
 *          2017年03月25日 23:28
 * @since Jdk1.6
 */
public class FileDicLoader implements DicLoader {

    private static final Logger logger = LoggerFactory.getLogger(FileDicLoader.class);

    private String dicPath;

    public FileDicLoader(String dicPath) {
        this.dicPath = dicPath;
    }

    public Map load() {
        if (dicPath.startsWith("http://") || dicPath.startsWith("https://")) {
            return loadFromRemote();
        } else {
            return loadFromLocal();
        }
    }

    /**
     * 从本地文件系统加载字典
     * @return
     */
    private Map loadFromLocal() {
        Yaml yaml = new Yaml();
        FileInputStream input = null;
        try {
            input = new FileInputStream(new File(dicPath));
            return (HashMap) yaml.load(input);
        } catch (FileNotFoundException e) {
            logger.error(dicPath + " is not found", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("close FileInputStrema error", e);
                }
            }
        }
        return null;
    }

    /**
     * 从远程地址加载字典
     *
     * @return
     */
    private Map loadFromRemote() {
        URL httpUrl;
        Yaml yaml = new Yaml();
        URLConnection connection = null;
        try {
            httpUrl = new URL(dicPath);
            connection = httpUrl.openConnection();
            connection.connect();
            return (HashMap) yaml.load(connection.getInputStream());
        } catch (IOException e) {
            logger.error("failed to load " + dicPath, e);
        } finally {
            if (connection != null) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
        return null;
    }
}
