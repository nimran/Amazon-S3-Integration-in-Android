package study.amazons3integration.aws

import android.content.Context
import android.net.Uri
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3Client
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws

object AmazonUtil {
    // We only need one instance of the clients and credentials provider
    private var amzonS3Client: AmazonS3Client? = null
    private var credProvider: CognitoCachingCredentialsProvider? = null
    private var transferUtility: TransferUtility? = null

    /**
     * Gets an instance of CognitoCachingCredentialsProvider which is
     * constructed using the given Context.
     *
     * @param context An Context instance.
     * @return A default credential provider.
     */
    internal fun getCredProvider(context: Context?): CognitoCachingCredentialsProvider? {
        if (credProvider == null) {
            // Initialize the Amazon Cognito credentials provider
            credProvider = CognitoCachingCredentialsProvider(
                    context,
                    AWSKeys.COGNITO_POOL_ID,  // Identity Pool ID
                    AWSKeys.MY_REGION // Region
            )
        }
        return credProvider
    }

    /**
     * Gets an instance of a S3 client which is constructed using the given
     * Context.
     *
     * @param context An Context instance.
     * @return A default S3 client.
     */
    fun getS3Client(context: Context?): AmazonS3Client? {
        if (amzonS3Client == null) {
            amzonS3Client = AmazonS3Client(getCredProvider(context),Region.getRegion(AWSKeys.MY_REGION))
            amzonS3Client!!.setRegion(Region.getRegion(AWSKeys.MY_REGION))
        }
        return amzonS3Client
    }

    /**
     * Gets an instance of the TransferUtility which is constructed using the
     * given Context
     *
     * @param context
     * @return a TransferUtility instance
     */
    fun getTransferUtility(context: Context): TransferUtility? {
        if (transferUtility == null) {
            transferUtility = TransferUtility.builder().s3Client(getS3Client(context)).context(context).build()
        }
        return transferUtility
    }

    /**
     * Converts number of bytes into proper scale.
     *
     * @param bytes number of bytes to be converted.
     * @return A string that represents the bytes in a proper scale.
     */
    fun getBytesString(bytes: Long): String {
        val quantifiers = arrayOf(
                "KB", "MB", "GB", "TB"
        )
        var speedNum = bytes.toDouble()
        var i = 0
        while (true) {
            if (i >= quantifiers.size) {
                return ""
            }
            speedNum /= 1024.0
            if (speedNum < 512) {
                return String.format("%.2f", speedNum) + " " + quantifiers[i]
            }
            i++
        }
    }

    /**
     * Copies the data from the passed in Uri, to a new file for use with the
     * Transfer Service
     *
     * @param context
     * @param uri
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun copyContentUriToFile(context: Context, uri: Uri?): File {
        val `is` = context.contentResolver.openInputStream(uri!!)
        val copiedData = File(context.getDir("SampleImagesDir", Context.MODE_PRIVATE), UUID
                .randomUUID().toString())
        copiedData.createNewFile()
        val fos = FileOutputStream(copiedData)
        val buf = ByteArray(2046)
        var read = -1
        while (`is`!!.read(buf).also { read = it } != -1) {
            fos.write(buf, 0, read)
        }
        fos.flush()
        fos.close()
        return copiedData
    }
}