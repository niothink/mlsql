package tech.mlsql.runtime

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import tech.mlsql.scheduler.algorithm.TimeScheduler
import tech.mlsql.scheduler.client.SchedulerTaskStore

/**
  * 2019-09-09 WilliamZhu(allwefantasy@gmail.com)
  */
class AsSchedulerService extends MLSQLLifecycle {

  import AsSchedulerService._

  override def beforeRuntimeStarted(params: Map[String, String], conf: SparkConf): Unit = {
    if (params.getOrElse(KEY, "false").toBoolean) {

      require(params.contains("streaming.datalake.path"),
        """
          |streaming.datalake.path should also been set when try to make
          |MLSQL work as scheduler service.
        """.stripMargin)

      require(params.contains(CONSOLE_URL), s"${CONSOLE_TOKEN} required")
      require(params.contains(CONSOLE_TOKEN), s"${CONSOLE_TOKEN} required")

      conf.set(PREFIX + KEY, params(KEY))
      conf.set(PREFIX + CONSOLE_URL, params(CONSOLE_URL))
      conf.set(PREFIX + CONSOLE_TOKEN, params(CONSOLE_TOKEN))

    }
  }

  override def afterRuntimeStarted(params: Map[String, String], conf: SparkConf, rootSparkSession: SparkSession): Unit = {
    val timezoneID = rootSparkSession.sessionState.conf.sessionLocalTimeZone
    val authSecret = rootSparkSession.conf.get(PREFIX + CONSOLE_TOKEN) //context.userDefinedParam.filter(f => f._1 == "__auth_secret__").head._2
    val consoleUrl = rootSparkSession.conf.get(PREFIX + CONSOLE_URL) //context.userDefinedParam.filter(f => f._1 == "__default__console_url__").head._2
    TimeScheduler.start(new SchedulerTaskStore(rootSparkSession, consoleUrl, authSecret), timezoneID)
  }
}

object AsSchedulerService {
  val KEY = "streaming.workAs.schedulerService"
  val CONSOLE_URL = KEY + ".consoleUrl"
  val CONSOLE_TOKEN = KEY + ".consoleToken"
  val PREFIX = "spark.mlsql."
}
