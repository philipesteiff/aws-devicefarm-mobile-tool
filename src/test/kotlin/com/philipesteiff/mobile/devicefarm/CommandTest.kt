package com.philipesteiff.mobile.devicefarm

import com.philipesteiff.mobile.devicefarm.commandline.Command
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class CommandTest {

  val projectArg = "myproject"
  val appPathArg = "myapppath"
  val testPathArg = "mytestpath"
  val waitTrueArg = "false" to false
  val waitFalseArg = "true" to true

  fun androidMockParameters(waitArg: Pair<String, Boolean>) = arrayOf(
    "-project", projectArg,
    "-appPath", appPathArg,
    "-testPath", testPathArg,
    "-wait", waitArg.first
  )

  @Test
  fun allArgumentsShouldBeParsedCorrectly() {
    Command(androidMockParameters(waitTrueArg)).apply {
      assertThat(projectName, equalTo(projectArg))
      assertThat(appPath, equalTo(appPathArg))
      assertThat(testPath, equalTo(testPathArg))
      assertThat(wait, equalTo(waitTrueArg.second))
    }
    Command(androidMockParameters(waitFalseArg)).apply {
      assertThat(wait, equalTo(waitFalseArg.second))
    }
  }

}