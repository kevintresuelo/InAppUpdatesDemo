package com.kevintresuelo.tests.updater

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.kevintresuelo.tests.updater.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {

        const val UPDATE_IMMEDIATE_REQUEST_CODE = 4502
        const val UPDATE_FLEXIBLE_REQUEST_CODE = 6708

    }

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        binding.version.text = "${BuildConfig.VERSION_NAME} // ${BuildConfig.VERSION_CODE}"
        binding.button.setOnClickListener {
            checkForUpdates(true)
        }
    }

    override fun onResume() {
        super.onResume()

        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                            val snackbar: Snackbar = Snackbar.make(
                                binding.mainCl,
                                getString(R.string.update_download_finished),
                                Snackbar.LENGTH_INDEFINITE
                            )
                            snackbar.setAction(
                                getString(R.string.update_restart),
                                { view -> appUpdateManager.completeUpdate() })
                            snackbar.setActionTextColor(resources.getColor(R.color.colorAccent, theme))
                            snackbar.show()
                        }
                    }
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            UPDATE_IMMEDIATE_REQUEST_CODE
                        )
                    }
                }
            }
        }
    }

    fun checkForUpdates(userTriggered: Boolean = false) {
        // Creates instance of the manager.
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        // Immediate, required update
                        appUpdateManager.startUpdateFlowForResult(
                            // Pass the intent that is returned by 'getAppUpdateInfo()'.
                            appUpdateInfo,
                            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                            AppUpdateType.IMMEDIATE,
                            // The current activity making the update request.
                            this,
                            // Include a request code to later monitor this update request.
                            UPDATE_IMMEDIATE_REQUEST_CODE
                        )
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        // Flexible, optional update
                        // Create a listener to track request state updates.
                        val listener = { state: InstallState ->
                            // Show module progress, log state, or install the update.
                            when (state.installStatus()) {
                                InstallStatus.DOWNLOADED -> {
                                    // After the update is downloaded, show a notification
                                    // and request user confirmation to restart the app.
                                    val snackbar: Snackbar = Snackbar.make(
                                        binding.mainCl,
                                        getString(R.string.update_download_finished),
                                        Snackbar.LENGTH_INDEFINITE
                                    )
                                    snackbar.setAction(
                                        getString(R.string.update_restart),
                                        { view -> appUpdateManager.completeUpdate() })
                                    snackbar.setActionTextColor(resources.getColor(R.color.colorAccent, theme))
                                    snackbar.show()
                                }
                                InstallStatus.FAILED -> {
                                    val snackbar: Snackbar = Snackbar.make(
                                        binding.mainCl,
                                        getString(R.string.update_download_failed),
                                        Snackbar.LENGTH_LONG
                                    )
                                    snackbar.setAction(getString(R.string.update_retry), { checkForUpdates() })
                                    snackbar.setActionTextColor(resources.getColor(R.color.colorAccent, theme))
                                    snackbar.show()
                                }
                            }
                        }

                        // Before starting an update, register a listener for updates.
                        appUpdateManager.registerListener(listener)

                        // Start an update.
                        appUpdateManager.startUpdateFlowForResult(
                            // Pass the intent that is returned by 'getAppUpdateInfo()'.
                            appUpdateInfo,
                            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                            AppUpdateType.FLEXIBLE,
                            // The current activity making the update request.
                            this,
                            // Include a request code to later monitor this update request.
                            UPDATE_FLEXIBLE_REQUEST_CODE
                        )

                        // When status updates are no longer needed, unregister the listener.
                        appUpdateManager.unregisterListener(listener)
                    }
                }
                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.update_notavailable_title))
                        .setMessage(getString(R.string.update_notavailable_message))
                        .setPositiveButton(getString(R.string.update_notavailable_ok), null)
                        .show()
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.update_inprogress_title))
                        .setMessage(getString(R.string.update_inprogress_message))
                        .setPositiveButton(getString(R.string.update_inprogress_ok), null)
                        .show()
                }
                UpdateAvailability.UNKNOWN -> {
                    if (userTriggered) {
                        AlertDialog.Builder(this)
                            .setTitle(getString(R.string.update_unknown_title))
                            .setMessage(getString(R.string.update_unknown_message))
                            .setPositiveButton(getString(R.string.update_unknown_ok), null)
                            .show()
                    }
                }
            }
        }
    }
}
