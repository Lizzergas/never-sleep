# Never Sleep iOS App Store delivery

This folder contains the production icon masters and storefront screenshots for
the iOS release. Store artwork uses real Simulator captures; generated imagery
is limited to the app icon. The fifth iPad artwork is a deterministic
warm-to-night comparison made from the real ready and active captures.

## Delivery matrix

| Asset | Path | Upload status |
|---|---|---|
| App icon, default | `icon/app-icon-marketing-1024.png` | 1024 × 1024, opaque PNG |
| App icon, dark | `icon/app-icon-dark-1024.png` | Included in the Xcode asset catalog |
| App icon, tinted | `icon/app-icon-tinted-1024.png` | Included in the Xcode asset catalog |
| iPhone 6.9-inch screenshots | `screenshots/iphone-6.9/en-US/` | 1320 × 2868, opaque PNG |
| iPad 13-inch screenshots | `screenshots/ipad-13/en-US/` | 2064 × 2752, opaque PNG |
| English listing copy | `metadata/en-US.md` | Review before App Store Connect entry |

The catalog source is
`app/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset`. iOS applies the final
rounded mask; source icons must remain square and unmasked.

## Rebuild

1. Put real, full-resolution captures in
   `screenshots/source/{iphone,ipad}/` using the numbered filenames already
   used by the output folders.
2. Run:

   ```bash
   python3 scripts/generate-ios-store-assets.py
   ```

3. Validate:

   ```bash
   python3 scripts/validate-ios-store-assets.py
   ```

Apple references:

- [App icon guidance](https://developer.apple.com/design/human-interface-guidelines/app-icons/)
- [Screenshot specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/)
- [Upload screenshots](https://developer.apple.com/help/app-store-connect/manage-app-information/upload-app-previews-and-screenshots)
