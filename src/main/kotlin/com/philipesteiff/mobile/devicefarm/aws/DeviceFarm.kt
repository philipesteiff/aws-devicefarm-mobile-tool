package com.philipesteiff.mobile.devicefarm.aws

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.devicefarm.AWSDeviceFarmAsyncClient
import com.amazonaws.services.devicefarm.model.*
import org.apache.http.Header
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.ContentType
import org.apache.http.entity.FileEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

class DeviceFarm(val awsClient: AWSDeviceFarmAsyncClient) {

    companion object {
        fun run(init: Builder.() -> Unit) = Builder(init)
    }

    enum class AWSUploadType(val type: UploadType) {
        ANDROID_APP(UploadType.ANDROID_APP), // An Android upload.
        IOS_APP(UploadType.IOS_APP), // An iOS upload.
        WEB_APP(UploadType.WEB_APP), // A web appliction upload.
        EXTERNAL_DATA(UploadType.EXTERNAL_DATA), // An external data upload.
        APPIUM_JAVA_JUNIT_TEST_PACKAGE(UploadType.APPIUM_JAVA_JUNIT_TEST_PACKAGE), // An Appium Java JUnit test package upload.
        APPIUM_JAVA_TESTNG_TEST_PACKAGE(UploadType.APPIUM_JAVA_TESTNG_TEST_PACKAGE), // An Appium Java TestNG test package upload.
        APPIUM_PYTHON_TEST_PACKAGE(UploadType.APPIUM_PYTHON_TEST_PACKAGE), // An Appium Python test package upload.
        APPIUM_WEB_JAVA_JUNIT_TEST_PACKAGE(UploadType.APPIUM_WEB_JAVA_JUNIT_TEST_PACKAGE), // An Appium Java JUnit test package upload.
        APPIUM_WEB_JAVA_TESTNG_TEST_PACKAGE(UploadType.APPIUM_WEB_JAVA_TESTNG_TEST_PACKAGE), // An Appium Java TestNG test package upload.
        APPIUM_WEB_PYTHON_TEST_PACKAGE(UploadType.APPIUM_WEB_PYTHON_TEST_PACKAGE), // An Appium Python test package upload.
        CALABASH_TEST_PACKAGE(UploadType.CALABASH_TEST_PACKAGE), // A Calabash test package upload.
        INSTRUMENTATION_TEST_PACKAGE(UploadType.INSTRUMENTATION_TEST_PACKAGE), // An instrumentation upload.
        UIAUTOMATION_TEST_PACKAGE(UploadType.UIAUTOMATION_TEST_PACKAGE), // A uiautomation test package upload.
        UIAUTOMATOR_TEST_PACKAGE(UploadType.UIAUTOMATOR_TEST_PACKAGE), // A uiautomator test package upload.
        XCTEST_TEST_PACKAGE(UploadType.XCTEST_TEST_PACKAGE), // An XCode test package upload.
        XCTEST_UI_TEST_PACKAGE(UploadType.XCTEST_UI_TEST_PACKAGE), // An XCode UI test package upload.
    }


    fun findProjectByName(projectName: String) = awsClient
            .listProjects(ListProjectsRequest())
            .projects
            .filter { it.name == projectName }
            .firstOrNull()
            ?: throw RuntimeException("Project `$projectName` not found.")

    fun findDevicePoolByName(projectArn: String, devicePoolName: String) = awsClient
            .listDevicePools(ListDevicePoolsRequest().withArn(projectArn))
            .devicePools
            .filter { it.name == devicePoolName }
            .firstOrNull()
            ?: throw RuntimeException("Device Pool `$devicePoolName` not found.")

    fun upload(projectArn: String, path: String, AWSUploadType: AWSUploadType, uploadSuccess: (Upload) -> Unit) {
        return awsClient.createUploadAsync(
                CreateUploadRequest()
                        .withProjectArn(projectArn)
                        .withType(AWSUploadType.type)
                        .withName(Paths.get(path).fileName.toString())
                        .withContentType("application/octet-stream"))
                .get()
                .upload
                .let { uploadApp(path, it, uploadSuccess) }
    }

    private fun uploadApp(appPath: String, upload: Upload, uploadSuccess: (Upload) -> Unit) {
        try {
            HttpClients.createDefault().apply {
                HttpPut(upload.url).apply {
                    addHeader("Content-Type", "application/octet-stream")
                    entity = FileEntity(File(appPath))
                }.let {
                    execute(it) { response ->
                        when (response.statusLine.statusCode) {
                            200 -> uploadSuccess.invoke(upload)
                            else -> throw Exception("Upload Error: " +
                                    "StatusCode: ${response.statusLine.statusCode}" +
                                    "Reason: ${response.statusLine.reasonPhrase}"
                            )
                        }
                    }
                }
            }
        } catch(exception: Exception) {
            println("Error: $exception")
        }
    }

    fun uploadProcessingStatus(appArn: String, processingSuccess: () -> Unit) {
        awsClient
                .getUploadAsync(GetUploadRequest().withArn(appArn))
                .get()
                .let {
                    it.upload.apply {
                        println("Upload status: $status")
                        when (status) {
                            "INITIALIZED", "PROCESSING" -> {
                                Timer().schedule(object : TimerTask() {
                                    override fun run() {
                                        uploadProcessingStatus(appArn, processingSuccess)
                                    }
                                }, 2L)
                            }
                            "SUCCEEDED" -> {
                                processingSuccess.invoke()
                            }
                            "FAILED" -> throw Exception("Processing upload failed.")
                        }
                    }
                }
    }

    fun createRunTest(projectArn: String, appArn: String, testArn: String, devicePoolArn: String) {
        ScheduleRunRequest().apply {
            withProjectArn(projectArn)
            withAppArn(appArn)
            withDevicePoolArn(devicePoolArn)
            withTest(ScheduleRunTest()
                    .withType(TestType.INSTRUMENTATION)
                    .withTestPackageArn(testArn)
            )
        }.let {
            try {
                awsClient.scheduleRunAsync(it).get()
            } catch (exception: ExecutionException) {
                exception.cause?.printStackTrace()
            }
        }
    }

    class Builder private constructor() {

        var credentials: AWSCredentials? = null
        var region = Region.getRegion(Regions.US_WEST_2)

        constructor(init: Builder.() -> Unit) : this() {
            init()
        }

        fun credentials(init: () -> AWSCredentials) = apply { credentials = init() }

        fun region(init: () -> Regions) = apply { region = Region.getRegion(init()) }

        fun deviceFarmClient(getClient: (DeviceFarm) -> Unit) {
            AWSDeviceFarmAsyncClient(credentials)
                    .apply { setRegion(region) }
                    .let { getClient(DeviceFarm(it)) }
        }

    }

}
