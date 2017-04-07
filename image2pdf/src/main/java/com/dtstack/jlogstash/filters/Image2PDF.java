package com.dtstack.jlogstash.filters;

import com.aspose.pdf.*;
import com.dtstack.jlogstash.annotation.Required;
import com.dtstack.jlogstash.util.AsposeLicenseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Image转换为PDF。将输入流中的Image字节数组转换为PDF字节数组
 *
 * @author zxb
 * @version 1.0.0
 *          2017年04月07日 14:49
 * @since Jdk1.6
 */
public class Image2PDF extends BaseFilter {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Image2PDF.class);

    @Required(required = true)
    private static String source;

    private static String target;

    public Image2PDF(Map config) {
        super(config);
    }

    public void prepare() {
        if (target == null || target.trim().length() == 0) {
            target = source;
        }
    }

    protected Map filter(Map event) {
        Object sourceObj = event.get(source);
        if (sourceObj != null) {
            if (List.class.isAssignableFrom(sourceObj.getClass())) {
                List<byte[]> imageList = (List<byte[]>) sourceObj;

                // 将images写入PDF
                byte[] pdfBytes = convertImages2PDF(imageList);
                if (pdfBytes != null) {
                    event.put(target, pdfBytes);
                } else {
                    LOGGER.warn("convert imageList: {} fail, no pdf found.", imageList);
                }
            } else {
                LOGGER.warn("the class of sourceObj: {} is not List<byte[]>.class");
            }
        } else {
            LOGGER.warn("the value get from event: {} by key: {} is null, ignore...", event, source);
        }
        return event;
    }

    private byte[] convertImages2PDF(List<byte[]> imageList) {
        if (AsposeLicenseUtil.setPdfLicense()) {
            BufferedImage originalImage = null;
            Document pdfDocument = new Document();

            // 遍历，将图片添加到PDF
            for (byte[] bytes : imageList) {
                try {
                    originalImage = ImageIO.read(new ByteArrayInputStream(bytes)); // read方法中已经关闭输入流
                } catch (IOException e) {
                    LOGGER.error("read image error", e);
                }

                if (originalImage != null) {
                    Page newPage = pdfDocument.getPages().add();
                    newPage.getResources().getImages().add(originalImage);
                    newPage.getContents().add(new Operator.GSave()); // 保存绘图状态

                    // 设置图片尺寸为PDF大小
                    Rectangle rectangle = newPage.getRect();
                    Matrix matrix = new Matrix(new double[]{rectangle.getURX() - rectangle.getLLX(), 0, 0, rectangle.getURY() - rectangle.getLLY(), rectangle.getLLX(), rectangle.getLLY()});
                    newPage.getContents().add(new Operator.ConcatenateMatrix(matrix));
                    XImage ximage = newPage.getResources().getImages().get_Item(newPage.getResources().getImages().size());

                    newPage.getContents().add(new Operator.Do(ximage.getName())); // 绘制图片
                    newPage.getContents().add(new Operator.GRestore()); // 恢复绘图状态
                    // originalImage.getGraphics().dispose();
                }
            }

            // 将PDF保存到输出流，返回字节数组
            if (pdfDocument.getPages().size() > 0) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2048);

                try {
                    pdfDocument.save(byteArrayOutputStream);
                    return byteArrayOutputStream.toByteArray();
                } finally {
                    try {
                        byteArrayOutputStream.close();
                    } catch (IOException e) {
                        LOGGER.error("close byteArrayOutputStream error", e);
                    }
                }
            }
        } else {
            LOGGER.error("set Aspose pdf license fail");
        }
        return null;
    }
}
