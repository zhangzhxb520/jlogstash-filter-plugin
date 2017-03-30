package com.dtstack.jlogstash.compiler;

import com.dtstack.jlogstash.exception.InitializeException;
import org.apache.commons.lang3.StringUtils;

import javax.tools.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 代码编译器
 *
 * @author zxb
 * @version 1.0.0
 *          2017年03月29日 14:21
 * @since Jdk1.6
 */
public class CodeCompiler {

    private File classPathDir;

    public CodeCompiler(String classPathDir) {
        initClassPath(classPathDir);
    }

    public CodeCompiler() {
        this(null);
    }

    /**
     * 初始化ClassPath
     *
     * @param classPathDir
     */
    private void initClassPath(String classPathDir) {
        if (StringUtils.isEmpty(classPathDir)) {
            String classPath = System.getProperty("user.dir") + File.separator + "plugin" + File.separator + "filter" + File.separator + "classes" + File.separator;
            this.classPathDir = new File(classPath);
        } else {
            this.classPathDir = new File(classPathDir);
        }

        if (!this.classPathDir.exists()) {
            this.classPathDir.mkdirs();
        }
    }

    public Class<?> compile(String fullClass, String code) {
        int lastDotIndex = fullClass.lastIndexOf(".");
        String packageName = fullClass.substring(0, lastDotIndex);
        String className = fullClass.substring(lastDotIndex + 1);

        JavaFileObject file = new JavaSourceFromString(className, code);
        Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(file);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        List<String> options = new ArrayList<String>();
        options.add("-d");
        options.add(classPathDir.getAbsolutePath());
        options.add("-classpath");
        options.add(getClassPath());

        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, options, null, fileObjects);
        if (task.call()) {
            try {
                // 使用Java.class的ClassLoader，反射调用addURL方法添加编译后的class输出路径到该classLoader的classpath中。
                URLClassLoader urlClassLoader = (URLClassLoader) CodeCompiler.class.getClassLoader();
                Class<?> clClazz = urlClassLoader.getClass();
                Method method = clClazz.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(urlClassLoader, classPathDir.toURI().toURL());

                // 使用CodeCompiler.class的ClassLoader加载该类。
                // 使用此类加载器，既可以访问父类加载器AppClassLoader的类成员，也可以访问当前Java这个类的成员。
                String fullClassName = packageName + "." + className;
                return urlClassLoader.loadClass(fullClassName);
            } catch (Exception e) {
                throw new InitializeException("find class error", e);
            } finally {
                deleteFile(classPathDir);
            }
        } else {
            deleteFile(classPathDir);
            StringBuilder sb = new StringBuilder();
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                sb.append(diagnostic.getMessage(Locale.CHINA));
            }
            throw new InitializeException(sb.toString());
        }
    }

    /**
     * 获取当前类的classpath
     *
     * @return
     */
    private String getClassPath() {
        URLClassLoader currentCL = (URLClassLoader) getClass().getClassLoader();
        URLClassLoader parentCL = (URLClassLoader) getClass().getClassLoader().getParent();
        StringBuilder sb = new StringBuilder();
        sb.append(getClassPathURL(parentCL)); // 父加载器（Logstash）
        sb.append(getClassPathURL(currentCL)); // 类加载器（当前Filter的）
        sb.append(classPathDir.getAbsolutePath());
        return sb.toString();
    }

    /**
     * 获取当前类加载器所加载的jar包路径
     *
     * @param classLoader
     * @return
     */
    private String getClassPathURL(URLClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        for (URL url : classLoader.getURLs()) {
            sb.append(url.getFile()).append(File.pathSeparator);
        }
        return sb.toString();
    }

    /**
     * 删除文件或目录
     *
     * @param file
     */
    private void deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0, len = files.length; i < len; i++) {
                    deleteFile(files[i]);
                }
                file.delete(); // delete the directory
            }
        }
    }
}
