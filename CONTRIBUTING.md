## Pull requests

- Make sure to run the `formatKotlin` Gradle task before committing your changes.
- Do not edit language files (`strings.xml`), except for the English file under the `values` folder. Translations are handled by Weblate, and manually editing translation files breaks it.
- If you are making changes that only work with newer qBittorrent versions, make sure that your changes do not break qBitController when older qBittorrent versions are used. If your changes add new UI elements, try to keep them hidden when older qBittorrent versions are used.

    - To detect if the feature is supported in code, if the changes you make require a new property that does not exist on older qBittorrent versions, you can check whether the property exists. Otherwise, you can directly check the qBittorrent version like this:

      ```kotlin
      val version = requestManager.getQBittorrentVersion(serverId)
      if (version > QBittorrentVersion(5, 0, 0)) {
          // ...
      }
      ```

## Translations

Check out [Weblate](https://hosted.weblate.org/engage/qbitcontroller) to contribute to localization.
