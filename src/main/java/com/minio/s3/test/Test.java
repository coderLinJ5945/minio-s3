package com.minio.s3.test;

import io.minio.MinioClient;


public class Test {
    //public static  String url = "http://111.22.15.73:9000";
    public static  String url = " http://192.168.2.121:29018";
    public static String accessKey = "minio";
    public static String secretKey = "minio123456";
    public static String bucketName = "asiatrip";
    //唯一key
    public static String objectName = "date.png";
    public static String fileName  ="C:\\Users\\linj\\Desktop\\temp\\jstack locked.png";

    public static void main(String[] args) {
       testUpload();
        // getUrl();
    }
    //测试上传
    public static void testUpload(){
        try {
            MinioClient minioClient = new MinioClient(url, accessKey, secretKey);
            boolean isExist = minioClient.bucketExists(bucketName);
            if (isExist) {
                System.out.println("Bucket already exists.");
            } else {
                minioClient.makeBucket(bucketName);
                System.out.println("创建bucket：asiatrip成功!!!");
            }
            // 上传文件
            minioClient.putObject(bucketName, objectName, fileName);
            System.out.println("上传成功");
        }catch (Exception e){
            System.out.println("error");
        }
    }

    //测试获取上传的url
    public static void getUrl(){
        try{
        MinioClient minioClient = new MinioClient(url, accessKey, secretKey);
        String url = minioClient.getObjectUrl(bucketName,objectName);
        System.out.println(url);
        }catch (Exception e){
            System.out.println("error");
        }
    }
}
