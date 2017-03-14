package study.amazons3integration.aws;

import android.content.Context;
import android.util.Log;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class S3Utils {

    /**
     * Method to generate a presignedurl for the image
     * @param applicationContext context
     * @param path image path
     * @return presignedurl
     */
    public static String generates3ShareUrl(Context applicationContext, String path) {
        String EXPIRY_DATE = "Jan 1, 2037"; // gave for ~20 years

        File f = new File(path);
        AmazonS3 s3client = AmazonUtil.getS3Client(applicationContext);

        Date expiration = new Date();
        long msec = expiration.getTime();
        msec += 1000 * 6000 * 6000; // 1 hour.
        Date d = new Date();
        DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        Date date = null;
        try {
            date = format.parse(EXPIRY_DATE);
            expiration.setTime(date.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            expiration.setTime(msec);
        }
        System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010


        ResponseHeaderOverrides overrideHeader = new ResponseHeaderOverrides();
        overrideHeader.setContentType("image/jpeg");
        String mediaUrl = f.getName();
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(AWSKeys.BUCKET_NAME, mediaUrl);
        generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
        generatePresignedUrlRequest.setExpiration(expiration);
        generatePresignedUrlRequest.setResponseHeaders(overrideHeader);

        URL url = s3client.generatePresignedUrl(generatePresignedUrlRequest);
        Log.e("s", url.toString());
        return url.toString();
    }
}
