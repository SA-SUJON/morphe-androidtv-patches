package ajstrick81.morphe.patches.rte.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

// ─────────────────────────────────────────────────────────────────────────────
// RTÉ Player (com.twentyfouri.tvbridge.rte, 24i "tvbridge" platform) — ad seams.
//
// Autopsy of 3.160.3: native Kotlin app on media3/ExoPlayer, ads via the Google
// IMA SDK used DIRECTLY (com.google.ads.interactivemedia.v3.api.*), NOT the
// media3-ima bridge. Two ad pipelines, both in obfuscated `z2.*` classes:
//   VOD  → client-side VAST: AdsLoader.requestAds() (z2/f), AdsManager.start()
//   Live → server-side DAI:  AdsLoader.requestStream() + StreamManager (z2/r,u)
//
// The app classes are R8-renamed (single letters), so these fingerprints match
// on the STABLE Google IMA API references in the method bodies (obfuscation-proof)
// rather than on class/method names.
//
// SCOPE: only the client-side VOD path is targeted. Live DAI is server-stitched
// SSAI (StreamManager.init() drives the whole stitched stream — see z2/r
// onAdsManagerLoaded → z2/u.R()), so it can't be removed client-side without
// breaking live content (same ceiling as Paramount/ESPN live). Left untouched.
// ─────────────────────────────────────────────────────────────────────────────

// Hook 1 — the client VOD ad-manager START method (z2/f.t(JJ) on 3.160.3).
//
// This is the single site that calls AdsManager.start() — the point where the
// loaded client VAST AdsManager begins playing ads. NPAW analytics never calls
// start(), and live DAI uses StreamManager (not AdsManager), so matching on the
// AdsManager.start() reference uniquely finds the VOD ad-start. Neutering it
// (return-void) means the client ad manager is never started → no pre/mid-roll
// VOD ads. Content plays independently (CSAI), so this is content-safe.
object ClientAdsManagerStartFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, _ ->
        method.implementation?.instructions?.any { insn ->
            (insn as? ReferenceInstruction)?.reference?.toString()
                ?.contains("Lcom/google/ads/interactivemedia/v3/api/AdsManager;->start()") == true
        } == true
    },
)

// Hook 2 — the client IMA AdEvent handler (z2/f.a(z2/f, AdEvent) on 3.160.3).
//
// The static lambda that receives every IMA AdEvent (AD_STARTED, AD_PROGRESS,
// AD_COMPLETED, …) for the VOD client path. Belt-and-suspenders behind Hook 1:
// with the manager never started no events fire, but neutering the handler too
// guarantees no ad-event-driven UI/state. Matched by: PUBLIC+STATIC, returns V,
// and an AdEvent parameter (the only such static in the ad player; the sibling
// VideoAdPlayer callbacks take AdMediaInfo, not AdEvent).
object ClientAdEventHandlerFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    custom = { method, _ ->
        method.parameterTypes.any {
            it == "Lcom/google/ads/interactivemedia/v3/api/AdEvent;"
        }
    },
)
