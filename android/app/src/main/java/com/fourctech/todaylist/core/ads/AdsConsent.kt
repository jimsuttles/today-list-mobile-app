package com.fourctech.todaylist.core.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** Google UMP consent for AdMob. No-ops quietly when a form is not required. */
object AdsConsent {
    fun gather(activity: Activity, onFinished: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form: ${formError.message}")
                    }
                    Log.d(
                        TAG,
                        "Consent status=${consentInformation.consentStatus} " +
                            "canRequestAds=${consentInformation.canRequestAds()}",
                    )
                    onFinished()
                }
            },
            { error ->
                Log.w(TAG, "Consent info update failed: ${error.message}")
                onFinished()
            },
        )
    }

    fun canRequestAds(activity: Activity): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).canRequestAds() ||
            UserMessagingPlatform.getConsentInformation(activity).consentStatus ==
            ConsentInformation.ConsentStatus.NOT_REQUIRED

    private const val TAG = "AdsConsent"
}
