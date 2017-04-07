package com.dtstack.jlogstash.util;


import org.apache.log4j.Logger;

import java.io.*;

/**
 * Aspose注册工具
 *
 * @author zxb
 * @version 1.0.0
 *          2016年10月17日 17:00
 * @since Jdk1.6
 */
public class AsposeLicenseUtil {

    private static byte[] licenseBytes = null;

    private static Logger logger = Logger.getLogger(AsposeLicenseUtil.class);

    static {
        init();
    }

    private static void init() {
        InputStream inputStream = null;
        try {
            inputStream = AsposeLicenseUtil.class.getClassLoader().getResourceAsStream("license.xml");

            licenseBytes = new byte[inputStream.available()];
            inputStream.read(licenseBytes, 0, licenseBytes.length);
        } catch (FileNotFoundException e) {
            logger.error("license not found!", e);
        } catch (IOException e) {
            logger.error("license io exception", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("close inputStream error!", e);
                }
            }
        }
    }

    /**
     * 获取License的输入流
     *
     * @return
     */
    private static InputStream getLicenseInput() {
        if (licenseBytes == null) {
            init();
        }
        return new ByteArrayInputStream(licenseBytes);
    }

    /**
     * 设置Aspose PDF的license
     *
     * @return true表示设置成功，false表示设置失败
     */
    public static boolean setPdfLicense() {
        InputStream licenseInput = getLicenseInput();
        if (licenseInput != null) {
            try {
                com.aspose.pdf.License aposeLic = new com.aspose.pdf.License();
                aposeLic.setLicense(licenseInput);
                return true;
            } catch (Exception e) {
                logger.error("set pdf license error!", e);
            }
        }
        return false;
    }
}
