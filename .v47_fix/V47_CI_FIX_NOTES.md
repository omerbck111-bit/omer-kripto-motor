# V47 CI düzeltmesi

Bu sürüm `android-actions/setup-android@v4` ile yeniden cmdline-tools kurmaz.
GitHub-hosted Ubuntu runner'ın hazır Android SDK'sını kullanır; yalnızca gerekli
platform/build-tools paketlerini `sdkmanager` ile doğrular/kurar.

Android proje kökü: `android-app/`
Gradle: 8.9
JDK: 17
Compile/Target SDK: 35
