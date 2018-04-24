package ticket.checker.reports

import android.content.Context
import com.google.auto.service.AutoService
import org.acra.config.CoreConfiguration
import org.acra.data.StringFormat
import org.acra.sender.HttpSender
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderFactory
import ticket.checker.services.ServiceManager.baseUrl
import ticket.checker.services.ServiceManager.currentPassword
import ticket.checker.services.ServiceManager.currentUsername

@AutoService(CustomReportsSenderFactory::class)
class CustomReportsSenderFactory : ReportSenderFactory {
    override fun create(context: Context, config: CoreConfiguration): ReportSender {
        val httpSender =  HttpSender(config, HttpSender.Method.POST, StringFormat.JSON, "$baseUrl/report")
        httpSender.setBasicAuth(currentUsername, currentPassword)
        return httpSender
    }

    override fun enabled(config: CoreConfiguration): Boolean {
        return true
    }
}