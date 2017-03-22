# Amazon-S3-Integration-in-Android
A sample project with descriptive information explaining about getting started and play with Amazon s3 from Android


Following are covered in this repo/tutorial/example
1) Creating a bucket
2) Create Identity pool
3) Configure IAM Role
4) Getting started with Android integration
5) Generating presignedrequest url


--------------------------------
1) **Creating a bucket**

    Signin with your amazon credentials and click s3
    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/create%20bucket.png)

    Enter your details and click create. You can create setup logging, if you need
    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/enter%20bucket%20details.png)

2) **Create Identity pool**

    To create an identity pool:
    Log in to the Amazon Cognito Console and click on Create new identity pool button.

    - I am creating as imranbucket
    - Make sure you check the checkbox to enable access to unauthenticated identities.
    - Click on the Create Pool button to create identity pool.


    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/create_identity_pool-1.png)


    Click on the Allow button to create two default roles associated with your identity pool: one for unauthenticated users and one for authenticated users.
    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/create_identity_pool-2.png)


    **Bucket is created, cognito identifier is created, let us give the necessary permissions to the bucket**


3) **Configure IAM role**

    Click on IAM from the services

    Click on Roles from the side menu

    You will find the two roles which you created in step(2)

    Click your Identity pool name for unauthenticated users (it will have “unauth” appended to your Identity Pool name).

    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/iam-1.png)


    After you selected UnAuthRole, Click on Permissions and create Inline Policy .
    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/iam-2.png)

    Click on Select to set the policies

    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/iam-3.png)

    On the Edit Permissions page, select Amazon S3 as AWS Service, select All Actions(*) for Actions and enter the arn:aws:s3:::your_bucket_name/* as Amazon Resource Name (ARN).

    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/iam-4.png)
    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/iam-5.png)
    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/iam-6.png)
    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/iam-7.png)
    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/iam-8.png)


    **CLICK ON TRUST RELATIONSHIPS AND NOTE DOWN YOUR COGNITO POOL ID**
    ![alt tag](https://github.com/nimran/Amazon-S3-Integration-in-Android/blob/master/images/cognito%20pool%20id.png)

    Thats it, you are done :) Let's move onto Android


4) **Getting started with Android integration**

    a) Get the AWS Mobile SDK for Android
    dependencies {
        compile 'com.amazonaws:aws-android-sdk-s3:2.2.+'
            compile 'com.amazonaws:aws-android-sdk-cognito:2.2.+'
            compile 'com.amazonaws:aws-android-sdk-cognitoidentityprovider:2.2.+'
    }

    b) set the necessary manifest permissons like Internet

    c) Let us add the transfer service of SDK

        Goto Manifest and add Transfer Service

        **<service
            android:name="com.amazonaws.mobileconnectors.s3.transferutility.TransferService"
            android:enabled="true" />**


     d) Initialize Credential Provider

        **new CognitoCachingCredentialsProvider(
                            context,
                            AWSKeys.COGNITO_POOL_ID, // Identity Pool ID
                            Regions.US_EAST_1 // Region
                    )**

     e) Set Amazon client

        **sS3Client = new AmazonS3Client(getCredProvider(context));
                    sS3Client.setRegion(Region.getRegion(Regions.US_EAST_1));**


     f) Configure your keys in AWSKEYS.java before running the application

     g) Set file to upload

        File file = new File(filePath);
        ObjectMetadata myObjectMetadata = new ObjectMetadata();
        myObjectMetadata.setContentType("image/png");
        String mediaUrl = file.getName();
        TransferObserver observer = transferUtility.upload(AWSKeys.BUCKET_NAME, mediaUrl,
                file);
        observer.setTransferListener(new UploadListener());

     h) To know the status of uploading and downloading the file, we need to set transfer listeners. Amazon SDK provides TransferObserver class

        private class UploadListener implements TransferListener {

            // Simply updates the UI list when notified.
            @Override
            public void onError(int id, Exception e) {
                Log.e(TAG, "Error during upload: " + id, e);
                s3UploadInterface.onUploadError(e.toString());
                s3UploadInterface.onUploadError("Error");
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d(TAG, String.format("onProgressChanged: %d, total: %d, current: %d",
                        id, bytesTotal, bytesCurrent));
            }

            @Override
            public void onStateChanged(int id, TransferState newState) {
                Log.d(TAG, "onStateChanged: " + id + ", " + newState);
                if (newState == TransferState.COMPLETED) {
                    s3UploadInterface.onUploadSuccess("Success");
                }
            }
        }


5) ** Generate PresignedUrl**

    If you want to store the image details in the DB, obviously you will not store the entire image, you need to store the image in some storage places and give the references
    to the DB. So in Amazon, after we store the image to s3, we need a reference or url to save in DB.

    Let us create presignedurl

    ResponseHeaderOverrides overrideHeader = new ResponseHeaderOverrides();
    overrideHeader.setContentType("image/jpeg");
    String mediaUrl = f.getName();
    GeneratePresignedUrlRequest generatePresignedUrlRequest =
            new GeneratePresignedUrlRequest(AWSKeys.BUCKET_NAME, mediaUrl);
    generatePresignedUrlRequest.setMethod(HttpMethod.GET); // Default.
    generatePresignedUrlRequest.setExpiration(expiration);
    generatePresignedUrlRequest.setResponseHeaders(overrideHeader);



    * Full Code *
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

    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/android-log.png)







For iOS, please find the code https://gist.github.com/nimran/008fac7f9a9a6c88f166e2108465ac39


HAPPY CODING :)


Thanks
Imran
https://nimran.github.io
