package com.dtstack.jlogstash.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author zxb
 * @version 1.0.0
 *          2017年03月29日 14:49
 * @since Jdk1.6
 */
public class TemplateUtil {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateUtil.class);


    /**
     * 从指定路径读取模板文件内容
     *
     * @param templatePath
     * @return
     * @throws IOException
     */
    public static String readTemplateFromPath(String templatePath) throws IOException {
        InputStream inputStream = TemplateUtil.class.getClassLoader().getResourceAsStream(templatePath);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                LOGGER.error("close template reader error", e);
            }
        }
    }
}
