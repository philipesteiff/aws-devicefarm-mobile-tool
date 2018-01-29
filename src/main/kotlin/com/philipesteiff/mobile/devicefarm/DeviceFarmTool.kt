package com.philipesteiff.mobile.devicefarm

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.regions.Regions
import com.philipesteiff.mobile.devicefarm.client.aws.DeviceFarm
import com.philipesteiff.mobile.devicefarm.client.aws.DeviceFarm.AWSUploadType
import com.philipesteiff.mobile.devicefarm.client.aws.DeviceFarm.DeviceFarmConfigure
import com.philipesteiff.mobile.devicefarm.commandline.Command
import java.util.concurrent.CompletableFuture

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

    val devicePool = deviceFarm.findDevicePoolByName(projectArn, "device-pool-test")
    val devicePoolArn = devicePool.arn

    val uploadApp = CompletableFuture
      .supplyAsync { deviceFarm.upload(projectArn, command.appPath, AWSUploadType.ANDROID_APP) }
      .thenApply { arn -> deviceFarm.uploadProcessingStatus(arn); arn }

    val uploadAppTest = CompletableFuture
      .supplyAsync { deviceFarm.upload(projectArn, command.testPath, AWSUploadType.INSTRUMENTATION_TEST_PACKAGE) }
      .thenApply { arn -> deviceFarm.uploadProcessingStatus(arn); arn }

    uploadApp.thenCombine(uploadAppTest) { appArn, appTestArn ->
      deviceFarm.createRunTest(projectArn, appArn, appTestArn, devicePoolArn) { run ->
        deviceFarm.checkRun(run.arn)
      }
    }

  }

}


