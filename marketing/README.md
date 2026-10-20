# Never Sleep launch kit

Public identity: Lizz Studio, home@lizz.com.
Play title: **Never Sleep: Keep Screen On**.

## Files and commands

```bash
./scripts/marketing.sh prepare   # campaign links, post captions, blank measurement template
./scripts/marketing.sh studio    # edit/preview the app demo
./scripts/marketing.sh render    # render the silent 14-second portrait video locally
./scripts/marketing.sh report marketing/reports/first-week.json
```

Files are written to `dist/marketing/`: `app-demo.mp4`, preview PNGs,
`app-demo-post.txt`, `campaign-links.json`, and a blank measurement template.
The video is 1080×1920, 30 fps, H.264, with no audio track or inline dot separators.

The video shows only the app: OFF at night, a tap to ON with the sun shader,
then a tap back to OFF. It ends with “Never Sleep”, “Always awake”, and
“Stay on task, even between taps.” The app scene uses a WebGL port of its actual
Android background shader and the website toggle shader. The permission step is
omitted for pacing; the website explains setup. Release the promotional clip once
the advertised Android UI and public Play listing are live. Nothing here posts to
social networks or submits the app to Play.

## Campaign links

Use a different source/campaign pair for each channel and creative:

- Sources: `youtube`, `instagram`, `tiktok`, `community`, `website`
- Campaigns: `launch_v1` (the app demo, separated by channel)

`campaign-links.json` contains both direct Play links and website links. Use the
tracked link where a platform supports clickable destinations. A shared profile
link identifies that profile's campaign, not every video someone watched.

The website forwards only approved public `utm_source` and `utm_campaign` values
to every Play button on the current page. Untagged visits use `website/launch_v1`.
It does not add personal identifiers, cookies, local storage, or analytics requests.
Unknown values are discarded. Without JavaScript, buttons still work with the
default website campaign. Labels do not persist across separate visits or an
untagged internal navigation. No website page-view or click counts are collected.

Google Play supports these dimensions in acquisition reporting:
https://support.google.com/googleplay/android-developer/answer/9859173
https://google.play/business/acquisitionreporting/

## First measurement session

1. After the Play listing is public, open a tagged website link and confirm its
   Play button retains the source and campaign. Do not use your own repeated
   visits/installs to assess conversion.
2. In Play Console, open Store performance / Store analysis (the acquisition
   report). Choose a complete reporting period and record its dates, timezone,
   country/language filters, and export date. Filter traffic source to **Ads and
   referrals** to inspect UTM source and campaign. Save the export.
3. Copy `marketing/measurement-template.json` to an ignored file such as
   `marketing/reports/first-week.json`. Fill the overall figures from the overall
   report, and each campaign row from that exact source/campaign breakdown.
   Add rows for Instagram, TikTok, or community posts actually published.
4. Optionally record each platform's video views and spend, Play's new/active
   users and user-perceived crash/ANR rates, and AdMob revenue for the same period.
   Leave missing or suppressed figures as `null`; enter `0` only for a known zero.
5. Run the report command. Listing conversion is acquisitions divided by listing
   visitors. Cost per acquisition uses that campaign's spend and acquisitions.
   Video views are not clicks or unique people. The tool deliberately does not
   sum segmented visitor rows into an overall unique audience.

Play listing acquisitions and device installs are different metrics. Export the
whole chosen period rather than summing daily unique visitors. Allow reporting
to settle before comparing periods; record export time and refresh incomplete
periods. UTM reporting may omit low-volume traffic. An absent row is not proof of
zero installs. Aggregate AdMob revenue does not establish campaign-specific LTV.
No in-app activation or retention events are instrumented by this setup.

## Weekly decisions

First milestone: 100 new users and evidence that the app works across devices.
Compare each creative's listing visitors and conversion, alongside the actual
feedback. Few visitors suggests a distribution or link-placement problem; visitors
without acquisitions suggests checking the listing and compatibility. Fix crashes
and permission confusion before increasing traffic. Keep early promotion organic.
Once there is enough traffic, test one listing asset at a time in Play's experiments:
https://support.google.com/googleplay/android-developer/answer/12053285

Keep a dated note of listing changes, creative changes, version releases, and any
changes in campaign placement. Do not declare a winning creative from a handful
of installs or compare campaigns with different reporting filters.
