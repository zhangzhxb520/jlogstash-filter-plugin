package com.dtstack.jlogstash.filters;

import com.dtstack.jlogstash.util.IOUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * @author zxb
 * @version 1.0.0
 *          2017年04月07日 15:13
 * @since Jdk1.6
 */
public class Image2PDFTest {

    @org.junit.Test
    public void filter() throws Exception {
        Field sourceFiled = Image2PDF.class.getDeclaredField("source");
        sourceFiled.setAccessible(true);
        sourceFiled.set(null, "images");

        Map<String, Object> config = new HashMap<String, Object>();
        Image2PDF image2PDF = new Image2PDF(config);
        image2PDF.prepare();

        Map<String, Object> event = new HashMap<String, Object>();
        List<byte[]> imageList = new ArrayList<byte[]>();
        imageList.add(readImage("1.png"));
        imageList.add(readImage("2.png"));
        imageList.add(readImage("3.png"));
        imageList.add(readImage("4.png"));
        imageList.add(readImage("5.png"));

        event.put("images", imageList);

        event = image2PDF.filter(event);
        byte[] pdfBytes = (byte[]) event.get("images");
        if (pdfBytes == null) {
            fail();
        }

        String path = Image2PDFTest.class.getResource("/").getPath();
        InputStream inputStream = new ByteArrayInputStream(pdfBytes);
        OutputStream outputStream = new FileOutputStream(path + "/images.pdf");
        IOUtils.copy(inputStream, outputStream, new byte[2048]);
        IOUtils.closeQuietly(outputStream);
    }

    private byte[] readImage(String path) {
        InputStream inputStream = Image2PDFTest.class.getClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        try {
            IOUtils.copy(inputStream, baos, new byte[2048]);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(baos);
        }
        return null;
    }

}