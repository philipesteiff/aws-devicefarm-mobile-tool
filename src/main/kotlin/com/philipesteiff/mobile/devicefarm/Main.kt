package com.philipesteiff.mobile.devicefarm

import com.philipesteiff.mobile.devicefarm.aws.DeviceFarm.*
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.regions.Regions
import com.philipesteiff.mobile.devicefarm.cli.Command
import org.apache.commons.cli.*

fun main(args: Array<String>) {
    parseArguments(args) { projectName, appPath, testPath, configPath, wait ->
        Companion.run {
            credentials { EnvironmentVariableCredentialsProvider().credentials }
            region { Regions.US_WEST_2 }
            deviceFarmClient { client ->

                val project = client.findProjectByName(projectName)
                val projectArn = project.arn
                println("Retrieved project: $project")

                val devicePool = client.findDevicePoolByName(projectArn, "device-pool-test")
                val devicePoolArn = devicePool.arn
                println("Retrieved devicePool: $devicePool")

                // TODO Remove callbackhell
                client.upload(projectArn, appPath, AWSUploadType.ANDROID_APP) { upload ->
                    val appArn = upload.arn
                    println("Start upload App: $upload")
                    client.uploadProcessingStatus(appArn) {
                        println("Upload app finished: $upload")
                        client.upload(projectArn, testPath, AWSUploadType.INSTRUMENTATION_TEST_PACKAGE) { upload ->
                            val testArn = upload.arn
                            println("Start upload test: $upload")
                            client.uploadProcessingStatus(testArn) {
                                println("Upload test finished: $upload")
                                client.createRunTest(projectArn, appArn, testArn, devicePoolArn) { run ->
                                    client.checkRun(run.arn)
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

fun parseArguments(
        args: Array<String>,
        function: (
                projectName: String,
                appPath: String,
                testPath: String,
                configPath: String,
                wait: Boolean
        ) -> Unit
) {
    Command.parse({
        addArgs { args }
        addOption {
            Option.builder("p")
                    .longOpt("project")
                    .desc("Path to the manifest file or the .apk of the application.")
                    .hasArg()
                    .argName(".apk or manifest")
                    .required()
                    .type(String::class.java)
                    .build()
        }
        addOption {
            Option.builder("ap")
                    .longOpt("appPath")
                    .desc("Path to the manifest file or the .apk of the application.")
                    .hasArg()
                    .argName(".apk or manifest")
                    .required()
                    .type(String::class.java)
                    .build()
        }
        addOption {
            Option.builder("tp")
                    .longOpt("testPath")
                    .desc("Path to the manifest file or the .apk of the application.")
                    .hasArg()
                    .argName(".apk or manifest")
                    .required()
                    .type(String::class.java)
                    .build()
        }
        addOption {
            Option.builder("c")
                    .longOpt("config")
                    .desc("Path to the manifest file or the .apk of the application.")
                    .hasArg()
                    .argName(".apk or manifest")
                    .type(String::class.java)
                    .build()
        }
        addOption {
            Option.builder("w")
                    .longOpt("wait")
                    .desc("Path to the manifest file or the .apk of the application.")
                    .hasArg()
                    .argName(".apk or manifest")
                    .numberOfArgs(1)
                    .type(Boolean::class.java)
                    .build()
        }
    }, { commandLine ->
        function(
                commandLine.getOptionValue("project"),
                commandLine.getOptionValue("appPath"),
                commandLine.getOptionValue("testPath"),
                commandLine.getOptionValue("config"),
                when (commandLine.getOptionValue("wait")) {
                    "true" -> true
                    "false" -> false
                    else -> throw IllegalArgumentException("Bad argument")
                }
        )
    })
}


