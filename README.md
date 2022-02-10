# KDash
A Kotlin wrapper for the iRacing SDK

## Usage
KDash allows the user to read data as easily as running

```kotlin
val reader = IRacingDataReader.fromLiveData()
println("Car currently moving at ${reader.mostRecentBuffer["Speed"]} m/s")
```

KDash also supports reading from telemetry files.

... more to come.
