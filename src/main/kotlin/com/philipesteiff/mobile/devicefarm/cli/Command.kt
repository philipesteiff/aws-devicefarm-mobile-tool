package com.philipesteiff.mobile.devicefarm.cli

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
        Option.builder("w")
          .longOpt("wait")
          .desc("Path to the manifest file or the .apk of the application.")
          .hasArg()
          .argName(".apk or manifest")
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