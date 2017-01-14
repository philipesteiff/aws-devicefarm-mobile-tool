package com.philipesteiff.mobile.devicefarm.commandline

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option

class Command(parameters: Array<String>) {

  lateinit var projectName: String
  lateinit var appPath: String
  lateinit var testPath: String
  var wait: Boolean = false

  init {
    CommandInput.parse({
      addArgs { parameters }
      addOption {
        Option.builder("project")
          .desc("Project name in Device Farm console.")
          .hasArg()
          .argName("Just the project name.")
          .required()
          .type(String::class.java)
          .build()
      }
      addOption {
        Option.builder("appPath")
          .desc("Path to the .apk of the application.")
          .hasArg()
          .argName(".apk")
          .required()
          .type(String::class.java)
          .build()
      }
      addOption {
        Option.builder("testPath")
          .desc("Path to the test .apk of the application.")
          .hasArg()
          .argName(".apk")
          .required()
          .type(String::class.java)
          .build()
      }
      addOption {
        Option.builder("wait")
          .desc("Set true to wait test finish. Default is false.")
          .hasArg()
          .argName("true or false")
          .numberOfArgs(1)
          .type(Boolean::class.java)
          .build()
      }
    }, { commandLine -> retrieveParseValues(commandLine) })
  }

  private fun retrieveParseValues(commandLine: CommandLine) {
    projectName = commandLine.getOptionValue("project")
    appPath = commandLine.getOptionValue("appPath")
    testPath = commandLine.getOptionValue("testPath")
    wait = when (commandLine.getOptionValue("wait")) {
      "true" -> true
      else -> false
    }
  }
}