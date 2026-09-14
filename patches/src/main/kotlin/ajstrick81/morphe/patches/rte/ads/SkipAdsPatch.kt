package ajstrick81.morphe.patches.rte.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import ajstrick81.morphe.patches.rte.shared.Constants

@Suppress("unused")
val skipAdsPatch = bytecodePatch(
    name = "Skip ads",
    description = "Suppresses client-side (VOD) Google IMA ads in the RTÉ Player Android TV app " +
        "by neutering the IMA AdsManager start and ad-event handler. Live DAI (server-stitched " +
        "SSAI) is not affected — that can't be removed client-side without breaking live content.",
) {
    compatibleWith(Constants.COMPATIBILITY)

    execute {
        // Hook 1 — neuter the client VOD AdsManager start (return-void so the
        // loaded VAST AdsManager never begins playing ads). methodOrNull so a
        // future re-anchor miss degrades gracefully instead of aborting.
        ClientAdsManagerStartFingerprint.methodOrNull?.addInstructions(0, "return-void")

        // Hook 2 — neuter the client IMA AdEvent handler (belt-and-suspenders).
        ClientAdEventHandlerFingerprint.methodOrNull?.addInstructions(0, "return-void")
    }
}
