package com.minio.s3.util;

import com.xiaoleilu.hutool.io.FileUtil;
import com.xiaoleilu.hutool.setting.dialect.Props;
import com.xiaoleilu.hutool.util.StrUtil;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * minio对接工具类
 */
public class MinioUtil {

    private static Logger logger = LoggerFactory.getLogger(MinioUtil.class);

    private  static final Props PROPS = new Props(MinioEnum.PROPERTIES.key);
    /**外网端口，用于下载预览*/
    private static final String URL_NETWORK = PROPS.getProperty(MinioEnum.URL_NETWORK.key);
    /**内网端口，上传走内网速度较快*/
    private static final String URL_INTRANET = PROPS.getProperty(MinioEnum.URL_INTRANET.key);

    private static final String ACCESSKEY = PROPS.getProperty(MinioEnum.ACCESSKEY.key);

    private static final String SECRETKEY= PROPS.getProperty(MinioEnum.SECRETKEY.key);

    private static final String BUCKETNAME = PROPS.getProperty(MinioEnum.DEFAULT_BUCKETNAME.key);

    public MinioUtil(){
    }

    public enum MinioEnum {
        /**配置文件名*/
        PROPERTIES("s3.properties"),
        /**外网url的key*/
        URL_NETWORK("s3.url.network"),
        /**内网url的key*/
        URL_INTRANET("s3.url.intranet"),
        /**minio 秘钥名 */
        ACCESSKEY("s3.accessKey"),
        /**minio 秘钥名 */
        SECRETKEY("s3.secretKey"),
        /**默认桶名*/
        DEFAULT_BUCKETNAME("s3.bucketName");
        private final String key;
        MinioEnum(String key) {
            this.key = key;
        }
    }

    /**
     * 对接Spring的文件上传接口
     * @param multipartFile
     * @return
     */
    public static ServerResponse upload(MultipartFile multipartFile) {
        ServerResponse result = upload(URL_NETWORK,URL_INTRANET,ACCESSKEY,SECRETKEY,BUCKETNAME, multipartFile);
        return result;
    }
    /**
     * 文件上传接口：
     * 内网连接上传，返回外网访问下载地址
     * 如果都是用外网或者内网，url可以填写相同
     * @param urlNetwork 外网的url
     * @param urlIntranet 内网的url
     * @param accessKey
     * @param secretKey
     * @param bucketName
     * @param multipartFile
     * @return
     */
    public static ServerResponse upload(final String urlNetwork,final String urlIntranet,final String accessKey,final String secretKey, final String bucketName, MultipartFile multipartFile){
        ConcurrentHashMap result = new ConcurrentHashMap();
        try {
            //内网连接上传
            MinioClient minioClient = new MinioClient(urlIntranet, accessKey, secretKey);
            boolean isExist = minioClient.bucketExists(bucketName);
            if (!isExist) {
                /**
                 * 这里不建议直接作为参数进行创建bucketName，而是需要进行规范验证之后再创建
                 * 原因：bucketName必须要满足AWS S3 的bucket的桶名要求
                 * bucketName满足条件参考地址：
                 * https://www.crifan.com/aws_s3_bucket_naming_rule/
                 *
                 */
                return ServerResponse.createByError("bucketName参数错误");
            }
            //key === objectName
            String key = getkey(multipartFile.getOriginalFilename());
            // 上传文件
            minioClient.putObject(bucketName,key,multipartFile.getInputStream(),multipartFile.getSize(),multipartFile.getContentType());
            //上传成功之后，返回外网下载地址
            String url = minioClient.getObjectUrl(bucketName,key);
            //替换外网操作
            url = replaceUrl(url,urlIntranet,urlNetwork);
            result.put("bucketName", bucketName);
            result.put("key", key);
            result.put("url", url);

        } catch (Exception e) {
            logger.error(e.toString());
            return ServerResponse.createByError("上传文件异常");
        }
        return ServerResponse.createBySucess("上传成功", result);
    }

    /**
     * 拼接上传文件的key，前缀+“_”+uuid后缀（根据具体要求实现）
     * @param fileName
     * @return
     */
    private static String getkey(String fileName){
        if(fileName == null){
            return null;
        }else{
            int index = fileName.lastIndexOf(".");
            if(index == -1){
                return "";
            } else{
                String prefix = FileUtil.mainName(fileName);
                String suffix = FileUtil.extName(fileName);
                StringBuffer keybf = new StringBuffer(prefix);
                keybf.append("_").append(UUID.randomUUID().toString()).append(".").append(suffix);
                return keybf.toString();
            }
        }

    }

    /**
     * 做url的内外网匹配替换
     * @param url
     * @param urlIntranet
     * @param urlNetwork
     * @return
     */
    public static String replaceUrl(String url,String urlIntranet, String urlNetwork){
        StringBuffer sb = new StringBuffer(urlIntranet);
        sb.append("(.*)");
        if(StrUtil.isNotEmpty(urlNetwork) && StrUtil.isNotEmpty(url) && url.matches(sb.toString())){
            url = url.replaceAll(urlIntranet,urlNetwork);
        }
        return url;
    }

    public static void main(String[] args) throws IOException {

       /* String url = "http://172.17.2.122:9000/asiatrip/date_6331afda-a603-466c-a762-1648b1045483.png";

        url  = replaceUrl(url,"http://172.17.2.122:9000","http://111.22.15.73:9000");

        System.out.println(url);*/
       /* String filepath = "C://Users/linj/Desktop/temp/test.txt";
        File file = new File(filepath);
        FileInputStream input = new FileInputStream(file);
        MultipartFile multipartFile = new MockMultipartFile("file", file.getName(), "text/plain", IOUtils.toByteArray(input));
        ServerResponse result = MinioUtil.upload(multipartFile);
        System.out.println(result.getData().toString());*/
      /*  String filepath = "C://Users/linj/Desktop/temp/nginx/test.txt";
        File file = new File(filepath);

        FileInputStream input = new FileInputStream(file);
         MultipartFile multipartFile = new MockMultipartFile("file", file.getName(), "text/plain", IOUtils.toByteArray(input));
        ServerResponse result = AwsUtils.upload(multipartFile);
        System.out.println(result.getData().toString());*/


    /*    String s = "192.168.6.130:7480192.168.6.130:7480192.168.6.130:7480192.168.6.130:7480";
        System.out.println(s.replace("192.168.6.130:7480", "100.124.21.1:7480"));
        System.out.println(s.replaceAll("192.168.6.130:7480", "100.124.21.1:7480"));
        System.out.println(s.replaceFirst("192.168.6.130:7480", "100.124.21.1:7480"));*/
    }


}