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

   * Go to [Amazon IAM Console](https://console.aws.amazon.com/iam/home) and select "Roles".
   * Select the `unauth` role you just created in step 1, which is of the form `Cognito_<IdentityPoolName>Unauth_Role`.
   * Select `Attach Policy`, then find `AmazonS3FullAccess` and attach it it to the role.
   * Note:  This will grant users in the identity pool full access to all buckets and operations in S3.  In a real app, you should restrict users to only have          access to the resources they need.
   
  

    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/iam-1.png)


    After you selected UnAuthRole, Click on Attach policy and find AmazonS3FullAccess and attach it
    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/IAM_Management_Console-2.png)
    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/IAM_Management_Console-3.png)

   

    Thats it, you are done :) Let's move onto Android


4) **Getting started with Android integration**

    a) Get the AWS Mobile SDK for Android
    	
        dependencies {
        	implementation 'com.amazonaws:aws-android-sdk-s3:2.18.0'
            implementation 'com.amazonaws:aws-android-sdk-cognito:2.18.0'
            implementation 'com.amazonaws:aws-android-sdk-cognitoidentityprovider:2.18.0'
    	}

    b) set the necessary manifest permissons like Internet

    c) Let us add the transfer service of SDK

        Goto Manifest and add Transfer Service

        <service
            android:name="com.amazonaws.mobileconnectors.s3.transferutility.TransferService"
            android:enabled="true" />


     d) Initialize Credential Provider

        credProvider = CognitoCachingCredentialsProvider(
                    context,
                    AWSKeys.COGNITO_POOL_ID,  // Identity Pool ID
                    AWSKeys.MY_REGION // Region

     e) Set Amazon client

       amzonS3Client = AmazonS3Client(getCredProvider(context),Region.getRegion(AWSKeys.MY_REGION))
          


     f) Configure your keys in **AWSKEYS.java** before running the application
     	
        object AWSKeys {
            internal const val COGNITO_POOL_ID = "YOUR COGNITO POOL ID"
            internal val MY_REGION = Regions.AP_SOUTHEAST_1 // WHAT EVER REGION IT MAY BE,PLEASE CHOOSE EXACT
            const val BUCKET_NAME = "YOUR BUCKET"
          }
        
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

        val overrideHeader = ResponseHeaderOverrides()
        overrideHeader.contentType = getMimeType(path)
        val mediaUrl = f.name
        val generatePresignedUrlRequest = GeneratePresignedUrlRequest(study.amazons3integration.aws.AWSKeys.BUCKET_NAME, mediaUrl)
        generatePresignedUrlRequest.method = HttpMethod.GET // Default.
        generatePresignedUrlRequest.expiration = expiration
        generatePresignedUrlRequest.responseHeaders = overrideHeader
        val url = s3client.generatePresignedUrl(generatePresignedUrlRequest)

    * Full Code *
    
            fun generates3ShareUrl(applicationContext: Context?, path: String?): String {
                val f = File(path)
                val s3client: AmazonS3 = AmazonUtil.getS3Client(applicationContext)!!
                val expiration = Date()
                var msec = expiration.time
                msec += 1000 * 60 * 60.toLong() // 1 hour.
                expiration.time = msec
                val overrideHeader = ResponseHeaderOverrides()
                overrideHeader.contentType = getMimeType(path)
                val mediaUrl = f.name
                val generatePresignedUrlRequest = GeneratePresignedUrlRequest(study.amazons3integration.aws.AWSKeys.BUCKET_NAME, mediaUrl)
                generatePresignedUrlRequest.method = HttpMethod.GET // Default.
                generatePresignedUrlRequest.expiration = expiration
                generatePresignedUrlRequest.responseHeaders = overrideHeader
                val url = s3client.generatePresignedUrl(generatePresignedUrlRequest)
                Log.e("Generated Url - ", url.toString())
                return url.toString()
            }

    ![alt tag](https://raw.githubusercontent.com/nimran/Amazon-S3-Integration-in-Android/master/images/android-log.png)







For iOS, please find the code https://gist.github.com/nimran/008fac7f9a9a6c88f166e2108465ac39


HAPPY CODING :)


Thanks
Imran
https://nimran.github.io
