package com.philipesteiff.mobile.devicefarm.client.aws

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.devicefarm.AWSDeviceFarmAsyncClient
import com.amazonaws.services.devicefarm.model.*
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.FileEntity
import org.apache.http.impl.client.HttpClients
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ExecutionException

class DeviceFarm(val awsClient: AWSDeviceFarmAsyncClient) {

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

  fun upload(projectArn: String, path: String, awsUploadType: AWSUploadType): String {
    return awsClient.createUploadAsync(
      CreateUploadRequest()
        .withProjectArn(projectArn)
        .withType(awsUploadType.type)
        .withName(Paths.get(path).fileName.toString())
        .withContentType("application/octet-stream"))
      .get().upload
      .let { uploadApp(path, it).arn }
  }

  @Deprecated("Deprecated")
  fun upload(projectArn: String, path: String, awsUploadType: AWSUploadType, uploadSuccess: (Upload) -> Unit) {
    return awsClient.createUploadAsync(
      CreateUploadRequest()
        .withProjectArn(projectArn)
        .withType(awsUploadType.type)
        .withName(Paths.get(path).fileName.toString())
        .withContentType("application/octet-stream"))
      .get()
      .upload
      .let { uploadApp(path, it, uploadSuccess) }
  }

  private fun uploadApp(appPath: String, upload: Upload): Upload {
    val httpClient = HttpClients.createDefault()
    val httpPut = HttpPut(upload.url).apply {
      addHeader("Content-Type", "application/octet-stream")
      entity = FileEntity(File(appPath))
    }

    val response = httpClient.execute(httpPut)
    return when (response.statusLine.statusCode) {
      200 -> upload
      else -> throw Exception("Upload Error: " +
        "StatusCode: ${response.statusLine.statusCode}" +
        "Reason: ${response.statusLine.reasonPhrase}"
      )
    }
  }

  @Deprecated("Deprecated")
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

  fun uploadProcessingStatus(appArn: String) {
    val upload = awsClient.getUploadAsync(GetUploadRequest().withArn(appArn)).get().upload
    upload.apply {
      println("Upload status: $status")
      when (status) {
        "INITIALIZED", "PROCESSING" -> {
          Timer().schedule(object : TimerTask() {
            override fun run() {
              uploadProcessingStatus(appArn)
            }
          }, 2L)
        }
        "SUCCEEDED" -> return
        "FAILED" -> throw Exception("Processing upload failed.")
      }
    }
  }

  @Deprecated("Deprecated")
  fun uploadProcessingStatus(appArn: String, processingSuccess: () -> Unit) {
    awsClient.getUploadAsync(GetUploadRequest().withArn(appArn)).get().let {
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

  fun createRunTest(
    projectArn: String,
    appArn: String,
    testArn: String,
    devicePoolArn: String,
    createRunSuccess: (Run) -> Unit
  ) {
    return ScheduleRunRequest().apply {
      withProjectArn(projectArn)
      withAppArn(appArn)
      withDevicePoolArn(devicePoolArn)
      withTest(ScheduleRunTest()
        .withType(TestType.INSTRUMENTATION)
        .withTestPackageArn(testArn)
      )
    }.let {
      try {
        awsClient.scheduleRunAsync(it).get().let { scheduleRunResult ->
          scheduleRunResult.run.let {
            println("RunTest status: ${it.status}")
            createRunSuccess(it)
          }
        }
      } catch (exception: ExecutionException) {
        exception.cause?.printStackTrace()
      }
    }
  }

  fun checkRun(runArn: String) {
    try {
      awsClient.getRunAsync(GetRunRequest().withArn(runArn)).get().run.let {
        println("RunTest status: ${it.status}")
        if (it.status != "COMPLETED") {
          Timer().schedule(object : TimerTask() {
            override fun run() {
              checkRun(runArn)
            }
          }, 2L)
        } else {
          println("RunTest result: ${it.result}")
          when (it.result) {
            "PASSED" -> System.exit(0)
            else -> System.exit(1)
          }

        }
      }
    } catch (exception: ExecutionException) {
      exception.cause?.printStackTrace()
      System.exit(1)
    }
  }

  companion object DeviceFarmConfigure {
    fun buildWith(init: Builder.() -> Unit) = Builder(init).build()
  }

  class Builder private constructor() {

    var credentials: AWSCredentials? = null
    var region: Region = Region.getRegion(Regions.US_WEST_2)

    constructor(init: Builder.() -> Unit) : this() {
      init()
    }

    fun credentials(init: () -> AWSCredentials) = apply { credentials = init() }

    fun region(init: () -> Regions) = apply { region = Region.getRegion(init()) }

    fun build() = AWSDeviceFarmAsyncClient(credentials)
      .apply { setRegion(region) }
      .let(::DeviceFarm)

  }

}
