# Where this project could find an audience

Research date: 2026-10-25. No community posts, messages, pitches, or upstream submissions have been sent. Ranking below is a judgment about fit, not a promise of attention or permission to post.

## Positioning

The useful story is **composing a panoramic frame while shooting on an existing Sony A7 II**, with a removable open-source app and an intended full-frame RAW workflow. The current claim is narrower: source and signed APK available; installation confirmed on ILCE-7M2 firmware 4.01; preview/capture/EVF behavior still being validated.

Suggested current title: **“Building an XPan-style mode for the Sony A7 II — open-source prototype, EVF testing next.”**

After hardware verification, a stronger title could be “Panoramic framing inside the A7 II viewfinder, with full-frame RAW retained.” Use it only if both EVF behavior and RAW output are actually demonstrated. Avoid “turn any Sony into an XPan,” “firmware upgrade,” or equivalence to the original camera's optics/film area. Only ILCE-7M2 is targeted.

## Prioritized places

| Priority | Place | Evidence of fit | What to share / when |
| --- | --- | --- | --- |
| 1 | r/SonyAlpha | A [discussion about aspect ratios](https://www.reddit.com/r/SonyAlpha/comments/1c0nzui/whats_your_favorite_aspect_ratio_when_recomposing/) includes interest in 65:24 and custom viewfinder grids. Another [thread about missing cinematic ratios](https://www.reddit.com/r/SonyAlpha/comments/zo9oot/) explicitly describes difficulty imagining the crop while using the viewfinder. | An honest build-in-progress post with the exact A7 II model and open EVF question. Best after a short LCD/EVF demonstration. Link the repo as supporting detail and disclose authorship. These older threads are demand evidence, not invitations to revive them with advertisements. |
| 2 | OpenMemories / PMCA developers | [OpenMemories-AppList](https://github.com/ma1co/OpenMemories-AppList) is the relevant distribution catalog; its README describes apps.yaml submissions. [Recipe Lab](https://github.com/voxivoid/recipe-lab-sony-pmca) is a nearby active project with discussions enabled and A7 II compatibility reports. | Technical notes about Scalar display composition, measured VideoRect behavior and the hardware test matrix. Use this project's own issues first. Consider an AppList pull request only after core camera behavior is verified; do not open unrelated upstream issues merely for promotion. |
| 3 | Hackaday | [The editorial tip page](https://hackaday.com/submit-a-tip/) provides a direct submission route. The fit is an inference: legacy hardware, an open build, proprietary API investigation, and a constrained reusable toolchain are a maker/engineering story. | A technical write-up with source links and a real video showing the camera behavior. A useful finding or limitation is more credible than an unsupported success headline. |
| 4 | SonyAlphaRumors | [Its contact page](https://www.sonyalpharumors.com/contact-me/) explicitly accepts news/rumors; this project is directly relevant to Sony owners. | Once working behavior is documented: a short news tip focused on a new use for an older A7 II, exact firmware and limitations. Identify it as an independent project. |
| 5 | PetaPixel | [The editorial contact form](https://petapixel.com/contact./) invites story tips and pitches. | A photo-led story after successful capture: through-finder demonstration, final panoramas and original full-frame comparison. The form's permission-to-republish option is separate from submitting a tip; decide that deliberately if pitching later. |
| 6 | Digital-XPan writers and channels | Compact Shooter has [an entire article about the search for a digital XPan](https://compactshooter.com/2024/09/the-search-for-a-digital-xpan-camera/). MPB published [digital alternatives to the XPan](https://www.mpb.com/en-uk/content/kit-guides/top-3-digital-alternatives-to-the-hasselblad-xpan) in September 2025. | These establish editorial interest in the problem. A small writer with a directly relevant article may be a better match than a general gear outlet. Approach with tested output and a specific addition to their comparison, rather than implying the app already matches dedicated cameras. |

## What was checked, and what was not

- GitHub API checks found OpenMemories-AppList not archived, with a last push in March 2024. Its submission instructions are explicit, but responsiveness cannot be assumed. They require a GitHub release title matching the manifest version. This project's prerelease follows that naming convention; no catalog submission is made yet.
- Recipe Lab had discussions enabled and recent repository activity in October 2026. That suggests a relevant technical audience; it does not authorize unsolicited promotional messages.
- The r/SonyAlpha rules URL was fetched, but the current rule text did not render in the research tool. **Posting eligibility and self-promotion rules are not verified.** Read the live rules and pinned threads before posting; ask moderators if the intended post is unclear. No third-party “self-promotion allowed” directory was treated as authoritative.
- No audience-size, reach, conversion or traffic estimates are asserted. Relevant examples are stronger evidence here than generic subreddit lists.
- DPReview/Fred Miranda are plausible secondary photography communities, but this pass did not establish a sufficiently specific current thread or verified posting route to prioritize them. Film-only communities are a weak first fit for an Android-camera development announcement.

## Content worth making next

1. **A 30–60 second uncut demonstration:** exact camera model; open the app; show the LCD mask; show the same mask through the EVF; half press, capture, and return to live view. If a step fails, report it as a finding.
2. **A paired image:** full-frame JPEG with the crop boundary beside the exported 6000×2215 panorama. Verify the matching RAW remains full frame. Use a photograph the owner agrees to publish.
3. **A compact technical article:** why composition-in-camera matters, how PMCA/Scalar and Android surfaces interact, what worked on firmware 4.01, the Mac MTP installation workaround, and what remains unknown. Credit ma1co and the reference projects prominently.
4. **A tester request:** exact body/firmware, observed behavior, expected evidence, and a link to the hardware-test issue form. This is the most useful near-term conversion: reproducible results rather than broad downloads.

Recommended sequence: publish repo/prerelease now → verify the first real LCD/EVF/capture run → share one substantive Sony-owner update → bring specific technical findings to PMCA contributors → pitch selected photography/maker outlets with evidence. Do not cross-post identical promotional text everywhere.

## Draft copy for later use (not sent)

**Technical / tester introduction**

> I'm building an open-source panoramic shooting app for the Sony A7 II (ILCE-7M2). It implements a 65:24 black framing mask, physical shutter handling and a RAW+JPEG workflow through Sony's camera APIs. The prototype APK installs on firmware 4.01. Preview, capture and especially EVF overlay behavior still need physical validation. The source, build instructions and a focused test checklist are available here: https://github.com/szabadkai/sony-a7ii-xpan . I'm particularly interested in reproducible findings about LCD/EVF composition and preview-to-capture alignment.

Keep that wording tied to the current evidence. Update it only when the hardware results change. A published repository is ready to inspect; it is not yet proof of a working photographic workflow.
