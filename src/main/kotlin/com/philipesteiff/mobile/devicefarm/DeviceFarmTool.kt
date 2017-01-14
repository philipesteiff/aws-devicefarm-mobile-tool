package com.philipesteiff.mobile.devicefarm

import com.philipesteiff.mobile.devicefarm.client.aws.DeviceFarm.*
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.regions.Regions
import com.philipesteiff.mobile.devicefarm.client.aws.DeviceFarm
import com.philipesteiff.mobile.devicefarm.commandline.Command

fun main(parameters: Array<String>) {
  val deviceFarm = DeviceFarmConfigure.buildWith {
    credentials { EnvironmentVariableCredentialsProvider().credentials }
    region { Regions.US_WEST_2 }
  }
  val command = Command(parameters)
  DeviceFarmTool(command, deviceFarm).start()
}

class DeviceFarmTool(val command: Command, val deviceFarm: DeviceFarm) {

  fun start() {
    val project = deviceFarm.findProjectByName(command.projectName)
    val projectArn = project.arn
    println("Retrieved project: $project")

    val devicePool = deviceFarm.findDevicePoolByName(projectArn, "device-pool-test")
    val devicePoolArn = devicePool.arn
    println("Retrieved devicePool: $devicePool")

    // TODO Remove callbackhell
    deviceFarm.upload(projectArn, command.appPath, AWSUploadType.ANDROID_APP) { upload ->
      val appArn = upload.arn
      println("Start upload App: $upload")
      deviceFarm.uploadProcessingStatus(appArn) {
        println("Upload app finished: $upload")
        deviceFarm.upload(projectArn, command.testPath, AWSUploadType.INSTRUMENTATION_TEST_PACKAGE) { upload ->
          val testArn = upload.arn
          println("Start upload test: $upload")
          deviceFarm.uploadProcessingStatus(testArn) {
            println("Upload test finished: $upload")
            deviceFarm.createRunTest(projectArn, appArn, testArn, devicePoolArn) { run ->
              deviceFarm.checkRun(run.arn)
            }
          }
        }
      }
    }
  }

}


